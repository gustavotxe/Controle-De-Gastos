package com.example.controledegastos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controledegastos.data.model.FlowType
import com.example.controledegastos.data.repository.ItemsDataSource
import com.example.controledegastos.data.repository.YearSelectionRepository
import com.example.controledegastos.ui.model.MonthSummaryUi
import com.example.controledegastos.ui.model.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.controledegastos.di.AppDispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MonthsViewModel @Inject constructor(
    repository: ItemsDataSource,
    private val yearSelectionRepository: YearSelectionRepository,
    private val dispatchers: AppDispatchers = AppDispatchers()
) : ViewModel() {
    val availableYears: Flow<List<Int>> = yearSelectionRepository.availableYears
    val selectedYear = yearSelectionRepository.selectedYear

    val months: StateFlow<List<MonthSummaryUi>> = selectedYear
        .flatMapLatest { year -> repository.getYear(year)
            .map { year to it } }
        .map { (year, transactions) ->
            MONTH_NAMES.mapIndexed { index, monthName ->
                val yearMonth = year * 100 + index + 1
                val items = transactions.filter { it.yearMonth == yearMonth }
                val inflow = items.filter { it.io == FlowType.INFLOW.value }.sumOf { it.amountCents }
                val outflow = items.filter { it.io == FlowType.OUTFLOW.value }.sumOf { it.amountCents }
                MonthSummaryUi(
                    yearMonth = yearMonth,
                    monthName = monthName,
                    inflowText = Money.format(inflow),
                    outflowText = Money.format(outflow),
                    balanceText = Money.format(inflow + outflow),
                    inflowPie = inflow.coerceAtLeast(0).toFloat() / 100,
                    outflowPie = -outflow.coerceAtMost(0).toFloat() / 100,
                    hasData = items.isNotEmpty()
                )
            }
        }
        .flowOn(dispatchers.computation)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectYear(year: Int) = yearSelectionRepository.selectYear(year)

    private companion object {
        val MONTH_NAMES = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
    }
}
