package com.example.controledegastos.data.backup

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackupIo

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds abstract fun bindBackupRepository(repository: JsonBackupRepository): BackupRepository

    companion object {
        @Provides @BackupIo fun provideDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
