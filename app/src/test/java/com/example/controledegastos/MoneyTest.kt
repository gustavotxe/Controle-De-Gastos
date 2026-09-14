package com.example.controledegastos

import com.example.controledegastos.ui.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun `parses monetary values exactly into cents`() {
        assertEquals(123_456L, Money.parseToCents("1234,56"))
        assertEquals(1L, Money.parseToCents("0.005"))
    }
}
