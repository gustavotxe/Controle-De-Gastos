package com.example.controledegastos

import com.example.controledegastos.data.model.Items
import com.example.controledegastos.data.repository.ItemsDataSource
import com.example.controledegastos.viewmodel.ItemsViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ItemsViewModelTest {
    @Test
    fun `insert delegates work to repository off the UI layer`() = runTest {
        val repository = FakeItemsDataSource()
        val viewModel = ItemsViewModel(repository)
        val item = Items(0, "Salário", "", "Entrada", "Pagamento à vista", 12_345, 1_735_689_600_000, 202501, "Salário")

        viewModel.insertItem(item)
        testScheduler.advanceUntilIdle()

        assert(repository.inserted == item)
    }

    private class FakeItemsDataSource : ItemsDataSource {
        var inserted: Items? = null
        override val allItems = flowOf(emptyList<Items>())
        override suspend fun insertItem(item: Items) { inserted = item }
        override suspend fun updateItem(item: Items) = Unit
        override suspend fun deleteItem(id: Int) = Unit
        override suspend fun deleteItemMonth(yearMonth: Int) = Unit
        override fun getMonth(yearMonth: Int) = allItems
        override fun getMonthFlow(yearMonth: Int, flow: String) = allItems
        override fun getIOFiltered(io: String) = allItems
        override fun getCategory(category: String) = allItems
        override fun getMonthCtg(yearMonth: Int, category: String) = allItems
    }
}
