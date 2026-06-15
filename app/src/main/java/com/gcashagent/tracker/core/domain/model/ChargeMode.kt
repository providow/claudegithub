package com.gcashagent.tracker.core.domain.model

/** How charges are computed for a GCash number and direction. */
enum class ChargeMode {
    /** Tiered fee brackets (amount range -> flat fee). */
    BRACKETS,

    /** A flat percentage of the amount, with an optional minimum charge. */
    PERCENT;

    val label: String
        get() = when (this) {
            BRACKETS -> "Brackets"
            PERCENT -> "Percentage"
        }
}
