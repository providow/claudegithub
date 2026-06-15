package com.gcashagent.tracker.core.domain.model

/**
 * A single recorded GCash transaction.
 *
 * [amountCentavos] is stored as an integer number of centavos to avoid any
 * floating-point rounding error in money math. Use [amountPesos] for display.
 */
data class Transaction(
    val id: Long = 0,
    val gcashNumberId: Long,
    val dateTime: Long,
    val type: TransactionType,
    val amountCentavos: Long,
    /** The agent's charge (income) for this transaction, snapshotted at entry. */
    val chargeCentavos: Long = 0,
    val counterpartyNumber: String? = null,
    val referenceNumber: String? = null,
    val screenshotPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val cashFlow: CashFlow get() = type.cashFlow

    val amountPesos: Double get() = amountCentavos / 100.0
}
