package com.example.controledegastos

import com.example.controledegastos.data.model.Items
import com.example.controledegastos.data.repository.ItemsDataSource
import com.example.controledegastos.data.repository.YearSelectionRepository
import com.example.controledegastos.viewmodel.ItemsViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals

class ItemsViewModelTest {
    @Test
    fun `insert delegates work to repository off the UI layer`() = runTest {
        val repository = FakeItemsDataSource()
        val viewModel = ItemsViewModel(repository, YearSelectionRepository(repository))
        val item = Items(0, "Salário", "", "Entrada", "Pagamento à vista", 12_345, 1_735_689_600_000, 202501, "Salário")

        // The ViewModel launches on Dispatchers.IO, outside the test scheduler.
        viewModel.insertItem(item).join()

        assertEquals(item, repository.inserted)
    }

    private class FakeItemsDataSource : ItemsDataSource {
        var inserted: Items? = null
        override val allItems = flowOf(emptyList<Items>())
        override suspend fun insertItem(item: Items) { inserted = item }
        override suspend fun updateItem(item: Items) = Unit
        override suspend fun deleteItem(id: Int) = Unit
        override suspend fun deleteItemMonth(yearMonth: Int) = Unit
        override fun getYear(year: Int) = allItems
        override fun getYearFlow(year: Int, flow: String) = allItems
        override fun getYearCategory(year: Int, category: String) = allItems
        override fun getMonth(yearMonth: Int) = allItems
        override fun getMonthFlow(yearMonth: Int, flow: String) = allItems
        override fun getIOFiltered(io: String) = allItems
        override fun getCategory(category: String) = allItems
        override fun getMonthCtg(yearMonth: Int, category: String) = allItems
    }
}
