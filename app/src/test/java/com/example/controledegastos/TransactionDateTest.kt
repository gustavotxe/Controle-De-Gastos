package com.example.controledegastos

import com.example.controledegastos.ui.model.TransactionDate
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionDateTest {
    @Test fun `selected leap day and year boundaries retain their local date across time zones`() {
        val previous = TimeZone.getDefault()
        try {
            for (zone in listOf("UTC", "America/Sao_Paulo", "Pacific/Kiritimati", "America/Los_Angeles")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                for ((year, month, day) in listOf(Triple(2024, 2, 29), Triple(2024, 12, 31), Triple(2025, 1, 1))) {
                    val calendar = Calendar.getInstance().apply { clear(); set(year, month - 1, day, 23, 59, 59) }
                    val millis = TransactionDate.atStartOfSelectedDay(calendar)
                    assertEquals(zone, year * 100 + month, TransactionDate.yearMonth(millis))
                    assertEquals(zone, "%02d/%02d/%04d".format(Locale.ROOT, day, month, year), TransactionDate.format(millis))
                    assertEquals(0, calendar.get(Calendar.MILLISECOND))
                }
            }
        } finally { TimeZone.setDefault(previous) }
    }
}
