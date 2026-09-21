package com.example.controledegastos.data.repository

import com.example.controledegastos.data.model.Items
import com.example.controledegastos.data.local.dao.ItemsDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ItemsRepository @Inject constructor(private val itemsDao: ItemsDao) : ItemsDataSource {

    override val availableYears: Flow<List<Int>> = itemsDao.getAvailableYears()

    override val allItems: Flow<List<Items>> = itemsDao.getAllItems()

    override suspend fun insertItem(item: Items) {
        itemsDao.insertItem(item)
    }

    override suspend fun updateItem(item: Items) {
        itemsDao.updateItem(item)
    }

    override suspend fun deleteItem(id: Int) {
        itemsDao.deleteItem(id)
    }

    override suspend fun deleteItemMonth(yearMonth: Int) {
        itemsDao.deleteItemMonth(yearMonth)
    }

    override fun getYear(year: Int): Flow<List<Items>> =
        itemsDao.getYear(year * 100 + 1, year * 100 + 12)

    override fun getYearFlow(year: Int, flow: String): Flow<List<Items>> =
        itemsDao.getYearFlow(year * 100 + 1, year * 100 + 12, flow)

    override fun getYearCategory(year: Int, category: String): Flow<List<Items>> =
        itemsDao.getYearCategory(year * 100 + 1, year * 100 + 12, category)

    override fun getMonth(yearMonth: Int): Flow<List<Items>> {
        return itemsDao.getMonth(yearMonth)
    }

    override fun getMonthFlow(yearMonth: Int, flow: String): Flow<List<Items>> {
        return itemsDao.getMonthFlow(yearMonth, flow)
    }

    override fun getIOFiltered(io: String): Flow<List<Items>> {
        return itemsDao.getIOFiltered(io)
    }

    override fun getCategory(category: String): Flow<List<Items>> {
        return itemsDao.getCategory(category)
    }

    override fun getMonthCtg(yearMonth: Int, category: String): Flow<List<Items>> {
        return itemsDao.getMonthCtg(yearMonth, category)
    }

}
