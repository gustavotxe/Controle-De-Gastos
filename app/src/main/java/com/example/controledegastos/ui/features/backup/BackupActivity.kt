package com.example.controledegastos.ui.features.backup

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.controledegastos.R
import com.example.controledegastos.databinding.ActivityBackupBinding
import com.example.controledegastos.viewmodel.BackupState
import com.example.controledegastos.viewmodel.BackupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class BackupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupBinding
    private val viewModel: BackupViewModel by viewModels()
    private val exportDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }
    private val importDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backupToolbar.setNavigationOnClickListener { finish() }
        binding.exportButton.setOnClickListener {
            val date = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.ROOT).format(Date())
            exportDocument.launch("controle-de-gastos-$date.json")
        }
        binding.importButton.setOnClickListener {
            importDocument.launch(arrayOf("application/json", "text/json", "text/plain", "application/octet-stream"))
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: BackupState) {
        val busy = state is BackupState.Working
        binding.backupProgress.isVisible = busy
        binding.exportButton.isEnabled = !busy
        binding.importButton.isEnabled = !busy
        binding.backupStatus.text = when (state) {
            BackupState.Idle -> getString(R.string.backup_ready)
            is BackupState.Working -> getString(if (state.importing) R.string.backup_importing else R.string.backup_exporting)
            is BackupState.Exported -> getString(R.string.backup_exported, state.count)
            is BackupState.Imported -> getString(R.string.backup_imported, state.added, state.skipped)
            is BackupState.Failed -> getString(when {
                state.invalidFile && state.importing -> R.string.backup_invalid
                state.invalidFile -> R.string.backup_export_limit
                state.importing -> R.string.backup_import_failed
                else -> R.string.backup_export_failed
            })
        }
    }
}
