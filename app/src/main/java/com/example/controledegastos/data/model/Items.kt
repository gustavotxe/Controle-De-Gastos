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
    val amountCents: Long,
    val occurredAtMillis: Long,
    val yearMonth: Int,
    val category: String
)
