package com.example.controledegastos.data.repository

import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/** Shared, in-memory selection used by every financial report screen. */
@Singleton
class YearSelectionRepository @Inject constructor(
    itemsDataSource: ItemsDataSource
) {
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val mutableSelectedYear = MutableStateFlow(currentYear)

    val selectedYear: StateFlow<Int> = mutableSelectedYear
    val availableYears: Flow<List<Int>> = itemsDataSource.allItems.map { items ->
        (items.map { it.yearMonth / 100 }.filter { it > 0 } + currentYear).distinct().sortedDescending()
    }

    fun selectYear(year: Int) {
        if (year > 0) mutableSelectedYear.value = year
    }
}
