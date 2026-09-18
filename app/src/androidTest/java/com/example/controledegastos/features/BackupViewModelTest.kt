package com.example.controledegastos.features

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.controledegastos.data.backup.BackupRepository
import com.example.controledegastos.data.backup.ImportResult
import com.example.controledegastos.viewmodel.BackupState
import com.example.controledegastos.viewmodel.BackupViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
            withTimeout(5000) { repository.started.await() }
            assertEquals(1, repository.calls)
            repository.result.complete(12)
            val result = withTimeout(5000) { viewModel.state.first { it is BackupState.Exported } }
            assertEquals(BackupState.Exported(12), result)
        } finally { onMain { store.clear() } }
    }

    @Test fun clearingViewModelCancelsInFlightOperation() = runBlocking {
        val repository = SuspendedRepository()
        val viewModel = BackupViewModel(repository)
        val store = ViewModelStore().apply { put("backup", viewModel) }
        try {
            onMain { viewModel.export(Uri.EMPTY) }
            withTimeout(5000) { repository.started.await() }
        } finally { onMain { store.clear() } }
        withTimeout(5000) { repository.finished.await() }
        assertTrue(viewModel.state.value !is BackupState.Failed)
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
