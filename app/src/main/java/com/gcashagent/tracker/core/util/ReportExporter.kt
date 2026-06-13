package com.gcashagent.tracker.core.util

import android.content.Context
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.domain.model.ReportSummary
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.core.util.ExcelWriter.Cell
import java.io.File

/**
 * Builds the .xlsx report from a set of transactions and writes it into
 * filesDir/exports so it can be shared through the app FileProvider.
 */
class ReportExporter(private val context: Context) {

    /**
     * @param scopeLabel  e.g. "Main (0991 209 3084)" or "All Numbers".
     * @param numbersById used to label each row's number in the combined report.
     * @return the written .xlsx file.
     */
    fun export(
        scopeLabel: String,
        range: DateRange,
        transactions: List<Transaction>,
        numbersById: Map<Long, GCashNumber>
    ): File {
        val summary = ReportSummary.from(transactions)
        val rows = mutableListOf<List<Cell>>()

        rows += listOf(ExcelWriter.cell("GCash Agent Tracker — Report"))
        rows += listOf(ExcelWriter.cell("Account"), ExcelWriter.cell(scopeLabel))
        rows += listOf(
            ExcelWriter.cell("Period"),
            ExcelWriter.cell(
                if (range.isSingleDay) PhDateTime.formatDate(range.startMillis)
                else "${PhDateTime.formatDate(range.startMillis)} – ${PhDateTime.formatDate(range.endExclusiveMillis - 1)}"
            )
        )
        rows += emptyList()
        rows += listOf(ExcelWriter.cell("Total Cash In"), ExcelWriter.cell(PesoFormatter.pesosValue(summary.totalCashInCentavos)))
        rows += listOf(ExcelWriter.cell("Total Cash Out"), ExcelWriter.cell(PesoFormatter.pesosValue(summary.totalCashOutCentavos)))
        rows += listOf(ExcelWriter.cell("Net (In − Out)"), ExcelWriter.cell(PesoFormatter.pesosValue(summary.netCentavos)))
        rows += listOf(ExcelWriter.cell("Transactions"), ExcelWriter.cell(summary.transactionCount.toDouble()))
        rows += emptyList()

        rows += listOf(
            ExcelWriter.cell("Date"),
            ExcelWriter.cell("Time"),
            ExcelWriter.cell("GCash Number"),
            ExcelWriter.cell("Type"),
            ExcelWriter.cell("Cash Flow"),
            ExcelWriter.cell("Amount"),
            ExcelWriter.cell("From / To"),
            ExcelWriter.cell("Reference No.")
        )

        for (t in transactions) {
            val number = numbersById[t.gcashNumberId]
            val numberLabel = number?.let { "${it.alias} (${it.formattedNumber})" } ?: "—"
            rows += listOf(
                ExcelWriter.cell(PhDateTime.formatDate(t.dateTime)),
                ExcelWriter.cell(PhDateTime.formatTime(t.dateTime)),
                ExcelWriter.cell(numberLabel),
                ExcelWriter.cell(t.type.label),
                ExcelWriter.cell(if (t.cashFlow == CashFlow.CASH_IN) "Cash In" else "Cash Out"),
                ExcelWriter.cell(PesoFormatter.pesosValue(t.amountCentavos)),
                ExcelWriter.cell(t.counterpartyNumber ?: ""),
                ExcelWriter.cell(t.referenceNumber ?: "")
            )
        }

        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeScope = scopeLabel.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "report" }
        val fileName = "GCash_${safeScope}_${PhDateTime.formatForFileName(range.startMillis)}-${PhDateTime.formatForFileName(range.endExclusiveMillis - 1)}.xlsx"
        val file = File(dir, fileName)
        ExcelWriter.write(file, listOf(ExcelWriter.Sheet("Report", rows)))
        return file
    }
}
