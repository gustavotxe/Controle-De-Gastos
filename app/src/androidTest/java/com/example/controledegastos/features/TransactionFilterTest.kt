package com.example.controledegastos.features

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.controledegastos.data.local.database.AppDatabase
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.data.repository.ItemsRepository
import com.example.controledegastos.data.repository.YearSelectionRepository
import com.example.controledegastos.ui.model.Money
import com.example.controledegastos.ui.model.TransactionFilter
import com.example.controledegastos.viewmodel.ItemsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionFilterTest {
    @Test fun filtersKeepYearMonthAndTotalsConsistent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val store = ViewModelStore()
        try {
            val sample = Items(0, "Salário", "", "Entrada", "Pix", 1000, 1726444800000, 202409, "Salário")
            database.getItemsDao().insertAllItems(listOf(
                sample,
                sample.copy(description = "Mercado", io = "Saída", amountCents = -300, category = "Alimentação"),
                sample.copy(description = "Outro mês", amountCents = 2000, yearMonth = 202408),
                sample.copy(description = "Outro ano", amountCents = 9000, yearMonth = 202509)
            ))
            val repository = ItemsRepository(database.getItemsDao())
            val vm = withContext(Dispatchers.Main) {
                ItemsViewModel(repository, YearSelectionRepository(repository)).also {
                    store.put("filters", it)
                    it.selectYear(2024)
                    it.selectMainFilter(TransactionFilter.INFLOW)
                }
            }
            val annual = withTimeout(5000) { vm.mainUiState.first { it.filter == TransactionFilter.INFLOW && it.items.isNotEmpty() } }
            assertEquals(setOf("Salário", "Outro mês"), annual.items.map { it.description }.toSet())
            assertEquals(Money.format(3000), annual.balance)
            withContext(Dispatchers.Main) {
                vm.initializeMonth(202409)
                vm.selectMonthFilter(TransactionFilter.FOOD)
            }
            val food = withTimeout(5000) { vm.monthUiState.first { it.filter == TransactionFilter.FOOD && it.items.isNotEmpty() } }
            assertEquals(listOf("Mercado"), food.items.map { it.description })
            assertEquals(Money.format(-300), food.balance)
            withContext(Dispatchers.Main) { vm.applyMonthFilter(202409) }
            val all = withTimeout(5000) { vm.monthUiState.first { it.filter == TransactionFilter.ALL && it.items.size == 2 } }
            assertEquals(Money.format(700), all.balance)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            database.close()
        }
    }

    @Test fun restoredMonthSelectionIsNotResetByScreenInitialization() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val store = ViewModelStore()
        try {
            val repository = ItemsRepository(database.getItemsDao())
            val vm = withContext(Dispatchers.Main) {
                val saved = SavedStateHandle(mapOf("month_filter" to TransactionFilter.SALARY, "month_period" to 202409))
                ItemsViewModel(repository, YearSelectionRepository(repository), saved).also {
                    store.put("restored", it)
                    it.selectYear(2024)
                    it.initializeMonth(202409)
                }
            }
            val state = withTimeout(5000) { vm.monthUiState.first { it.filter == TransactionFilter.SALARY } }
            assertEquals(TransactionFilter.SALARY, state.filter)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            database.close()
        }
    }
}
