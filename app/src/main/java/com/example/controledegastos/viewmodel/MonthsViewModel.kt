package com.example.controledegastos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controledegastos.data.model.FlowType
import com.example.controledegastos.data.repository.ItemsDataSource
import com.example.controledegastos.ui.model.MonthSummaryUi
import com.example.controledegastos.ui.model.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MonthsViewModel @Inject constructor(repository: ItemsDataSource) : ViewModel() {
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    val months: StateFlow<List<MonthSummaryUi>> = repository.allItems
        .map { transactions ->
            val year = transactions.maxOfOrNull { it.yearMonth / 100 }?.takeIf { it > 0 } ?: currentYear
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private companion object {
        val MONTH_NAMES = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
    }
}
