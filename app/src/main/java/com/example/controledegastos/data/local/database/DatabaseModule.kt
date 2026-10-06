package com.example.controledegastos.data.local.database

import android.content.Context
import androidx.room.Room
import com.example.controledegastos.data.local.dao.ItemsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database")
            .addMigrations(AppDatabaseMigrations.MIGRATION_2_3)
            .build()

    }

    @Provides
    fun provideItemsDao(database: AppDatabase): ItemsDao {
        return database.getItemsDao()
    }

}

