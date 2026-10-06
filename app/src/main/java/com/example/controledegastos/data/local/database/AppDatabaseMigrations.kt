package com.example.controledegastos.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE Items_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                description TEXT NOT NULL, observation TEXT NOT NULL,
                io TEXT NOT NULL, paymentMethod TEXT NOT NULL,
                amountCents INTEGER NOT NULL, occurredAtMillis INTEGER NOT NULL,
                yearMonth INTEGER NOT NULL, category TEXT NOT NULL)""")
            db.execSQL("""INSERT INTO Items_new
                (id, description, observation, io, paymentMethod, amountCents,
                 occurredAtMillis, yearMonth, category)
                SELECT id, description, observation, COALESCE(io, ''), COALESCE(paymentMethod, ''),
                    CAST(ROUND(value * 100) AS INTEGER),
                    COALESCE(CAST(strftime('%s', substr(date, 7, 4) || '-' || substr(date, 4, 2) || '-' || substr(date, 1, 2)) AS INTEGER) * 1000, 0),
                    CASE WHEN length(date) = 10
                        THEN CAST(substr(date, 7, 4) || substr(date, 4, 2) AS INTEGER) ELSE 0 END,
                    COALESCE(category, '')
                FROM Items""")
            db.execSQL("DROP TABLE Items")
            db.execSQL("ALTER TABLE Items_new RENAME TO Items")
        }
    }
}
