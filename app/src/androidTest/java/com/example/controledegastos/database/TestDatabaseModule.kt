package com.example.controledegastos.database

import android.content.Context
import androidx.room.Room
import com.example.controledegastos.data.local.dao.ItemsDao
import com.example.controledegastos.data.local.database.AppDatabase
import com.example.controledegastos.data.local.database.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides fun dao(database: AppDatabase): ItemsDao = database.getItemsDao()
}
