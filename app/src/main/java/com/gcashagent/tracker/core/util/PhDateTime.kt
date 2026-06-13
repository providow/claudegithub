package com.gcashagent.tracker.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Philippine-locale date/time formatting. All display uses the Asia/Manila
 * time zone so reports line up with the agent's business day.
 */
object PhDateTime {

    val ZONE: ZoneId = ZoneId.of("Asia/Manila")
    private val LOCALE: Locale = Locale.forLanguageTag("en-PH")

    private val dateTimeFmt = DateTimeFormatter.ofPattern("MMM d, yyyy '·' h:mm a", LOCALE)
    private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", LOCALE)
    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", LOCALE)
    private val dayHeaderFmt = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", LOCALE)
    private val fileFmt = DateTimeFormatter.ofPattern("yyyyMMdd", LOCALE)

    /** "Jun 12, 2026 · 4:32 PM" */
    fun formatDateTime(epochMillis: Long): String = local(epochMillis).format(dateTimeFmt)

    /** "Jun 12, 2026" */
    fun formatDate(epochMillis: Long): String = local(epochMillis).format(dateFmt)

    /** "4:32 PM" */
    fun formatTime(epochMillis: Long): String = local(epochMillis).format(timeFmt)

    /** "Friday, Jun 12, 2026" — used as a section header in lists. */
    fun formatDayHeader(epochMillis: Long): String = local(epochMillis).format(dayHeaderFmt)

    /** "20260612" — for export file names. */
    fun formatForFileName(epochMillis: Long): String = local(epochMillis).format(fileFmt)

    /** The local calendar date for an instant, in Manila time. */
    fun toLocalDate(epochMillis: Long): LocalDate = local(epochMillis).toLocalDate()

    private fun local(epochMillis: Long) =
        Instant.ofEpochMilli(epochMillis).atZone(ZONE)
}
