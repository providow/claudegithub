package com.gcashagent.tracker.core.domain.model

/**
 * The direction of a GCash transaction, from the agent's device.
 *
 *  - [SEND]    : the agent sent money out of their GCash wallet.
 *  - [RECEIVE] : the agent received money into their GCash wallet.
 */
enum class TransactionType {
    SEND,
    RECEIVE;

    /**
     * The single source of truth for the agent business rule:
     *
     *   agent SENDS money   -> CASH IN
     *   agent RECEIVES money -> CASH OUT
     *
     * This is the agent's perspective (not standard accounting). Every screen,
     * report and export derives cash-flow from this mapping so the rule can
     * never drift.
     */
    val cashFlow: CashFlow
        get() = when (this) {
            SEND -> CashFlow.CASH_IN
            RECEIVE -> CashFlow.CASH_OUT
        }

    val label: String
        get() = when (this) {
            SEND -> "Send"
            RECEIVE -> "Receive"
        }
}
