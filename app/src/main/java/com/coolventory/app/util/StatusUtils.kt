package com.coolventory.app.util

import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.model.ProductQuantityStatus
import java.time.LocalDate

/**
 * Pure, side-effect-free status calculation. Date-based statuses are ORGANIZATIONAL reminders only;
 * they never assert anything about food safety.
 */
object StatusUtils {

    const val DEFAULT_SOON_THRESHOLD_DAYS = 3
    val SOON_THRESHOLD_OPTIONS = listOf(1, 3, 5, 7, 14)

    /**
     * The effective "review date" a product should be judged against: the earlier of the package
     * expiry date and the opened-product expiry date (openedDate + consumeWithinDays), when both
     * exist. Returns null when neither is available.
     */
    fun effectiveReviewDate(product: FoodProduct): LocalDate? {
        val packageExpiry = DateUtils.parseOrNull(product.expiryDate)
        val openedExpiry = DateUtils.openedExpiry(product.openedDate, product.consumeWithinDaysAfterOpening)
        return when {
            packageExpiry != null && openedExpiry != null ->
                if (openedExpiry.isBefore(packageExpiry)) openedExpiry else packageExpiry
            packageExpiry != null -> packageExpiry
            openedExpiry != null -> openedExpiry
            else -> null
        }
    }

    /** True when the opened-product date is the governing (earlier) date. */
    fun openedDateGoverns(product: FoodProduct): Boolean {
        val openedExpiry = DateUtils.openedExpiry(product.openedDate, product.consumeWithinDaysAfterOpening)
            ?: return false
        val packageExpiry = DateUtils.parseOrNull(product.expiryDate)
        return packageExpiry == null || !openedExpiry.isAfter(packageExpiry)
    }

    /**
     * Derived date status, following the required evaluation order.
     */
    fun dateStatus(
        product: FoodProduct,
        soonThresholdDays: Int = DEFAULT_SOON_THRESHOLD_DAYS,
        today: LocalDate = DateUtils.today(),
    ): ProductDateStatus {
        // 1 & 2: lifecycle overrides.
        when (product.lifecycleState) {
            ProductLifecycleState.Used -> return ProductDateStatus.Used
            ProductLifecycleState.Discarded -> return ProductDateStatus.Discarded
            ProductLifecycleState.Active -> Unit
        }

        val threshold = soonThresholdDays.coerceAtLeast(0)
        val openedExpiry = DateUtils.openedExpiry(product.openedDate, product.consumeWithinDaysAfterOpening)

        // 3 & 4: opened-product effective date takes priority when present.
        if (openedExpiry != null) {
            if (openedExpiry.isBefore(today)) return ProductDateStatus.Expired
            val daysToOpened = DateUtils.daysBetween(today, openedExpiry) ?: 0
            if (daysToOpened <= threshold) return ProductDateStatus.OpenedSoon
            // otherwise fall through to package expiry evaluation below
        }

        // 5: no package expiry date.
        val packageExpiry = DateUtils.parseOrNull(product.expiryDate)
            ?: return ProductDateStatus.NoExpiryDate

        // 6: package expiry already passed.
        if (packageExpiry.isBefore(today)) return ProductDateStatus.Expired

        // 7: due today or within Soon threshold.
        val daysToExpiry = DateUtils.daysBetween(today, packageExpiry) ?: 0
        if (daysToExpiry <= threshold) return ProductDateStatus.Soon

        // 8: otherwise fresh.
        return ProductDateStatus.Fresh
    }

    /** Quantity availability status. Running-low uses optional minimumQuantity. */
    fun quantityStatus(product: FoodProduct): ProductQuantityStatus {
        val qty = product.quantity
        if (qty <= 0.0) return ProductQuantityStatus.Empty
        val minimum = product.minimumQuantity
        if (minimum != null && minimum >= 0.0 && qty <= minimum) return ProductQuantityStatus.RunningLow
        return ProductQuantityStatus.Available
    }

    fun isRunningLow(product: FoodProduct): Boolean =
        product.lifecycleState == ProductLifecycleState.Active &&
            quantityStatus(product) == ProductQuantityStatus.RunningLow

    /** Days until the effective review date (negative = overdue). Null when no relevant date. */
    fun daysUntilReview(product: FoodProduct, today: LocalDate = DateUtils.today()): Long? =
        DateUtils.daysBetween(today, effectiveReviewDate(product))

    /** Human sentence describing the relevant date, in neutral wording. */
    fun reviewSummary(
        product: FoodProduct,
        soonThresholdDays: Int = DEFAULT_SOON_THRESHOLD_DAYS,
        today: LocalDate = DateUtils.today(),
    ): String {
        val status = dateStatus(product, soonThresholdDays, today)
        return when (status) {
            ProductDateStatus.NoExpiryDate -> "No expiry date"
            ProductDateStatus.Used -> "Recorded as used"
            ProductDateStatus.Discarded -> "Recorded as discarded"
            else -> {
                val days = daysUntilReview(product, today)
                when {
                    days == null -> "No expiry date"
                    days < 0L -> "Expiry date passed ${-days}d ago"
                    days == 0L -> "Expiry date is today"
                    days == 1L -> "1 day left"
                    else -> "$days days left"
                }
            }
        }
    }
}
