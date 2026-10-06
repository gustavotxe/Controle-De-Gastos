package com.example.controledegastos.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controledegastos.data.backup.BackupRepository
import com.example.controledegastos.data.backup.InvalidBackupException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupState {
    data object Idle : BackupState
    data class Working(val importing: Boolean) : BackupState
    data class Exported(val count: Int) : BackupState
    data class Imported(val added: Int, val skipped: Int) : BackupState
    data class Failed(val invalidFile: Boolean, val importing: Boolean) : BackupState
}

@HiltViewModel
class BackupViewModel @Inject constructor(private val repository: BackupRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<BackupState>(BackupState.Idle)
    val state = mutableState.asStateFlow()

    fun export(uri: Uri) = runOperation(importing = false) {
        BackupState.Exported(repository.export(uri))
    }

    fun import(uri: Uri) = runOperation(importing = true) {
        repository.import(uri).let { BackupState.Imported(it.added, it.skipped) }
    }

    private fun runOperation(importing: Boolean, operation: suspend () -> BackupState) {
        if (mutableState.value is BackupState.Working) return
        mutableState.value = BackupState.Working(importing)
        viewModelScope.launch {
            mutableState.value = try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                BackupState.Failed(exception is InvalidBackupException, importing)
            }
        }
    }
}
