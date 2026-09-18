package com.example.controledegastos.data.backup

import android.net.Uri
import androidx.room.withTransaction
import com.example.controledegastos.data.local.database.AppDatabase
import com.example.controledegastos.data.model.Items
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction

data class ImportResult(val added: Int, val skipped: Int)

interface BackupRepository {
    suspend fun export(uri: Uri): Int
    suspend fun import(uri: Uri): ImportResult
}

class JsonBackupRepository @Inject constructor(
    private val database: AppDatabase,
    private val documents: BackupDocumentStore,
    private val codec: BackupJsonCodec,
    @BackupIo private val dispatcher: CoroutineDispatcher
) : BackupRepository {
    override suspend fun export(uri: Uri): Int = withContext(dispatcher) {
        val items = database.getItemsDao().getBackupSnapshot()
        // Apply the same bounds on both sides so every successful export can be imported.
        if (items.size > BackupJsonCodec.MAX_ITEMS) throw InvalidBackupException()
        documents.write(uri, { coroutineContext.ensureActive() }) { writer ->
            codec.write(writer, items) { coroutineContext.ensureActive() }
        }
        items.size
    }

    override suspend fun import(uri: Uri): ImportResult = withContext(dispatcher) {
        val imported = documents.openInput(uri).use { input ->
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            InputStreamReader(input, decoder).buffered().use { reader ->
                codec.read(reader) { coroutineContext.ensureActive() }
            }
        }
        database.withTransaction {
            val dao = database.getItemsDao()
            // Compare contents, not device-local IDs. Preserve repeated identical entries
            // up to their multiplicity in the file while making repeated imports idempotent.
            val remaining = HashMap<Items, Int>()
            dao.getBackupSnapshot().forEach { item ->
                coroutineContext.ensureActive()
                val key = item.copy(id = 0)
                remaining[key] = (remaining[key] ?: 0) + 1
            }
            var added = 0
            imported.chunked(500).forEach { batch ->
                coroutineContext.ensureActive()
                val missing = batch.filter { item ->
                    val count = remaining[item] ?: 0
                    if (count > 0) remaining[item] = count - 1
                    count == 0
                }
                dao.insertAllItems(missing)
                added += missing.size
            }
            ImportResult(added, imported.size - added)
        }
    }
}
