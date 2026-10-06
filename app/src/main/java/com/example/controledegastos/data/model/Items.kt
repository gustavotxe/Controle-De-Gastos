package com.example.controledegastos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Items")
data class Items(

    @PrimaryKey(autoGenerate = true)
    val id: Int,

    val description: String,
    val observation: String,
    val io: String,
    val paymentMethod: String,
    /** Signed amount in cents. Using an integer avoids floating-point rounding errors. */
    val amountCents: Long,
    /** Instant selected by the user, in UTC milliseconds. */
    val occurredAtMillis: Long,
    /** YYYYMM, stored to keep month/year filtering and indexing simple. */
    val yearMonth: Int,
    val category: String
)
