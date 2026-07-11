package com.coolventory.app.util

import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.model.StorageShelf

/**
 * Grouping helpers for the shelf dashboard. All functions tolerate missing/deleted shelves and
 * empty inputs without throwing.
 */
object ShelfUtils {

    const val UNASSIGNED_SHELF_ID = "__unassigned__"

    /** Enabled shelves for a location, ordered by sortOrder then name. */
    fun shelvesFor(shelves: List<StorageShelf>, location: StorageLocation): List<StorageShelf> =
        shelves
            .filter { it.storageLocation == location && it.enabled }
            .sortedWith(compareBy({ it.sortOrder }, { it.name.lowercase() }))

    /** All shelves for a location (including disabled), ordered — used by shelf management. */
    fun allShelvesFor(shelves: List<StorageShelf>, location: StorageLocation): List<StorageShelf> =
        shelves
            .filter { it.storageLocation == location }
            .sortedWith(compareBy({ it.sortOrder }, { it.name.lowercase() }))

    /** Active products at a location. */
    fun activeProductsFor(products: List<FoodProduct>, location: StorageLocation): List<FoodProduct> =
        products.filter {
            it.storageLocation == location && it.lifecycleState == ProductLifecycleState.Active
        }

    /**
     * Group active products for a location by their shelf. Products whose shelf is missing/deleted
     * are collected under a synthetic "unassigned" bucket keyed by [UNASSIGNED_SHELF_ID].
     * Returns an ordered map preserving the enabled-shelf order, with unassigned last if present.
     */
    fun groupByShelf(
        products: List<FoodProduct>,
        shelves: List<StorageShelf>,
        location: StorageLocation,
    ): LinkedHashMap<String, List<FoodProduct>> {
        val orderedShelves = shelvesFor(shelves, location)
        val validIds = orderedShelves.map { it.id }.toSet()
        val active = activeProductsFor(products, location)

        val result = LinkedHashMap<String, List<FoodProduct>>()
        orderedShelves.forEach { shelf ->
            result[shelf.id] = active.filter { it.shelfId == shelf.id }
        }
        val unassigned = active.filter { it.shelfId.isBlank() || it.shelfId !in validIds }
        if (unassigned.isNotEmpty()) {
            result[UNASSIGNED_SHELF_ID] = unassigned
        }
        return result
    }

    fun shelfName(shelves: List<StorageShelf>, shelfId: String?): String {
        if (shelfId.isNullOrBlank()) return "Unassigned"
        if (shelfId == UNASSIGNED_SHELF_ID) return "Unassigned"
        return shelves.firstOrNull { it.id == shelfId }?.name ?: "Shelf unavailable"
    }

    fun findShelf(shelves: List<StorageShelf>, shelfId: String?): StorageShelf? =
        if (shelfId.isNullOrBlank()) null else shelves.firstOrNull { it.id == shelfId }
}
