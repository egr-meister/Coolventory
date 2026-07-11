package com.coolventory.app.util

import com.coolventory.app.model.FoodCategory
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductHistoryEvent
import com.coolventory.app.model.ProductHistoryEventType
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.model.StorageLocation
import java.time.LocalDate

/** Neutral, local-only statistics. No health/waste scoring of any kind. */
data class Statistics(
    val activeProducts: Int = 0,
    val fridgeCount: Int = 0,
    val freezerCount: Int = 0,
    val pantryCount: Int = 0,
    val usedThisMonth: Int = 0,
    val discardedThisMonth: Int = 0,
    val buyAgainCount: Int = 0,
    val mostUsedCategory: String = "—",
    val averageActiveDurationDays: Long? = null,
    val withoutExpiryDate: Int = 0,
)

object StatsUtils {

    fun compute(
        products: List<FoodProduct>,
        historyEvents: List<ProductHistoryEvent>,
        buyAgainCount: Int,
        today: LocalDate = DateUtils.today(),
    ): Statistics {
        val active = products.filter { it.lifecycleState == ProductLifecycleState.Active }
        val monthKey = DateUtils.monthKey(DateUtils.format(today))

        val usedThisMonth = historyEvents.count {
            it.eventType == ProductHistoryEventType.MarkedUsed &&
                DateUtils.monthKey(it.eventDate) == monthKey
        }
        val discardedThisMonth = historyEvents.count {
            it.eventType == ProductHistoryEventType.MarkedDiscarded &&
                DateUtils.monthKey(it.eventDate) == monthKey
        }

        val mostUsedCategory = historyEvents
            .filter { it.eventType == ProductHistoryEventType.MarkedUsed }
            .mapNotNull { event -> products.firstOrNull { it.id == event.productId }?.category }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.label
            ?: mostUsedCategoryFromActive(active)

        val durations = products.mapNotNull { product ->
            val added = DateUtils.parseOrNull(product.addedDate) ?: return@mapNotNull null
            val end = when (product.lifecycleState) {
                ProductLifecycleState.Active -> today
                else -> DateUtils.parseOrNull(product.updatedAt.take(10)) ?: today
            }
            val days = DateUtils.daysBetween(added, end) ?: return@mapNotNull null
            if (days < 0) null else days
        }
        val averageDuration = if (durations.isEmpty()) null else durations.sum() / durations.size

        return Statistics(
            activeProducts = active.size,
            fridgeCount = active.count { it.storageLocation == StorageLocation.Fridge },
            freezerCount = active.count { it.storageLocation == StorageLocation.Freezer },
            pantryCount = active.count { it.storageLocation == StorageLocation.Pantry },
            usedThisMonth = usedThisMonth,
            discardedThisMonth = discardedThisMonth,
            buyAgainCount = buyAgainCount,
            mostUsedCategory = mostUsedCategory,
            averageActiveDurationDays = averageDuration,
            withoutExpiryDate = active.count { DateUtils.parseOrNull(it.expiryDate) == null },
        )
    }

    private fun mostUsedCategoryFromActive(active: List<FoodProduct>): String =
        active.groupingBy { it.category }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.label
            ?: "—"

    /** Count of a category across a product list — used by simple category bars. */
    fun categoryCounts(products: List<FoodProduct>): Map<FoodCategory, Int> =
        products.groupingBy { it.category }.eachCount()
}
