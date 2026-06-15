package com.gcashagent.tracker

import com.gcashagent.tracker.core.util.FeeLadderGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeeLadderGeneratorTest {

    @Test
    fun generatesContiguousLadderInCentavos() {
        val out = FeeLadderGenerator.generate(startPeso = 1, stepPeso = 500, firstFeePeso = 5, feeStepPeso = 5, uptoPeso = 10000)
        assertEquals(20, out.size)
        // first bracket: ₱1–₱500 -> ₱5
        assertEquals(Triple(100L, 50000L, 500L), out.first())
        // last bracket: ₱9,501–₱10,000 -> ₱100
        assertEquals(Triple(950100L, 1000000L, 10000L), out.last())
        // contiguous: each min is previous max + ₱1 (100 centavos)
        for (i in 1 until out.size) {
            assertEquals(out[i - 1].second + 100L, out[i].first)
        }
    }

    @Test
    fun feeIncreasesByStepEachBracket() {
        val out = FeeLadderGenerator.generate(1, 1000, 10, 10, 5000)
        assertEquals(1000L, out[0].third)  // ₱10
        assertEquals(2000L, out[1].third)  // ₱20
        assertEquals(5000L, out[4].third)  // ₱50
    }

    @Test
    fun rejectsInvalidInput() {
        assertTrue(FeeLadderGenerator.generate(1, 0, 5, 5, 10000).isEmpty())   // step 0
        assertTrue(FeeLadderGenerator.generate(5000, 500, 5, 5, 100).isEmpty()) // upto < start
    }
}
