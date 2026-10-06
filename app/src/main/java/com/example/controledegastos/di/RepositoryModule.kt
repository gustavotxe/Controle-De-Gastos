package com.example.controledegastos.di

import com.example.controledegastos.data.repository.ItemsDataSource
import com.example.controledegastos.data.repository.ItemsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindItemsDataSource(repository: ItemsRepository): ItemsDataSource
}
