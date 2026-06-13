package com.gcashagent.tracker

import com.gcashagent.tracker.core.util.PesoFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PesoFormatterTest {

    @Test
    fun formatsWithPesoSignCommasAndTwoDecimals() {
        assertEquals("₱1,234.50", PesoFormatter.format(123450))
        assertEquals("₱0.00", PesoFormatter.format(0))
        assertEquals("₱1,000,000.00", PesoFormatter.format(100_000_000))
    }

    @Test
    fun signedFormatUsesMinusForNegative() {
        assertEquals("−₱500.00", PesoFormatter.formatSigned(-50000))
        assertEquals("₱500.00", PesoFormatter.formatSigned(50000))
    }

    @Test
    fun parsesUserInputToCentavos() {
        assertEquals(123450L, PesoFormatter.parseToCentavos("1,234.50"))
        assertEquals(123450L, PesoFormatter.parseToCentavos("1234.5"))
        assertEquals(100000L, PesoFormatter.parseToCentavos("₱1,000"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(PesoFormatter.parseToCentavos(""))
        assertNull(PesoFormatter.parseToCentavos("abc"))
        assertNull(PesoFormatter.parseToCentavos("-5"))
    }
}
