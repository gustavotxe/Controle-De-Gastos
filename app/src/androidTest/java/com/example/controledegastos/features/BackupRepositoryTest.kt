package com.example.controledegastos.features

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.controledegastos.data.backup.BackupDocumentStore
import com.example.controledegastos.data.backup.BackupJsonCodec
import com.example.controledegastos.data.backup.ImportResult
import com.example.controledegastos.data.backup.InvalidBackupException
import com.example.controledegastos.data.backup.JsonBackupRepository
import com.example.controledegastos.data.local.database.AppDatabase
import com.example.controledegastos.data.model.Items
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.StringReader
import java.io.StringWriter

/** Uses an isolated in-memory database and private temporary files, never the app's history. */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: JsonBackupRepository
    private lateinit var file: File
    private val codec = BackupJsonCodec()
    private val sample = Items(0, "Salário ç \"teste\"", "Linha 1\nLinha 2", "Entrada",
        "Pix", 9007199254740993L, 1726444800000L, 202409, "Salário")

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = JsonBackupRepository(database, BackupDocumentStore(context), codec, Dispatchers.IO)
        file = File.createTempFile("backup-test-", ".json", context.cacheDir)
    }

    @After fun cleanup() { database.close(); file.delete() }

    @Test fun exportIncludesAllYearsInOrderAndPreservesExactValues() = runBlocking {
        val earlier = sample.copy(id = 7, yearMonth = 202201, occurredAtMillis = 1640995200000L,
            io = "Saída", amountCents = -12345, description = "")
        val later = sample.copy(id = 8)
        database.getItemsDao().insertAllItems(listOf(later, earlier))
        assertEquals(2, repository.export(Uri.fromFile(file)))
        val decoded = codec.read(file.reader()) {}
        assertEquals(listOf(earlier.copy(id = 0), later.copy(id = 0)), decoded)
        assertTrue(file.readText().contains("9007199254740993"))
    }

    @Test fun importMergesWithoutReplacingIdsAndPreservesDuplicateMultiplicity() = runBlocking {
        val existing = sample.copy(id = 17)
        val other = sample.copy(id = 18, description = "Outro", yearMonth = 202501)
        database.getItemsDao().insertAllItems(listOf(existing, other))
        codec.write(file.writer(), listOf(sample.copy(id = 18), sample.copy(id = 19))) {}
        assertEquals(ImportResult(1, 1), repository.import(Uri.fromFile(file)))
        assertEquals(ImportResult(0, 2), repository.import(Uri.fromFile(file)))
        val rows = database.getItemsDao().getBackupSnapshot()
        assertEquals(3, rows.size)
        assertTrue(rows.contains(existing))
        assertTrue(rows.contains(other))
    }

    @Test fun invalidLastRecordLeavesDatabaseUnchanged() = runBlocking {
        database.getItemsDao().insertItem(sample)
        val before = database.getItemsDao().getBackupSnapshot()
        codec.write(file.writer(), listOf(sample.copy(description = "Novo"), sample)) {}
        file.writeText(file.readText().replace("\"yearMonth\": 202409", "\"yearMonth\": 202413"))
        try {
            repository.import(Uri.fromFile(file))
            fail("Invalid month accepted")
        } catch (_: InvalidBackupException) { }
        assertEquals(before, database.getItemsDao().getBackupSnapshot())
    }

    @Test fun codecRejectsMissingFieldsDuplicateKeysFractionsAndUnsupportedVersions() {
        val valid = StringWriter().also { codec.write(it, listOf(sample)) {} }.toString()
        val invalid = listOf(
            valid.replace("\"version\": 1", "\"version\": 2"),
            valid.replace("\"version\": 1", "\"version\": 1, \"version\": 1"),
            valid.replace("\"yearMonth\": 202409", "\"yearMonth\": 202409.1"),
            valid.replace("\"yearMonth\": 202409,", ""),
            valid.replace("9007199254740993", "-1"),
            valid + "{}",
            valid.dropLast(10)
        )
        invalid.forEach { text ->
            try { codec.read(StringReader(text)) {}; fail("Invalid backup accepted") }
            catch (_: InvalidBackupException) { }
        }
    }

    @Test fun cancellationIsNotConvertedIntoInvalidFileError() {
        try {
            codec.read(StringReader("{\"version\":1}")) { throw CancellationException() }
            fail("Cancellation ignored")
        } catch (_: CancellationException) { }
    }

    @Test fun emptyBackupRoundTrips() = runBlocking {
        assertEquals(0, repository.export(Uri.fromFile(file)))
        assertEquals(ImportResult(0, 0), repository.import(Uri.fromFile(file)))
    }

    @Test fun databaseFailureRollsBackEarlierBatches() = runBlocking {
        database.getItemsDao().insertItem(sample)
        val before = database.getItemsDao().getBackupSnapshot()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_test BEFORE INSERT ON Items WHEN NEW.description = 'Reject' " +
                "BEGIN SELECT RAISE(ABORT, 'test failure'); END"
        )
        val rows = (1..500).map { sample.copy(description = "New $it") } + sample.copy(description = "Reject")
        codec.write(file.writer(), rows) {}
        try {
            repository.import(Uri.fromFile(file))
            fail("Database failure ignored")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(before, database.getItemsDao().getBackupSnapshot())
    }
}
