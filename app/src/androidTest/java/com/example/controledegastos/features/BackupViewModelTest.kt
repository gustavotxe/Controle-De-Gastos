package com.example.controledegastos.features

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.controledegastos.data.backup.BackupRepository
import com.example.controledegastos.data.backup.ImportResult
import com.example.controledegastos.data.backup.InvalidBackupException
import java.io.IOException
import com.example.controledegastos.viewmodel.BackupState
import com.example.controledegastos.viewmodel.BackupViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class BackupViewModelTest {
    @Test fun ignoresConcurrentRequestsAndPublishesCompletion() = runBlocking {
        val repository = SuspendedRepository()
        val viewModel = BackupViewModel(repository)
        val store = ViewModelStore().apply { put("backup", viewModel) }
        try {
            onMain {
                viewModel.export(Uri.EMPTY)
                viewModel.import(Uri.EMPTY)
                assertTrue(viewModel.state.value is BackupState.Working)
            }
            withTimeout(5000.milliseconds) { repository.started.await() }
            assertEquals(1, repository.calls)
            repository.result.complete(12)
            val result = withTimeout(5000.milliseconds) { viewModel.state.first { it is BackupState.Exported } }
            assertEquals(BackupState.Exported(12), result)
        } finally { onMain { store.clear() } }
    }

    @Test fun clearingViewModelCancelsInFlightOperation() = runBlocking {
        val repository = SuspendedRepository()
        val viewModel = BackupViewModel(repository)
        val store = ViewModelStore().apply { put("backup", viewModel) }
        try {
            onMain { viewModel.export(Uri.EMPTY) }
            withTimeout(5000.milliseconds) { repository.started.await() }
        } finally { onMain { store.clear() } }
        withTimeout(5000.milliseconds) { repository.finished.await() }
        assertTrue(viewModel.state.value !is BackupState.Failed)
    }

    @Test fun reportsImportAndExportErrorsAndAllowsRetry() = runBlocking {
        for (importing in listOf(false, true)) {
            for (invalidFile in listOf(false, true)) {
                var failure: Exception? = if (invalidFile) InvalidBackupException() else IOException()
                val repository = object : BackupRepository {
                    override suspend fun export(uri: Uri): Int {
                        failure?.let { throw it }
                        return 12
                    }
                    override suspend fun import(uri: Uri): ImportResult {
                        failure?.let { throw it }
                        return ImportResult(2, 3)
                    }
                }
                val vm = BackupViewModel(repository)
                val store = ViewModelStore().apply { put("backup", vm) }
                try {
                    onMain { if (importing) vm.import(Uri.EMPTY) else vm.export(Uri.EMPTY) }
                    val failed = withTimeout(5000.milliseconds) { vm.state.first { it is BackupState.Failed } }
                    assertEquals(BackupState.Failed(invalidFile, importing), failed)
                    onMain {
                        failure = null
                        if (importing) vm.import(Uri.EMPTY) else vm.export(Uri.EMPTY)
                    }
                    val completed = withTimeout(5000.milliseconds) {
                        vm.state.first { it is BackupState.Imported || it is BackupState.Exported }
                    }
                    assertEquals(if (importing) BackupState.Imported(2, 3) else BackupState.Exported(12), completed)
                } finally { onMain { store.clear() } }
            }
        }
    }

    private fun onMain(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)

    private class SuspendedRepository : BackupRepository {
        val started = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val result = CompletableDeferred<Int>()
        var calls = 0
        override suspend fun export(uri: Uri): Int {
            calls++
            started.complete(Unit)
            return try { result.await() } finally { finished.complete(Unit) }
        }
        override suspend fun import(uri: Uri): ImportResult {
            calls++
            return ImportResult(0, 0)
        }
    }
}
