package com.gcashagent.tracker.core.domain.model

/** Agent-perspective cash flow classification of a transaction. */
enum class CashFlow {
    CASH_IN,
    CASH_OUT;

    val label: String
        get() = when (this) {
            CASH_IN -> "Cash In"
            CASH_OUT -> "Cash Out"
        }

    /** Inverse of [TransactionType.cashFlow]: the type that produces this cash flow. */
    val transactionType: TransactionType
        get() = when (this) {
            CASH_IN -> TransactionType.SEND
            CASH_OUT -> TransactionType.RECEIVE
        }
}
