package com.gcashagent.tracker.core.util

import com.gcashagent.tracker.core.domain.model.ChargeConfig
import com.gcashagent.tracker.core.domain.model.ChargeMode
import com.gcashagent.tracker.core.domain.model.FeeBracket

/**
 * Pure charge computation. In [ChargeMode.BRACKETS] the fee is the first bracket
 * whose range contains the amount (₱0 if none match). In [ChargeMode.PERCENT]
 * the fee is a rounded percentage of the amount, floored at the configured
 * minimum charge.
 */
object ChargeCalculator {

    fun chargeFor(brackets: List<FeeBracket>, amountCentavos: Long): Long =
        brackets.firstOrNull { amountCentavos in it.minCentavos..it.maxCentavos }?.feeCentavos ?: 0L

    fun charge(config: ChargeConfig, brackets: List<FeeBracket>, amountCentavos: Long): Long =
        when (config.mode) {
            ChargeMode.BRACKETS -> chargeFor(brackets, amountCentavos)
            ChargeMode.PERCENT -> {
                if (amountCentavos <= 0L) 0L
                else {
                    // round half up: (amount * bp) / 10000
                    val pct = (amountCentavos * config.percentBasisPoints + 5000) / 10000
                    maxOf(pct, config.minChargeCentavos)
                }
            }
        }
}
