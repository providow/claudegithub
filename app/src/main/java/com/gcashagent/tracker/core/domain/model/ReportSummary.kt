package com.gcashagent.tracker.core.domain.model

/**
 * Aggregated totals for a set of transactions (a single number or all numbers)
 * over a date range. All money values are in centavos.
 */
data class ReportSummary(
    val totalCashInCentavos: Long,
    val totalCashOutCentavos: Long,
    val transactionCount: Int
) {
    /** Net position from the agent's perspective: CASH IN − CASH OUT. */
    val netCentavos: Long get() = totalCashInCentavos - totalCashOutCentavos

    companion object {
        val EMPTY = ReportSummary(0, 0, 0)

        /** Build a summary from raw transactions using the central cash-flow rule. */
        fun from(transactions: List<Transaction>): ReportSummary {
            var cashIn = 0L
            var cashOut = 0L
            for (t in transactions) {
                when (t.cashFlow) {
                    CashFlow.CASH_IN -> cashIn += t.amountCentavos
                    CashFlow.CASH_OUT -> cashOut += t.amountCentavos
                }
            }
            return ReportSummary(cashIn, cashOut, transactions.size)
        }
    }
}
