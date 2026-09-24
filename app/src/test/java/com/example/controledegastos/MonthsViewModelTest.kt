package com.example.controledegastos

import androidx.lifecycle.ViewModelStore
import com.example.controledegastos.data.repository.YearSelectionRepository
import com.example.controledegastos.di.AppDispatchers
import com.example.controledegastos.ui.model.Money
import com.example.controledegastos.viewmodel.MonthsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun cleanup() = store.clear()

    @Test fun `summaries include empty months and update when year changes`() = runTest {
        val repository = FakeItemsDataSource()
        val dispatchers = AppDispatchers(main.dispatcher, main.dispatcher)
        val vm = MonthsViewModel(repository, YearSelectionRepository(repository, dispatchers), dispatchers)
        store.put("months", vm)
        repository.allItems.value = listOf(sampleItem(),
            sampleItem(2, amountCents = -300).copy(io = "Saída"), sampleItem(3, 202501, 2500))
        vm.selectYear(2024)
        backgroundScope.launch { vm.months.collect() }
        runCurrent()
        assertEquals((202401..202412).toList(), vm.months.value.map { it.yearMonth })
        val september = vm.months.value[8]
        assertEquals("Setembro", september.monthName)
        assertEquals(Money.format(700), september.balanceText)
        assertEquals(Money.format(1000), september.inflowText)
        assertEquals(Money.format(-300), september.outflowText)
        assertEquals(10f, september.inflowPie, 0f)
        assertEquals(3f, september.outflowPie, 0f)
        assertTrue(september.hasData)
        assertFalse(vm.months.value[0].hasData)
        assertEquals(Money.format(0), vm.months.value[0].balanceText)
        vm.selectYear(2025)
        runCurrent()
        assertEquals(202501, vm.months.value.first().yearMonth)
        assertEquals(Money.format(2500), vm.months.value.first().balanceText)
        assertFalse(vm.months.value[8].hasData)
    }
}
