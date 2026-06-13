package com.gcashagent.tracker.core.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

/**
 * A half-open [start, endExclusive) range in epoch millis, derived from Manila
 * calendar dates so a "day" matches the agent's local business day.
 */
data class DateRange(
    val startMillis: Long,
    val endExclusiveMillis: Long,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    companion object {
        private fun startOfDay(date: LocalDate): Long =
            date.atStartOfDay(PhDateTime.ZONE).toInstant().toEpochMilli()

        /** Inclusive from/to calendar dates -> half-open millis range. */
        fun of(from: LocalDate, to: LocalDate): DateRange {
            val lo = if (from.isAfter(to)) to else from
            val hi = if (from.isAfter(to)) from else to
            return DateRange(
                startMillis = startOfDay(lo),
                endExclusiveMillis = startOfDay(hi.plusDays(1)),
                startDate = lo,
                endDate = hi
            )
        }

        fun today(today: LocalDate = LocalDate.now(PhDateTime.ZONE)): DateRange =
            of(today, today)

        fun thisWeek(today: LocalDate = LocalDate.now(PhDateTime.ZONE)): DateRange {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return of(monday, today)
        }

        fun thisMonth(today: LocalDate = LocalDate.now(PhDateTime.ZONE)): DateRange {
            val first = today.withDayOfMonth(1)
            return of(first, today)
        }
    }

    val isSingleDay: Boolean get() = ChronoUnit.DAYS.between(startDate, endDate) == 0L
}
