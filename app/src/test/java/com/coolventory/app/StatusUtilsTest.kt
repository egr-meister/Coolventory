package com.coolventory.app

import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.model.ProductQuantityStatus
import com.coolventory.app.util.StatusUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatusUtilsTest {

    private val today = LocalDate.parse(TestData.TODAY)

    @Test
    fun freshStatus() {
        val p = TestData.product(expiryDate = "2026-07-30")
        assertEquals(ProductDateStatus.Fresh, StatusUtils.dateStatus(p, 3, today))
    }

    @Test
    fun soonStatus_withinThreshold() {
        val p = TestData.product(expiryDate = "2026-07-12")
        assertEquals(ProductDateStatus.Soon, StatusUtils.dateStatus(p, 3, today))
    }

    @Test
    fun dueTodayCountsAsSoon() {
        val p = TestData.product(expiryDate = TestData.TODAY)
        assertEquals(ProductDateStatus.Soon, StatusUtils.dateStatus(p, 3, today))
        assertEquals(0L, StatusUtils.daysUntilReview(p, today))
    }

    @Test
    fun expiredStatus() {
        val p = TestData.product(expiryDate = "2026-07-01")
        assertEquals(ProductDateStatus.Expired, StatusUtils.dateStatus(p, 3, today))
    }

    @Test
    fun noExpiryDateStatus() {
        val p = TestData.product(expiryDate = "")
        assertEquals(ProductDateStatus.NoExpiryDate, StatusUtils.dateStatus(p, 3, today))
    }

    @Test
    fun openedSoonStatus() {
        val p = TestData.product(expiryDate = "2026-08-30", openedDate = "2026-07-08", consumeWithin = 3)
        // openedExpiry = 2026-07-11 -> within threshold -> OpenedSoon
        assertEquals(ProductDateStatus.OpenedSoon, StatusUtils.dateStatus(p, 3, today))
    }

    @Test
    fun openedExpiredBeatsPackage() {
        val p = TestData.product(expiryDate = "2026-08-30", openedDate = "2026-07-01", consumeWithin = 3)
        // openedExpiry = 2026-07-04 -> before today -> Expired
        assertEquals(ProductDateStatus.Expired, StatusUtils.dateStatus(p, 3, today))
    }

    @Test
    fun effectiveReviewDateIsEarlierDate() {
        val p = TestData.product(expiryDate = "2026-08-01", openedDate = "2026-07-08", consumeWithin = 3)
        assertEquals(LocalDate.parse("2026-07-11"), StatusUtils.effectiveReviewDate(p))
    }

    @Test
    fun lifecycleUsedAndDiscardedOverride() {
        val used = TestData.product(expiryDate = "2026-07-01", lifecycle = ProductLifecycleState.Used)
        assertEquals(ProductDateStatus.Used, StatusUtils.dateStatus(used, 3, today))
        val discarded = TestData.product(expiryDate = "2026-07-30", lifecycle = ProductLifecycleState.Discarded)
        assertEquals(ProductDateStatus.Discarded, StatusUtils.dateStatus(discarded, 3, today))
    }

    @Test
    fun invalidDatesNeverCrashAndFallBack() {
        val p = TestData.product(expiryDate = "not-a-date")
        // Invalid expiry parses to null -> treated as no expiry date.
        assertEquals(ProductDateStatus.NoExpiryDate, StatusUtils.dateStatus(p, 3, today))
    }

    @Test
    fun invalidOpenedDateIgnored() {
        val p = TestData.product(expiryDate = "2026-07-30", openedDate = "13-13-13", consumeWithin = 2)
        assertEquals(ProductDateStatus.Fresh, StatusUtils.dateStatus(p, 3, today))
        assertNull(com.coolventory.app.util.DateUtils.openedExpiry("13-13-13", 2))
    }

    @Test
    fun runningLowStatus() {
        assertEquals(ProductQuantityStatus.RunningLow, StatusUtils.quantityStatus(TestData.product(quantity = 1.0, minimumQuantity = 2.0)))
        assertEquals(ProductQuantityStatus.Available, StatusUtils.quantityStatus(TestData.product(quantity = 5.0, minimumQuantity = 2.0)))
        assertEquals(ProductQuantityStatus.Empty, StatusUtils.quantityStatus(TestData.product(quantity = 0.0)))
        assertTrue(StatusUtils.isRunningLow(TestData.product(quantity = 2.0, minimumQuantity = 2.0)))
    }
}
