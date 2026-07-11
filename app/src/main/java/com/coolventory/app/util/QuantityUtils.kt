package com.coolventory.app.util

import com.coolventory.app.model.QuantityUnit
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max

/**
 * Numeric quantity helpers. Quantities are stored as [Double]; localized formatting only happens in
 * the UI. Values are always clamped to be non-negative and within a reasonable maximum.
 */
object QuantityUtils {

    /** Generous upper bound to avoid absurd/overflow input while allowing bulk grams/ml. */
    const val MAX_QUANTITY = 1_000_000.0

    /**
     * Parse a user-entered quantity string. Accepts comma as decimal separator, tolerates a lone
     * trailing/leading separator during typing, and returns null for genuinely invalid input.
     * Blank input returns null (caller decides whether that is allowed).
     */
    fun parseQuantityOrNull(raw: String?): Double? {
        if (raw == null) return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        // Normalize comma to dot; strip spaces used as thousands separators.
        val normalized = trimmed.replace(",", ".").replace(" ", "")
        // Intermediate typing states like "." or "-" are not valid numbers yet.
        val value = normalized.toDoubleOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return value.coerceIn(0.0, MAX_QUANTITY)
    }

    /** Clamp any quantity into the allowed range and drop NaN/Infinity. */
    fun sanitize(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return value.coerceIn(0.0, MAX_QUANTITY)
    }

    /** Round to the unit's sensible number of decimals for display. */
    fun round(value: Double, unit: QuantityUnit): Double {
        val decimals = max(0, unit.maxDecimals)
        return BigDecimal(sanitize(value))
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }

    /** Format a quantity trimming trailing zeros: 2.0 -> "2", 1.50 -> "1.5". */
    fun format(value: Double, unit: QuantityUnit): String {
        val rounded = round(value, unit)
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            BigDecimal(rounded).stripTrailingZeros().toPlainString()
        }
    }

    /** Increase by the unit's default step (Custom has no auto-step). */
    fun increment(value: Double, unit: QuantityUnit): Double {
        if (unit == QuantityUnit.Custom) return sanitize(value)
        return round(sanitize(value) + unit.defaultStep, unit)
    }

    /** Decrease by the unit's default step, never below zero. */
    fun decrement(value: Double, unit: QuantityUnit): Double {
        if (unit == QuantityUnit.Custom) return sanitize(value)
        val next = sanitize(value) - unit.defaultStep
        return round(max(0.0, next), unit)
    }

    fun isZero(value: Double): Boolean = sanitize(value) <= 0.0
}
