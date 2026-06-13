package com.gcashagent.tracker.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats Philippine Peso amounts as "₱1,234.50" with comma grouping and two
 * decimal places, independent of the device locale. Money is handled in
 * centavos (integer) to avoid floating-point error.
 */
object PesoFormatter {

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val format = DecimalFormat("#,##0.00", symbols)

    /** "₱1,234.50" from a centavo amount. */
    fun format(centavos: Long): String = "₱" + format.format(centavos / 100.0)

    /** Signed form, e.g. "−₱500.00" / "₱1,200.00", for net figures. */
    fun formatSigned(centavos: Long): String =
        if (centavos < 0) "−" + format(-centavos) else format(centavos)

    /** Plain number (no peso sign) for spreadsheet cells: 1234.50. */
    fun pesosValue(centavos: Long): Double = centavos / 100.0

    /**
     * Parse user input like "1,234.50" or "1234.5" into centavos.
     * Returns null if the text is not a valid non-negative amount.
     */
    fun parseToCentavos(input: String): Long? {
        val cleaned = input.trim().replace(",", "").removePrefix("₱").trim()
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value < BigDecimal.ZERO) return null
        return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }
}
