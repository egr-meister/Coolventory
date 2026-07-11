package com.coolventory.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Date / time helpers. Everything here is null-safe and never throws into the UI. Expiry math uses
 * [LocalDate] only (no timezone-dependent instants) so a "day" is a calendar day on the device.
 */
object DateUtils {

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd
    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Today on the device. Injectable indirectly via callers for tests. */
    fun today(): LocalDate = LocalDate.now()

    fun nowTime(): String = LocalDateTime.now().format(TIME_FORMAT)

    /** ISO-8601 timestamp for createdAt/updatedAt. */
    fun nowIso(): String = LocalDateTime.now().toString()

    fun todayString(): String = today().format(DATE_FORMAT)

    /** Parse a YYYY-MM-DD string, returning null on any problem (blank, malformed, impossible date). */
    fun parseOrNull(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value.trim(), DATE_FORMAT)
        } catch (e: Exception) {
            null
        }
    }

    fun isValidDate(value: String?): Boolean = parseOrNull(value) != null

    fun format(date: LocalDate?): String = date?.format(DATE_FORMAT) ?: ""

    /** Friendly display for a stored date string; falls back gracefully. */
    fun displayDate(value: String?): String {
        if (value.isNullOrBlank()) return "—"
        val parsed = parseOrNull(value) ?: return "Date unavailable"
        return parsed.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }

    /** Whole days from [from] until [to]. Negative when [to] is in the past. Null if either invalid. */
    fun daysBetween(from: LocalDate?, to: LocalDate?): Long? {
        if (from == null || to == null) return null
        return ChronoUnit.DAYS.between(from, to)
    }

    fun daysUntil(target: LocalDate?, reference: LocalDate = today()): Long? =
        daysBetween(reference, target)

    fun daysSince(target: LocalDate?, reference: LocalDate = today()): Long? =
        daysBetween(target, reference)

    /** openedDate + consumeWithinDaysAfterOpening, or null when inputs are insufficient/invalid. */
    fun openedExpiry(openedDate: String?, consumeWithinDays: Int?): LocalDate? {
        val opened = parseOrNull(openedDate) ?: return null
        val days = consumeWithinDays ?: return null
        if (days <= 0) return null
        return try {
            opened.plusDays(days.toLong())
        } catch (e: Exception) {
            null
        }
    }

    /** Year-month key (yyyy-MM) for grouping history, or "unknown". */
    fun monthKey(dateString: String?): String {
        val parsed = parseOrNull(dateString) ?: return "unknown"
        return parsed.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    fun monthLabel(dateString: String?): String {
        val parsed = parseOrNull(dateString) ?: return "Unknown date"
        return parsed.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
}
