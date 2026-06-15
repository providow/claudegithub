package com.gcashagent.tracker.core.util

/**
 * The common GCash agent fee schedule used to seed a new number's charge tables.
 * Each entry is (minPeso, maxPeso, feePeso); agents can edit/add/remove brackets
 * afterwards. Values mirror the standard ₱5-per-₱500 ladder.
 */
object DefaultFeeTemplate {

    private val PESO = listOf(
        Triple(1, 899, 5),
        Triple(900, 1499, 10),
        Triple(1500, 1999, 15),
        Triple(2000, 2499, 20),
        Triple(2500, 2999, 25),
        Triple(3000, 3499, 30),
        Triple(3500, 3999, 35),
        Triple(4000, 4499, 40),
        Triple(4500, 4999, 45),
        Triple(5000, 5499, 50),
        Triple(5500, 5999, 55),
        Triple(6000, 6499, 60),
        Triple(6500, 6999, 65),
        Triple(7000, 7499, 70),
        Triple(7500, 7999, 75),
        Triple(8000, 8499, 80),
        Triple(8500, 8999, 85),
        Triple(9000, 9499, 90),
        Triple(9500, 9999, 95)
    )

    /** Brackets as (minCentavos, maxCentavos, feeCentavos). */
    fun bracketsCentavos(): List<Triple<Long, Long, Long>> =
        PESO.map { (lo, hi, fee) -> Triple(lo * 100L, hi * 100L, fee * 100L) }
}
