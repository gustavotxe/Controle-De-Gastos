package com.example.controledegastos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.controledegastos.data.repository.YearSelectionRepository
import com.example.controledegastos.di.AppDispatchers
import com.example.controledegastos.ui.model.Money
import com.example.controledegastos.ui.model.TransactionFilter
import com.example.controledegastos.viewmodel.AnnualChartUiState
import com.example.controledegastos.viewmodel.ItemsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeItemsDataSource()
    private val store = ViewModelStore()
    private lateinit var viewModel: ItemsViewModel
    private lateinit var years: YearSelectionRepository
    private lateinit var dispatchers: AppDispatchers

    @Before fun setup() {
        dispatchers = AppDispatchers(main.dispatcher, main.dispatcher)
        years = YearSelectionRepository(repository, dispatchers)
        viewModel = ItemsViewModel(repository, years, dispatchers = dispatchers)
        store.put("items", viewModel)
        viewModel.selectYear(2024)
    }
    @After fun cleanup() = store.clear()

    @Test fun `writes delegate exact arguments and deletion uses selected year`() = runTest {
        val item = sampleItem()
        viewModel.insertItem(item).join()
        viewModel.updateItem(item.copy(description = "Editado")).join()
        viewModel.deleteItem(7).join()
        viewModel.selectYear(2025)
        viewModel.deleteItemMonth(202409)
        runCurrent()
        assertEquals(item, repository.inserted)
        assertEquals(item.copy(description = "Editado"), repository.updated)
        assertEquals(7, repository.deletedId)
        assertEquals(202509, repository.deletedMonth)
    }

    @Test fun `annual filter updates rows and all three totals`() = runTest {
        repository.allItems.value = listOf(sampleItem(),
            sampleItem(2, amountCents = -300).copy(io = "Saída", category = "Alimentação"),
            sampleItem(3, 202509, 9000))
        backgroundScope.launch { viewModel.mainUiState.collect() }
        runCurrent()
        assertEquals(listOf(1, 2), viewModel.mainUiState.value.items.map { it.id })
        assertEquals(Money.format(700), viewModel.mainUiState.value.balance)
        assertEquals(Money.format(1000), viewModel.mainUiState.value.inflow)
        assertEquals(Money.format(-300), viewModel.mainUiState.value.outflow)
        viewModel.selectMainFilter(TransactionFilter.INFLOW)
        runCurrent()
        assertEquals(listOf(1), viewModel.mainUiState.value.items.map { it.id })
        assertEquals(Money.format(0), viewModel.mainUiState.value.outflow)
        viewModel.selectYear(2025)
        runCurrent()
        assertEquals(listOf(3), viewModel.mainUiState.value.items.map { it.id })
        assertEquals(Money.format(9000), viewModel.mainUiState.value.balance)
    }

    @Test fun `monthly category follows selected year and clears stale results`() = runTest {
        repository.allItems.value = listOf(
            sampleItem(1, amountCents = -300).copy(io = "Saída", category = "Alimentação"),
            sampleItem(2, 202408), sampleItem(3, 202509))
        viewModel.initializeMonth(202409)
        viewModel.selectMonthFilter(TransactionFilter.FOOD)
        backgroundScope.launch { viewModel.monthUiState.collect() }
        runCurrent()
        assertEquals(listOf(1), viewModel.monthUiState.value.items.map { it.id })
        assertEquals(Money.format(-300), viewModel.monthUiState.value.balance)
        viewModel.selectYear(2025)
        runCurrent()
        assertTrue(viewModel.monthUiState.value.items.isEmpty())
        assertEquals(Money.format(0), viewModel.monthUiState.value.balance)
        viewModel.applyMonthFilter(202509)
        runCurrent()
        assertEquals(listOf(3), viewModel.monthUiState.value.items.map { it.id })
    }

    @Test fun `chart totals stay independent of the list filter`() = runTest {
        repository.allItems.value = listOf(sampleItem(),
            sampleItem(2, amountCents = -300).copy(io = "Saída"))
        backgroundScope.launch { viewModel.annualChartState.collect() }
        viewModel.selectMainFilter(TransactionFilter.INFLOW)
        runCurrent()
        assertEquals(AnnualChartUiState(1000, -300, 700), viewModel.annualChartState.value)
        repository.allItems.value = emptyList()
        runCurrent()
        assertEquals(AnnualChartUiState(), viewModel.annualChartState.value)
    }

    @Test fun `initializing restored month keeps filter and period`() = runTest {
        val saved = SavedStateHandle(mapOf("month_filter" to TransactionFilter.SALARY, "month_period" to 202409))
        val restored = ItemsViewModel(repository, years, saved, dispatchers)
        store.put("restored", restored)
        repository.allItems.value = listOf(sampleItem(), sampleItem(2, 202408))
        restored.initializeMonth(202409)
        backgroundScope.launch { restored.monthUiState.collect() }
        runCurrent()
        assertEquals(TransactionFilter.SALARY, restored.monthUiState.value.filter)
        assertEquals(listOf(1), restored.monthUiState.value.items.map { it.id })
    }
}
