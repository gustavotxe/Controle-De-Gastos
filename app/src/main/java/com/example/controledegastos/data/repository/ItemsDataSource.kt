package com.example.controledegastos.data.repository

import com.example.controledegastos.data.model.Items
import kotlinx.coroutines.flow.Flow

interface ItemsDataSource {
    val allItems: Flow<List<Items>>
    suspend fun insertItem(item: Items)
    suspend fun updateItem(item: Items)
    suspend fun deleteItem(id: Int)
    suspend fun deleteItemMonth(yearMonth: Int)
    fun getMonth(yearMonth: Int): Flow<List<Items>>
    fun getMonthFlow(yearMonth: Int, flow: String): Flow<List<Items>>
    fun getIOFiltered(io: String): Flow<List<Items>>
    fun getCategory(category: String): Flow<List<Items>>
    fun getMonthCtg(yearMonth: Int, category: String): Flow<List<Items>>
}
