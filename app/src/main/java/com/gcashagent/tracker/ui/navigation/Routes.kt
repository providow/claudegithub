package com.gcashagent.tracker.ui.navigation

/** Centralized navigation routes and argument keys. */
object Routes {
    const val NUMBERS = "numbers"

    const val ARG_NUMBER_ID = "numberId"
    const val ARG_TRANSACTION_ID = "transactionId"

    /** Sentinel meaning "all numbers" for the combined report. */
    const val ALL_NUMBERS = -1L

    const val TRANSACTIONS = "transactions/{$ARG_NUMBER_ID}"
    fun transactions(numberId: Long) = "transactions/$numberId"

    const val TRANSACTION_ENTRY = "entry/{$ARG_NUMBER_ID}?$ARG_TRANSACTION_ID={$ARG_TRANSACTION_ID}"
    fun transactionEntry(numberId: Long, transactionId: Long? = null) =
        "entry/$numberId" + (transactionId?.let { "?$ARG_TRANSACTION_ID=$it" } ?: "")

    const val REPORT = "report/{$ARG_NUMBER_ID}"
    fun report(numberId: Long) = "report/$numberId"
}
