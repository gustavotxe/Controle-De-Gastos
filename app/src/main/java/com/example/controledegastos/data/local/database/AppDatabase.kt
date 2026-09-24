package com.example.controledegastos.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.controledegastos.data.local.dao.ItemsDao
import com.example.controledegastos.data.model.Items

@Database(entities = [Items::class], version = 3, exportSchema = true)
abstract class AppDatabase: RoomDatabase() {

    abstract fun getItemsDao() : ItemsDao
}
