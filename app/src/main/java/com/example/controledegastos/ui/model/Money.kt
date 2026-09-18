package com.example.controledegastos.ui.model

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object Money {
    private val brazil = Locale("pt", "BR")
    private val inputPattern = Regex("(?:[0-9]+|[0-9]{1,3}(?:\\.[0-9]{3})+)(?:,[0-9]{1,2})?")

    // Each call owns its mutable formatter, including calls outside the UI thread.
    fun format(cents: Long): String =
        NumberFormat.getCurrencyInstance(brazil).format(BigDecimal.valueOf(cents, 2))

    fun formatInput(cents: Long): String =
        BigDecimal.valueOf(cents, 2).abs().toPlainString().replace('.', ',')

    /** Dot groups thousands; comma separates up to two cent digits. No rounding. */
    fun parseToCents(value: String): Long? {
        if (value.length > 64) return null
        val input = value.trim()
        if (!inputPattern.matches(input)) return null
        return try {
            BigDecimal(input.replace(".", "").replace(',', '.'))
                .movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }
}
