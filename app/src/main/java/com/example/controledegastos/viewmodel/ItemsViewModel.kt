package com.example.controledegastos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.controledegastos.data.model.FlowType
import com.example.controledegastos.data.model.Items
import com.example.controledegastos.data.repository.ItemsDataSource
import com.example.controledegastos.data.repository.YearSelectionRepository
import com.example.controledegastos.ui.model.Money
import com.example.controledegastos.ui.model.TransactionFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val items: List<Items> = emptyList(),
    val balance: String = Money.format(0),
    val inflow: String = Money.format(0),
    val outflow: String = Money.format(0),
    val filter: TransactionFilter = TransactionFilter.ALL
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
    private val itemsRepository: ItemsDataSource,
    private val yearSelectionRepository: YearSelectionRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val mainFilter = savedStateHandle.getStateFlow("main_filter", TransactionFilter.ALL)
    private val monthFilter = savedStateHandle.getStateFlow("month_filter", TransactionFilter.ALL)
    private val monthPeriod = savedStateHandle.getStateFlow<Int?>("month_period", null)

    val availableYears = yearSelectionRepository.availableYears
    val selectedYear = yearSelectionRepository.selectedYear

    val mainUiState: StateFlow<TransactionsUiState> = mainFilter
        .combine(selectedYear) { filter, year -> filter to year }
        .flatMapLatest { (filter, year) -> filter.toYearItemsFlow(year).map { it.toUiState(filter) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    val monthUiState: StateFlow<TransactionsUiState> = combine(monthPeriod, monthFilter, selectedYear) { period, filter, year ->
        (period?.let { year * 100 + (it % 100) }) to filter
    }
        .flatMapLatest { (period, filter) ->
            (period?.let { filter.toMonthItemsFlow(it) } ?: flowOf(emptyList())).map { it.toUiState(filter) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    val annualChartState: StateFlow<AnnualChartUiState> = selectedYear
        .flatMapLatest { year -> itemsRepository.getYear(year) }
        .map { items ->
            val inflow = items.filter { it.io == FlowType.INFLOW.value }.sumOf { it.amountCents }
            val outflow = items.filter { it.io == FlowType.OUTFLOW.value }.sumOf { it.amountCents }
            AnnualChartUiState(inflow, outflow, inflow + outflow)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnnualChartUiState())

    fun selectMainFilter(filter: TransactionFilter) { savedStateHandle["main_filter"] = filter }
    fun selectMonthFilter(filter: TransactionFilter) { savedStateHandle["month_filter"] = filter }
    fun applyMainAllFilter() = selectMainFilter(TransactionFilter.ALL)
    fun applyMainFlowFilter(flow: String) = selectMainFilter(TransactionFilter.from(flow = flow))
    fun applyMainCategoryFilter(category: String) = selectMainFilter(TransactionFilter.from(category = category))
    fun selectYear(year: Int) = yearSelectionRepository.selectYear(year)

    fun applyMonthFilter(yearMonth: Int, category: String? = null, flow: String? = null) {
        savedStateHandle["month_period"] = yearMonth
        selectMonthFilter(TransactionFilter.from(flow, category))
    }

    fun initializeMonth(yearMonth: Int) {
        if (monthPeriod.value != yearMonth) applyMonthFilter(yearMonth)
    }

    fun insertItem(item: Items) = viewModelScope.launch(Dispatchers.IO) { itemsRepository.insertItem(item) }
    fun updateItem(item: Items) = viewModelScope.launch(Dispatchers.IO) { itemsRepository.updateItem(item) }
    fun deleteItem(id: Int) = viewModelScope.launch(Dispatchers.IO) { itemsRepository.deleteItem(id) }
    fun deleteItemMonth(yearMonth: Int) {
        val selectedYearMonth = selectedYear.value * 100 + (yearMonth % 100)
        viewModelScope.launch(Dispatchers.IO) { itemsRepository.deleteItemMonth(selectedYearMonth) }
    }

    private fun TransactionFilter.toYearItemsFlow(year: Int): Flow<List<Items>> = when {
        flow != null -> itemsRepository.getYearFlow(year, flow)
        category != null -> itemsRepository.getYearCategory(year, category)
        else -> itemsRepository.getYear(year)
    }

    private fun TransactionFilter.toMonthItemsFlow(yearMonth: Int): Flow<List<Items>> = when {
        flow != null -> itemsRepository.getMonthFlow(yearMonth, flow)
        category != null -> itemsRepository.getMonthCtg(yearMonth, category)
        else -> itemsRepository.getMonth(yearMonth)
    }

    private fun List<Items>.toUiState(filter: TransactionFilter): TransactionsUiState {
        val inflow = filter { it.io == FlowType.INFLOW.value }.sumOf { it.amountCents }
        val outflow = filter { it.io == FlowType.OUTFLOW.value }.sumOf { it.amountCents }
        return TransactionsUiState(
            items = this,
            balance = Money.format(inflow + outflow),
            inflow = Money.format(inflow),
            outflow = Money.format(outflow),
            filter = filter
        )
    }
}
