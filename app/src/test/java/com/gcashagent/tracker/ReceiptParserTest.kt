package com.gcashagent.tracker

import com.gcashagent.tracker.core.util.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptParserTest {

    @Test
    fun prefersLabeledAmountOverFeeAndTotal() {
        val text = """
            Sent via GCash
            Amount
            ₱1,000.00
            Convenience Fee
            ₱15.00
            Total Amount Sent
            ₱1,015.00
            Ref No. 1234 567 890123
        """.trimIndent()
        val r = ReceiptParser.parse(text)
        assertEquals(100000L, r.amountCentavos)
        assertEquals("1234567890123", r.referenceNumber)
    }

    @Test
    fun fallsBackToLargestWhenNoLabel() {
        val text = """
            GCash
            ₱500.00
            Received from JUAN D.
            Ref. No. 9876543210987
        """.trimIndent()
        val r = ReceiptParser.parse(text)
        assertEquals(50000L, r.amountCentavos)
        assertEquals("9876543210987", r.referenceNumber)
    }

    @Test
    fun handlesInlineLabelAndAmount() {
        val text = "Amount ₱2,500.50\nRef No 1112223334445"
        val r = ReceiptParser.parse(text)
        assertEquals(250050L, r.amountCentavos)
        assertEquals("1112223334445", r.referenceNumber)
    }

    @Test
    fun parsesAmountWithoutCurrencySymbol() {
        val text = "Amount\n1,234.00\nRef No.\n1234 5678 90123"
        val r = ReceiptParser.parse(text)
        assertEquals(123400L, r.amountCentavos)
        assertEquals("1234567890123", r.referenceNumber)
    }

    @Test
    fun refDigitsAreNotMistakenForAmount() {
        val text = "GCash transaction\nRef No 1234567890123"
        val r = ReceiptParser.parse(text)
        assertNull(r.amountCentavos)
        assertEquals("1234567890123", r.referenceNumber)
    }

    @Test
    fun returnsNullsWhenNothingFound() {
        val r = ReceiptParser.parse("GCash receipt\nThank you for using GCash")
        assertNull(r.amountCentavos)
        assertNull(r.referenceNumber)
    }
}
