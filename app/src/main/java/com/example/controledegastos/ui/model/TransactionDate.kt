package com.example.controledegastos.ui.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TransactionDate {
    private val formatter get() = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    fun format(millis: Long): String = formatter.format(millis)

    fun yearMonth(millis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH) + 1
    }

    fun atStartOfSelectedDay(calendar: Calendar): Long {
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
