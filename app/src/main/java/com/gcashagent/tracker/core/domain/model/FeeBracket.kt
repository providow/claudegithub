package com.gcashagent.tracker.core.domain.model

/**
 * A single charge bracket for one GCash number and one cash-flow direction:
 * a transaction whose amount falls within [minCentavos]..[maxCentavos]
 * (inclusive) earns a fee of [feeCentavos]. Agents maintain their own brackets.
 */
data class FeeBracket(
    val id: Long = 0,
    val gcashNumberId: Long,
    val flow: CashFlow,
    val minCentavos: Long,
    val maxCentavos: Long,
    val feeCentavos: Long
)
