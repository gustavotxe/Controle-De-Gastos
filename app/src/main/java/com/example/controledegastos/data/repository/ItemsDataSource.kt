package com.example.controledegastos.data.repository

import com.example.controledegastos.data.model.Items
import kotlinx.coroutines.flow.Flow

interface ItemsDataSource {
    val availableYears: Flow<List<Int>>
    val allItems: Flow<List<Items>>
    suspend fun insertItem(item: Items)
    suspend fun updateItem(item: Items)
    suspend fun deleteItem(id: Int)
    suspend fun deleteItemMonth(yearMonth: Int)
    fun getYear(year: Int): Flow<List<Items>>
    fun getYearFlow(year: Int, flow: String): Flow<List<Items>>
    fun getYearCategory(year: Int, category: String): Flow<List<Items>>
    fun getMonth(yearMonth: Int): Flow<List<Items>>
    fun getMonthFlow(yearMonth: Int, flow: String): Flow<List<Items>>
    fun getIOFiltered(io: String): Flow<List<Items>>
    fun getCategory(category: String): Flow<List<Items>>
    fun getMonthCtg(yearMonth: Int, category: String): Flow<List<Items>>
}
