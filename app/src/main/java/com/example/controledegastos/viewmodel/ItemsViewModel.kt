package com.example.controledegastos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controledegastos.data.model.FlowType
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.data.repository.ItemsDataSource
import com.example.controledegastos.ui.model.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val items: List<Items> = emptyList(),
    val balance: String = Money.format(0),
    val inflow: String = Money.format(0),
    val outflow: String = Money.format(0)
)

data class AnnualChartUiState(
    val inflowCents: Long = 0,
    val outflowCents: Long = 0,
    val balanceCents: Long = 0
) {
    val inflowText get() = Money.format(inflowCents)
    val outflowText get() = Money.format(outflowCents)
    val balanceText get() = Money.format(balanceCents)
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ItemsViewModel @Inject constructor(
    private val itemsRepository: ItemsDataSource
) : ViewModel() {

    private sealed interface MainFilter {
        data object All : MainFilter
        data class Flow(val value: String) : MainFilter
        data class Category(val value: String) : MainFilter
    }

    private data class MonthFilter(
        val yearMonth: Int,
        val category: String? = null,
        val flow: String? = null
    )

    private val mainFilter = MutableStateFlow<MainFilter>(MainFilter.All)
    private val monthFilter = MutableStateFlow<MonthFilter?>(null)

    val mainUiState: StateFlow<TransactionsUiState> = mainFilter
        .flatMapLatest { filter -> filter.toItemsFlow() }
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    val monthUiState: StateFlow<TransactionsUiState> = monthFilter
        .flatMapLatest { filter -> filter?.toItemsFlow() ?: flowOf(emptyList()) }
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    val annualChartState: StateFlow<AnnualChartUiState> = itemsRepository.allItems
        .map { items ->
            val inflow = items.filter { it.io == FlowType.INFLOW.value }.sumOf { it.amountCents }
            val outflow = items.filter { it.io == FlowType.OUTFLOW.value }.sumOf { it.amountCents }
            AnnualChartUiState(inflow, outflow, inflow + outflow)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnnualChartUiState())

    fun applyMainAllFilter() { mainFilter.value = MainFilter.All }
    fun applyMainFlowFilter(flow: String) { mainFilter.value = MainFilter.Flow(flow) }
    fun applyMainCategoryFilter(category: String) { mainFilter.value = MainFilter.Category(category) }

    fun applyMonthFilter(yearMonth: Int, category: String? = null, flow: String? = null) {
        monthFilter.value = MonthFilter(yearMonth, category, flow)
    }

    fun insertItem(item: Items) = viewModelScope.launch(Dispatchers.IO) { itemsRepository.insertItem(item) }
    fun updateItem(item: Items) = viewModelScope.launch(Dispatchers.IO) { itemsRepository.updateItem(item) }
    fun deleteItem(id: Int) = viewModelScope.launch(Dispatchers.IO) { itemsRepository.deleteItem(id) }
    fun deleteItemMonth(yearMonth: Int) = viewModelScope.launch(Dispatchers.IO) { itemsRepository.deleteItemMonth(yearMonth) }

    private fun MainFilter.toItemsFlow(): Flow<List<Items>> = when (this) {
        MainFilter.All -> itemsRepository.allItems
        is MainFilter.Flow -> itemsRepository.getIOFiltered(value)
        is MainFilter.Category -> itemsRepository.getCategory(value)
    }

    private fun MonthFilter.toItemsFlow(): Flow<List<Items>> = when {
        flow != null -> itemsRepository.getMonthFlow(yearMonth, flow)
        category != null -> itemsRepository.getMonthCtg(yearMonth, category)
        else -> itemsRepository.getMonth(yearMonth)
    }

    private fun List<Items>.toUiState(): TransactionsUiState {
        val inflow = filter { it.io == FlowType.INFLOW.value }.sumOf { it.amountCents }
        val outflow = filter { it.io == FlowType.OUTFLOW.value }.sumOf { it.amountCents }
        return TransactionsUiState(
            items = this,
            balance = Money.format(inflow + outflow),
            inflow = Money.format(inflow),
            outflow = Money.format(outflow)
        )
    }
}
