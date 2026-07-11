package com.coolventory.app.util

import com.coolventory.app.model.FoodCategory
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.model.StorageShelf

/** Product sort options for search / all-products screens. */
enum class ProductSort(val label: String) {
    ExpiryNearest("Expiry: nearest first"),
    MostOverdue("Most overdue first"),
    RecentlyAdded("Recently added"),
    RecentlyUpdated("Recently updated"),
    NameAsc("Name (A–Z)"),
    QuantityLowToHigh("Quantity: low to high"),
    Location("Storage location"),
    Shelf("Shelf"),
}

/** Status filters selectable in search. */
enum class StatusFilter(val label: String) {
    Fresh("Fresh"),
    Soon("Review soon"),
    Expired("Expiry date passed"),
    NoExpiryDate("No expiry date"),
    RunningLow("Running low"),
    BuyAgain("Buy Again"),
    Used("Used"),
    Discarded("Discarded"),
}

/** Immutable filter/sort criteria for search. */
data class ProductQuery(
    val text: String = "",
    val locations: Set<StorageLocation> = emptySet(),
    val statuses: Set<StatusFilter> = emptySet(),
    val categories: Set<FoodCategory> = emptySet(),
    val sort: ProductSort = ProductSort.ExpiryNearest,
)

object SearchUtils {

    /** Apply full-text search, filters and sorting. Never throws; safe on empty inputs. */
    fun query(
        products: List<FoodProduct>,
        shelves: List<StorageShelf>,
        criteria: ProductQuery,
        soonThresholdDays: Int,
    ): List<FoodProduct> {
        val needle = criteria.text.trim().lowercase()

        val filtered = products.filter { product ->
            matchesText(product, shelves, needle) &&
                matchesLocation(product, criteria.locations) &&
                matchesCategory(product, criteria.categories) &&
                matchesStatus(product, criteria.statuses, soonThresholdDays)
        }
        return sort(filtered, shelves, criteria.sort, soonThresholdDays)
    }

    private fun matchesText(product: FoodProduct, shelves: List<StorageShelf>, needle: String): Boolean {
        if (needle.isEmpty()) return true
        val shelfName = ShelfUtils.shelfName(shelves, product.shelfId).lowercase()
        return product.name.lowercase().contains(needle) ||
            product.categoryDisplay().lowercase().contains(needle) ||
            shelfName.contains(needle) ||
            product.note.lowercase().contains(needle)
    }

    private fun matchesLocation(product: FoodProduct, locations: Set<StorageLocation>): Boolean =
        locations.isEmpty() || product.storageLocation in locations

    private fun matchesCategory(product: FoodProduct, categories: Set<FoodCategory>): Boolean =
        categories.isEmpty() || product.category in categories

    private fun matchesStatus(
        product: FoodProduct,
        statuses: Set<StatusFilter>,
        soonThresholdDays: Int,
    ): Boolean {
        if (statuses.isEmpty()) return true
        val dateStatus = StatusUtils.dateStatus(product, soonThresholdDays)
        return statuses.any { filter ->
            when (filter) {
                StatusFilter.Fresh -> dateStatus == ProductDateStatus.Fresh
                StatusFilter.Soon ->
                    dateStatus == ProductDateStatus.Soon || dateStatus == ProductDateStatus.OpenedSoon
                StatusFilter.Expired -> dateStatus == ProductDateStatus.Expired
                StatusFilter.NoExpiryDate -> dateStatus == ProductDateStatus.NoExpiryDate
                StatusFilter.RunningLow -> StatusUtils.isRunningLow(product)
                StatusFilter.BuyAgain -> product.buyAgain
                StatusFilter.Used -> product.lifecycleState == ProductLifecycleState.Used
                StatusFilter.Discarded -> product.lifecycleState == ProductLifecycleState.Discarded
            }
        }
    }

    fun sort(
        products: List<FoodProduct>,
        shelves: List<StorageShelf>,
        sort: ProductSort,
        soonThresholdDays: Int,
    ): List<FoodProduct> {
        // Large sentinel so items without a relevant date sort after dated ones.
        val farAway = Long.MAX_VALUE / 4
        return when (sort) {
            ProductSort.ExpiryNearest -> products.sortedBy {
                StatusUtils.daysUntilReview(it) ?: farAway
            }
            ProductSort.MostOverdue -> products.sortedBy {
                StatusUtils.daysUntilReview(it) ?: farAway
            } // most overdue = most negative days-until = smallest value first
            ProductSort.RecentlyAdded -> products.sortedByDescending { it.createdAt }
            ProductSort.RecentlyUpdated -> products.sortedByDescending { it.updatedAt }
            ProductSort.NameAsc -> products.sortedBy { it.name.lowercase() }
            ProductSort.QuantityLowToHigh -> products.sortedBy { it.quantity }
            ProductSort.Location -> products.sortedWith(
                compareBy({ it.storageLocation.ordinal }, { it.name.lowercase() }),
            )
            ProductSort.Shelf -> products.sortedWith(
                compareBy({ ShelfUtils.shelfName(shelves, it.shelfId).lowercase() }, { it.name.lowercase() }),
            )
        }
    }
}
