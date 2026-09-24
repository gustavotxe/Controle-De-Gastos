package com.example.controledegastos

import com.example.controledegastos.data.repository.YearSelectionRepository
import com.example.controledegastos.di.AppDispatchers
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class YearSelectionRepositoryTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `years include current year once and invalid selection is ignored`() = runTest {
        val source = FakeItemsDataSource()
        val current = Calendar.getInstance().get(Calendar.YEAR)
        source.availableYears.value = listOf(current, current - 2, current - 2, current + 1)
        val repository = YearSelectionRepository(source, AppDispatchers(main.dispatcher, main.dispatcher))
        assertEquals(current, repository.selectedYear.value)
        assertEquals(listOf(current + 1, current, current - 2), repository.availableYears.first())
        repository.selectYear(2024)
        repository.selectYear(0)
        repository.selectYear(-1)
        assertEquals(2024, repository.selectedYear.value)
        source.availableYears.value = emptyList()
        assertEquals(listOf(current), repository.availableYears.first())
    }
}
