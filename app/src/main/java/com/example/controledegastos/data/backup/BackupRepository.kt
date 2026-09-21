package com.example.controledegastos.data.backup

import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.controledegastos.data.local.database.AppDatabase
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.data.local.dao.ItemsDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.currentCoroutineContext
import javax.inject.Inject
import java.io.InputStreamReader
import java.io.File
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

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
        var count = 0
        documents.write(uri, { coroutineContext.ensureActive() }) { writer ->
            database.withTransaction {
                val dao = database.getItemsDao()
                if (dao.countItems() > BackupJsonCodec.MAX_ITEMS) throw InvalidBackupException()
                count = codec.writeRows(writer, backupRows(dao)) { coroutineContext.ensureActive() }
            }
        }
        count
    }

    override suspend fun import(uri: Uri): ImportResult = withContext(dispatcher) {
        documents.withLocalInput(uri, { coroutineContext.ensureActive() }) { file ->
            val missingCounts = HashMap<String, Int>()
            var importedCount = 0
            file.strictReader().use { reader ->
                codec.readRows(reader) { coroutineContext.ensureActive() }.forEach { item ->
                    val key = item.fingerprint()
                    missingCounts[key] = (missingCounts[key] ?: 0) + 1
                    importedCount++
                }
            }
            database.withTransaction {
                val dao = database.getItemsDao()
                backupRows(dao).collect { item -> missingCounts.consume(item.fingerprint()) }
                var added = 0
                val batch = ArrayList<Items>()
                var batchChars = 0
                file.strictReader().use { reader ->
                    for (item in codec.readRows(reader) { coroutineContext.ensureActive() }) {
                        if (missingCounts.consume(item.fingerprint())) {
                            batch.add(item)
                            batchChars += item.description.length + item.observation.length +
                                item.paymentMethod.length + item.category.length + item.io.length
                            if (batch.size >= PAGE_SIZE || batchChars >= MAX_BATCH_CHARS) {
                                dao.insertAllItems(batch)
                                added += batch.size
                                batch.clear()
                                batchChars = 0
                            }
                        }
                    }
                }
                if (batch.isNotEmpty()) {
                    dao.insertAllItems(batch)
                    added += batch.size
                }
                ImportResult(added, importedCount - added)
            }
        }
    }

    private fun backupRows(dao: ItemsDao) = flow {
        dao.getBackupCursor().use { cursor ->
            val id = cursor.getColumnIndexOrThrow("id")
            val description = cursor.getColumnIndexOrThrow("description")
            val observation = cursor.getColumnIndexOrThrow("observation")
            val io = cursor.getColumnIndexOrThrow("io")
            val payment = cursor.getColumnIndexOrThrow("paymentMethod")
            val amount = cursor.getColumnIndexOrThrow("amountCents")
            val date = cursor.getColumnIndexOrThrow("occurredAtMillis")
            val month = cursor.getColumnIndexOrThrow("yearMonth")
            val category = cursor.getColumnIndexOrThrow("category")
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                emit(Items(cursor.getInt(id), cursor.getString(description), cursor.getString(observation),
                    cursor.getString(io), cursor.getString(payment), cursor.getLong(amount),
                    cursor.getLong(date), cursor.getInt(month), cursor.getString(category)))
            }
        }
    }

    private fun File.strictReader() = InputStreamReader(inputStream(), Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)).buffered()

    private fun MutableMap<String, Int>.consume(key: String): Boolean {
        val count = this[key] ?: return false
        if (count == 1) remove(key) else this[key] = count - 1
        return true
    }

    private fun Items.fingerprint(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun number(value: Long) {
            for (shift in 56 downTo 0 step 8) digest.update((value ushr shift).toByte())
        }
        fun text(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            number(bytes.size.toLong())
            digest.update(bytes)
        }
        text(description)
        text(observation)
        text(io)
        text(paymentMethod)
        number(amountCents)
        number(occurredAtMillis)
        number(yearMonth.toLong())
        text(category)
        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP)
    }

    private companion object {
        const val PAGE_SIZE = 64
        const val MAX_BATCH_CHARS = 256 * 1024
    }
}
