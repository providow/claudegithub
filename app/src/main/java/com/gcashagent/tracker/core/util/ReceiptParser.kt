package com.gcashagent.tracker.core.util

/**
 * Pure, dependency-free parser that pulls the transaction amount and reference
 * number out of the raw text recognized from a GCash screenshot. Kept separate
 * from the ML Kit OCR call so the heuristics can be unit-tested on the JVM.
 *
 * Heuristics (GCash receipts vary, so results are best-effort and the UI keeps
 * them editable):
 *  - Amount: prefer a peso value tied to an "Amount" label that is not a
 *    "Total" or "Fee" line; otherwise fall back to the largest peso value,
 *    ignoring values on fee/total lines.
 *  - Reference: the 13-digit GCash reference (often shown space-grouped), taken
 *    from a "Ref No." label when present, else any standalone 13-digit run.
 */
object ReceiptParser {

    data class ParsedReceipt(val amountCentavos: Long?, val referenceNumber: String?)

    // A peso value with two decimals, optional thousands separators and currency prefix.
    private val MONEY = Regex("""(?i)(?:₱|php|p)?\s*((?:\d{1,3}(?:,\d{3})+|\d+)\.\d{2})""")
    private val REF = Regex("""(?i)ref(?:erence)?\.?\s*(?:no\.?|number|#)?\s*[:#]?\s*(\d[\d ]{9,18}\d)""")
    private val DIGIT_RUN = Regex("""(?<!\d)(\d[\d ]{11,17}\d)(?!\d)""")

    fun parse(text: String): ParsedReceipt =
        ParsedReceipt(parseAmount(text), parseRef(text))

    private fun parseAmount(text: String): Long? {
        val lines = text.lines()

        // Pass 1: a money value on (or just under) an "Amount" label, excluding fee/total lines.
        val labeled = mutableListOf<Long>()
        lines.forEachIndexed { i, line ->
            val money = MONEY.find(line) ?: return@forEachIndexed
            val context = (line + " " + (lines.getOrNull(i - 1) ?: "")).lowercase()
            if ("amount" in context && "total" !in context && "fee" !in context) {
                PesoFormatter.parseToCentavos(money.groupValues[1])?.let { labeled += it }
            }
        }
        labeled.maxOrNull()?.let { return it }

        // Pass 2: largest peso value that isn't on a fee/total line.
        val candidates = mutableListOf<Long>()
        for (line in lines) {
            val lower = line.lowercase()
            if ("fee" in lower || "total" in lower) continue
            for (m in MONEY.findAll(line)) {
                PesoFormatter.parseToCentavos(m.groupValues[1])?.let { candidates += it }
            }
        }
        if (candidates.isNotEmpty()) return candidates.max()

        // Pass 3: any peso value at all.
        return MONEY.findAll(text)
            .mapNotNull { PesoFormatter.parseToCentavos(it.groupValues[1]) }
            .maxOrNull()
    }

    private fun parseRef(text: String): String? {
        REF.find(text)?.groupValues?.get(1)?.digitsOnly()?.let { if (it.isValidRef()) return it }
        return DIGIT_RUN.findAll(text)
            .map { it.groupValues[1].digitsOnly() }
            .firstOrNull { it.length == 13 }
    }

    private fun String.digitsOnly() = filter { it.isDigit() }
    private fun String.isValidRef() = length in 10..15
}
