package com.gcashagent.tracker

import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.FeeBracket
import com.gcashagent.tracker.core.util.ChargeCalculator
import com.gcashagent.tracker.core.util.DefaultFeeTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class ChargeCalculatorTest {

    private fun bracket(minPeso: Long, maxPeso: Long, feePeso: Long) =
        FeeBracket(
            gcashNumberId = 1,
            flow = CashFlow.CASH_IN,
            minCentavos = minPeso * 100,
            maxCentavos = maxPeso * 100,
            feeCentavos = feePeso * 100
        )

    private val table = listOf(
        bracket(1, 899, 5),
        bracket(900, 1499, 10),
        bracket(1500, 1999, 15)
    )

    @Test
    fun returnsFeeForMatchingBracket() {
        assertEquals(500L, ChargeCalculator.chargeFor(table, 50000))   // ₱500 -> ₱5
        assertEquals(1000L, ChargeCalculator.chargeFor(table, 120000)) // ₱1,200 -> ₱10
        assertEquals(1500L, ChargeCalculator.chargeFor(table, 199900)) // ₱1,999 -> ₱15
    }

    @Test
    fun boundariesAreInclusive() {
        assertEquals(1000L, ChargeCalculator.chargeFor(table, 90000))  // exactly ₱900
        assertEquals(1000L, ChargeCalculator.chargeFor(table, 149900)) // exactly ₱1,499
    }

    @Test
    fun amountOutsideEveryBracketEarnsZero() {
        assertEquals(0L, ChargeCalculator.chargeFor(table, 500000))    // ₱5,000 -> no bracket
        assertEquals(0L, ChargeCalculator.chargeFor(emptyList(), 50000))
    }

    @Test
    fun defaultTemplateCoversTheSampleTable() {
        val brackets = DefaultFeeTemplate.bracketsCentavos().map { (min, max, fee) ->
            FeeBracket(gcashNumberId = 1, flow = CashFlow.CASH_IN, minCentavos = min, maxCentavos = max, feeCentavos = fee)
        }
        assertEquals(500L, ChargeCalculator.chargeFor(brackets, 89900))    // ₱899 -> ₱5
        assertEquals(9500L, ChargeCalculator.chargeFor(brackets, 999900))  // ₱9,999 -> ₱95
        assertEquals(0L, ChargeCalculator.chargeFor(brackets, 1000000))    // ₱10,000 -> none
    }
}
