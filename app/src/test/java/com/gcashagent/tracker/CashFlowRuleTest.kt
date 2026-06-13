package com.gcashagent.tracker

import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Locks in the core agent business rule. */
class CashFlowRuleTest {

    @Test
    fun agentSend_isCashIn() {
        assertEquals(CashFlow.CASH_IN, TransactionType.SEND.cashFlow)
    }

    @Test
    fun agentReceive_isCashOut() {
        assertEquals(CashFlow.CASH_OUT, TransactionType.RECEIVE.cashFlow)
    }
}
