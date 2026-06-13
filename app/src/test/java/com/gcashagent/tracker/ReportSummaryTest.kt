package com.gcashagent.tracker

import com.gcashagent.tracker.core.domain.model.ReportSummary
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportSummaryTest {

    private fun txn(type: TransactionType, centavos: Long) =
        Transaction(gcashNumberId = 1, dateTime = 0, type = type, amountCentavos = centavos)

    @Test
    fun aggregatesCashInOutNetAndCount() {
        val txns = listOf(
            txn(TransactionType.SEND, 100_00),     // cash in
            txn(TransactionType.SEND, 50_00),      // cash in
            txn(TransactionType.RECEIVE, 30_00)    // cash out
        )
        val summary = ReportSummary.from(txns)

        assertEquals(150_00L, summary.totalCashInCentavos)
        assertEquals(30_00L, summary.totalCashOutCentavos)
        assertEquals(120_00L, summary.netCentavos)
        assertEquals(3, summary.transactionCount)
    }

    @Test
    fun emptyListYieldsZeroes() {
        val summary = ReportSummary.from(emptyList())
        assertEquals(0L, summary.totalCashInCentavos)
        assertEquals(0L, summary.totalCashOutCentavos)
        assertEquals(0L, summary.netCentavos)
        assertEquals(0, summary.transactionCount)
    }
}
