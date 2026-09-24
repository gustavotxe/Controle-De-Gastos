package com.example.controledegastos.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.controledegastos.data.local.database.AppDatabase
import com.example.controledegastos.data.local.database.AppDatabaseMigrations
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMigrationTest {
    @Test fun migrationFromVersion2PreservesRowsAndMatchesCurrentRoomSchema() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                old.execSQL("""CREATE TABLE Items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    description TEXT NOT NULL, observation TEXT NOT NULL,
                    io TEXT, paymentMethod TEXT, value REAL NOT NULL,
                    date TEXT NOT NULL, monthNumber TEXT, category TEXT)""")
                old.execSQL("INSERT INTO Items VALUES (7, 'Salário', 'Nota', 'Entrada', 'Pix', 1234.56, '16/09/2024', '9', 'Salário')")
                old.execSQL("INSERT INTO Items VALUES (8, 'Mercado', '', 'Saída', NULL, -12.34, '29/02/2024', '2', NULL)")
                old.version = 2
            }
            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabaseMigrations.MIGRATION_2_3).build()
            try {
                val rows = migrated.getItemsDao().getBackupSnapshot().associateBy { it.id }
                assertEquals(setOf(7, 8), rows.keys)
                val salary = rows.getValue(7)
                assertEquals(123456L, salary.amountCents)
                assertEquals(202409, salary.yearMonth)
                assertEquals(1726444800000L, salary.occurredAtMillis)
                assertEquals("Nota", salary.observation)
                assertEquals("Pix", salary.paymentMethod)
                val expense = rows.getValue(8)
                assertEquals(-1234L, expense.amountCents)
                assertEquals(202402, expense.yearMonth)
                assertEquals("", expense.paymentMethod)
                assertEquals("", expense.category)
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
}
