package com.gcashagent.tracker.core.domain.model

/**
 * Per-number, per-direction charge configuration. Absence of a stored config
 * means [ChargeMode.BRACKETS] (the default), so percentage is purely opt-in.
 *
 * [percentBasisPoints] stores the rate as basis points (100 = 1%, 250 = 2.5%)
 * to keep money math integer-only.
 */
data class ChargeConfig(
    val mode: ChargeMode = ChargeMode.BRACKETS,
    val percentBasisPoints: Long = 0,
    val minChargeCentavos: Long = 0
) {
    /** Rate as a human percentage, e.g. 2.5. */
    val percent: Double get() = percentBasisPoints / 100.0

    companion object {
        val DEFAULT = ChargeConfig()

        fun percentOf(percent: Double): Long = Math.round(percent * 100.0)
    }
}
