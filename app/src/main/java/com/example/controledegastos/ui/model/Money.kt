package com.example.controledegastos.ui.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

object Money {
    fun format(cents: Long): String = NumberFormat.getCurrencyInstance().format(cents / 100.0)

    fun parseToCents(value: String): Long? = runCatching {
        BigDecimal(value.trim().replace(',', '.'))
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()
}
