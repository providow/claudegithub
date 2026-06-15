package com.gcashagent.tracker.core.util

/**
 * Generates a contiguous, linear charge ladder from a simple rule, so an agent
 * can express their own fee scheme in a few inputs instead of typing every
 * bracket. Inputs are whole pesos; output brackets are (minCentavos,
 * maxCentavos, feeCentavos), contiguous like 1–500, 501–1000, …
 */
object FeeLadderGenerator {

    private const val MAX_BRACKETS = 1000

    /**
     * @param startPeso  first bracket's starting amount (e.g. 1)
     * @param stepPeso   bracket width in pesos (e.g. 500)
     * @param firstFeePeso charge for the first bracket (e.g. 5)
     * @param feeStepPeso  how much the charge increases each bracket (e.g. 5)
     * @param uptoPeso   generate brackets until this amount is covered
     * @return brackets as (minCentavos, maxCentavos, feeCentavos), or empty if inputs are invalid.
     */
    fun generate(
        startPeso: Long,
        stepPeso: Long,
        firstFeePeso: Long,
        feeStepPeso: Long,
        uptoPeso: Long
    ): List<Triple<Long, Long, Long>> {
        if (stepPeso <= 0 || startPeso < 0 || uptoPeso < startPeso) return emptyList()
        if (firstFeePeso < 0 || feeStepPeso < 0) return emptyList()

        val out = ArrayList<Triple<Long, Long, Long>>()
        var i = 0L
        while (out.size < MAX_BRACKETS) {
            val minPeso = startPeso + i * stepPeso
            if (minPeso > uptoPeso) break
            val maxPeso = minPeso + stepPeso - 1
            val feePeso = firstFeePeso + i * feeStepPeso
            out += Triple(minPeso * 100L, maxPeso * 100L, feePeso * 100L)
            i++
        }
        return out
    }
}
