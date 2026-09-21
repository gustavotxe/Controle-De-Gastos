package com.example.controledegastos.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Items ADD COLUMN amountCents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE Items ADD COLUMN occurredAtMillis INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE Items ADD COLUMN yearMonth INTEGER NOT NULL DEFAULT 0")

            db.execSQL("UPDATE Items SET amountCents = CAST(ROUND(value * 100) AS INTEGER)")
            db.execSQL(
                "UPDATE Items SET yearMonth = CASE " +
                    "WHEN length(date) = 10 THEN CAST(substr(date, 7, 4) || substr(date, 4, 2) AS INTEGER) " +
                    "ELSE 0 END"
            )
            db.execSQL(
                "UPDATE Items SET occurredAtMillis = CASE " +
                    "WHEN length(date) = 10 THEN CAST(strftime('%s', substr(date, 7, 4) || '-' || substr(date, 4, 2) || '-' || substr(date, 1, 2)) AS INTEGER) * 1000 " +
                    "ELSE 0 END"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_Items_yearMonth ON Items(yearMonth)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_Items_occurredAtMillis ON Items(occurredAtMillis)")
        }
    }
}
