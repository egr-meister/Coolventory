package com.coolventory.app

import com.coolventory.app.model.QuantityUnit
import com.coolventory.app.util.QuantityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantityUtilsTest {

    @Test
    fun parsesCommaAsDecimal() {
        assertEquals(1.5, QuantityUtils.parseQuantityOrNull("1,5")!!, 0.0001)
    }

    @Test
    fun blankAndInvalidReturnNull() {
        assertNull(QuantityUtils.parseQuantityOrNull(""))
        assertNull(QuantityUtils.parseQuantityOrNull("."))
        assertNull(QuantityUtils.parseQuantityOrNull("abc"))
        assertNull(QuantityUtils.parseQuantityOrNull(null))
    }

    @Test
    fun negativeIsClampedToZero() {
        assertEquals(0.0, QuantityUtils.parseQuantityOrNull("-5")!!, 0.0001)
    }

    @Test
    fun incrementUsesUnitStep() {
        assertEquals(2.0, QuantityUtils.increment(1.0, QuantityUnit.Pieces), 0.0001)
        assertEquals(150.0, QuantityUtils.increment(100.0, QuantityUnit.Grams), 0.0001)
        assertEquals(0.2, QuantityUtils.increment(0.1, QuantityUnit.Liters), 0.0001)
    }

    @Test
    fun decrementNeverGoesNegative() {
        assertEquals(0.0, QuantityUtils.decrement(0.0, QuantityUnit.Pieces), 0.0001)
        assertEquals(0.0, QuantityUtils.decrement(30.0, QuantityUnit.Grams), 0.0001) // 30-50 -> 0
    }

    @Test
    fun customUnitHasNoAutoStep() {
        assertEquals(3.0, QuantityUtils.increment(3.0, QuantityUnit.Custom), 0.0001)
        assertEquals(3.0, QuantityUtils.decrement(3.0, QuantityUnit.Custom), 0.0001)
    }

    @Test
    fun formatTrimsTrailingZeros() {
        assertEquals("2", QuantityUtils.format(2.0, QuantityUnit.Pieces))
        assertEquals("1.5", QuantityUtils.format(1.5, QuantityUnit.Liters))
    }

    @Test
    fun sanitizeHandlesNaNAndInfinity() {
        assertEquals(0.0, QuantityUtils.sanitize(Double.NaN), 0.0001)
        assertTrue(QuantityUtils.sanitize(Double.POSITIVE_INFINITY) <= QuantityUtils.MAX_QUANTITY)
    }
}
