package com.example.controledegastos

import com.example.controledegastos.ui.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyTest {
    @Test fun `parses Brazilian reais into exact cents`() {
        mapOf("7.000" to 700_000L, "7.000,00" to 700_000L,
            "7000" to 700_000L, "7000,00" to 700_000L,
            "1.234.567,89" to 123_456_789L, "1234,56" to 123_456L,
            "7,5" to 750L, "0,01" to 1L, "0,00" to 0L, " 25,90 " to 2590L,
            "90.071.992.547.409,93" to 9_007_199_254_740_993L,
            "92.233.720.368.547.758,07" to Long.MAX_VALUE
        ).forEach { (text, cents) -> assertEquals(text, cents, Money.parseToCents(text)) }
    }

    @Test fun `rejects malformed amounts instead of rounding or changing their meaning`() {
        listOf("", ",", "7.00", "7,000", "7,000.00", "1.23.456", "7.", "7,",
            "-7,00", "+7", "1e3", "NaN", "7 000", "R$ 7,00", "7,,00",
            "92.233.720.368.547.758,08", "1".repeat(1000)
        ).forEach { assertNull(it, Money.parseToCents(it)) }
    }

    @Test fun `editing preserves cents without floating point conversions`() {
        assertEquals("7000,00", Money.formatInput(-700_000))
        listOf(1L, 700_000L, 9_007_199_254_740_993L, Long.MAX_VALUE).forEach {
            assertEquals(it, Money.parseToCents(Money.formatInput(it)))
        }
    }

    @Test fun `currency is BRL regardless of device locale and retains exact cents`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("R$ 7.000,00", normalize(Money.format(700_000)))
            assertEquals("R$ 90.071.992.547.409,93", normalize(Money.format(9_007_199_254_740_993L)))
        } finally { Locale.setDefault(previous) }
    }

    private fun normalize(text: String) = text.replace('\u00a0', ' ').replace('\u202f', ' ')
}
