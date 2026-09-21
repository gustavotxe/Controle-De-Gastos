package com.example.controledegastos.data.repository

import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class YearSelectionRepository @Inject constructor(
    itemsDataSource: ItemsDataSource
) {
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val mutableSelectedYear = MutableStateFlow(currentYear)

    val selectedYear: StateFlow<Int> = mutableSelectedYear
    val availableYears: Flow<List<Int>> = itemsDataSource.availableYears.map { years ->
        (years + currentYear).distinct().sortedDescending()
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    fun selectYear(year: Int) {
        if (year > 0) mutableSelectedYear.value = year
    }
}
