package com.example.controledegastos

import com.example.controledegastos.data.model.Items
import com.example.controledegastos.data.repository.ItemsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeItemsDataSource : ItemsDataSource {
    override val allItems = MutableStateFlow(emptyList<Items>())
    override val availableYears = MutableStateFlow(emptyList<Int>())
    var inserted: Items? = null
    var updated: Items? = null
    var deletedId: Int? = null
    var deletedMonth: Int? = null
    override suspend fun insertItem(item: Items) { inserted = item }
    override suspend fun updateItem(item: Items) { updated = item }
    override suspend fun deleteItem(id: Int) { deletedId = id }
    override suspend fun deleteItemMonth(yearMonth: Int) { deletedMonth = yearMonth }
    override fun getYear(year: Int) = matching { it.yearMonth / 100 == year }
    override fun getYearFlow(year: Int, flow: String) = matching { it.yearMonth / 100 == year && it.io == flow }
    override fun getYearCategory(year: Int, category: String) = matching { it.yearMonth / 100 == year && it.category == category }
    override fun getMonth(yearMonth: Int) = matching { it.yearMonth == yearMonth }
    override fun getMonthFlow(yearMonth: Int, flow: String) = matching { it.yearMonth == yearMonth && it.io == flow }
    override fun getMonthCtg(yearMonth: Int, category: String) = matching { it.yearMonth == yearMonth && it.category == category }
    override fun getIOFiltered(io: String) = matching { it.io == io }
    override fun getCategory(category: String) = matching { it.category == category }
    private fun matching(predicate: (Items) -> Boolean) = allItems.map { rows -> rows.filter(predicate) }
}

fun sampleItem(id: Int = 1, yearMonth: Int = 202409, amountCents: Long = 1000) =
    Items(id, "Salário", "", "Entrada", "Pix", amountCents, 1726444800000L, yearMonth, "Salário")
