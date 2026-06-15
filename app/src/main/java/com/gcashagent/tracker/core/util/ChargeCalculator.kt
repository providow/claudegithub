package com.gcashagent.tracker.core.util

import com.gcashagent.tracker.core.domain.model.FeeBracket

/**
 * Pure charge lookup: the fee for an amount is the first bracket whose
 * [FeeBracket.minCentavos]..[FeeBracket.maxCentavos] range contains it. Amounts
 * with no matching bracket earn ₱0 (the agent adds brackets as needed).
 */
object ChargeCalculator {
    fun chargeFor(brackets: List<FeeBracket>, amountCentavos: Long): Long =
        brackets.firstOrNull { amountCentavos in it.minCentavos..it.maxCentavos }?.feeCentavos ?: 0L
}
