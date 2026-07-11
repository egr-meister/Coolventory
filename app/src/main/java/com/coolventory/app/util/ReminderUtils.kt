package com.coolventory.app.util

import com.coolventory.app.model.AppSettings
import com.coolventory.app.model.BuyAgainItem
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.model.ProductLifecycleState
import java.time.LocalDate

/** Result of evaluating in-app reminders. Purely local; no notifications are ever sent. */
data class ReminderSnapshot(
    val expired: List<FoodProduct> = emptyList(),
    val dueToday: List<FoodProduct> = emptyList(),
    val soon: List<FoodProduct> = emptyList(),
    val openedSoon: List<FoodProduct> = emptyList(),
    val runningLow: List<FoodProduct> = emptyList(),
    val uncheckedBuyAgain: Int = 0,
) {
    val totalProductsNeedingReview: Int
        get() = (expired + dueToday + soon + openedSoon).map { it.id }.toSet().size

    val hasAnything: Boolean
        get() = totalProductsNeedingReview > 0 || runningLow.isNotEmpty() || uncheckedBuyAgain > 0

    /** Neutral headline like "3 products need review." */
    fun headline(): String {
        val review = totalProductsNeedingReview
        return when {
            review == 1 -> "1 product needs review."
            review > 1 -> "$review products need review."
            runningLow.isNotEmpty() -> "${runningLow.size} product(s) running low."
            uncheckedBuyAgain > 0 -> "$uncheckedBuyAgain Buy Again item(s) pending."
            else -> "Nothing needs review right now."
        }
    }

    /** Neutral detail line naming a few products, without any safety language. */
    fun detail(): String {
        val names = (expired + dueToday + soon + openedSoon)
            .distinctBy { it.id }
            .take(3)
            .map { it.name.ifBlank { "Unnamed product" } }
        if (names.isEmpty()) return ""
        return when (names.size) {
            1 -> "${names[0]} should be reviewed."
            2 -> "${names[0]} and ${names[1]} should be reviewed."
            else -> "${names[0]}, ${names[1]} and others should be reviewed."
        }
    }
}

object ReminderUtils {

    fun evaluate(
        products: List<FoodProduct>,
        buyAgainItems: List<BuyAgainItem>,
        settings: AppSettings,
        today: LocalDate = DateUtils.today(),
    ): ReminderSnapshot {
        val r = settings.reminderSettings
        if (!r.enabled) return ReminderSnapshot()

        val active = products.filter { it.lifecycleState == ProductLifecycleState.Active }
        val threshold = settings.soonThresholdDays

        val expired = mutableListOf<FoodProduct>()
        val dueToday = mutableListOf<FoodProduct>()
        val soon = mutableListOf<FoodProduct>()
        val openedSoon = mutableListOf<FoodProduct>()
        val runningLow = mutableListOf<FoodProduct>()

        active.forEach { product ->
            when (StatusUtils.dateStatus(product, threshold, today)) {
                ProductDateStatus.Expired -> if (r.showExpired) expired += product
                ProductDateStatus.Soon -> {
                    val days = StatusUtils.daysUntilReview(product, today)
                    if (days == 0L) {
                        if (r.showDueToday) dueToday += product
                    } else if (r.showSoon) {
                        soon += product
                    }
                }
                ProductDateStatus.OpenedSoon -> {
                    val days = StatusUtils.daysUntilReview(product, today)
                    if (days == 0L) {
                        if (r.showDueToday) dueToday += product
                    } else if (r.showSoon) {
                        openedSoon += product
                    }
                }
                else -> Unit
            }
            if (r.showLowQuantity && StatusUtils.isRunningLow(product)) {
                runningLow += product
            }
        }

        val uncheckedBuyAgain =
            if (r.showBuyAgain) buyAgainItems.count { !it.checked } else 0

        return ReminderSnapshot(
            expired = expired,
            dueToday = dueToday,
            soon = soon,
            openedSoon = openedSoon,
            runningLow = runningLow,
            uncheckedBuyAgain = uncheckedBuyAgain,
        )
    }
}
