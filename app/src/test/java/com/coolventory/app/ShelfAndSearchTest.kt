package com.coolventory.app

import com.coolventory.app.model.StorageLocation
import com.coolventory.app.util.DefaultShelves
import com.coolventory.app.util.ProductQuery
import com.coolventory.app.util.ProductSort
import com.coolventory.app.util.SearchUtils
import com.coolventory.app.util.ShelfUtils
import com.coolventory.app.util.StatusFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfAndSearchTest {

    private val shelves = DefaultShelves.build("2026-07-01T00:00:00")

    @Test
    fun defaultShelvesBuiltForAllLocations() {
        assertTrue(shelves.any { it.storageLocation == StorageLocation.Fridge })
        assertTrue(shelves.any { it.storageLocation == StorageLocation.Freezer })
        assertTrue(shelves.any { it.storageLocation == StorageLocation.Pantry })
        // Deterministic ids => no duplicates.
        assertEquals(shelves.map { it.id }.toSet().size, shelves.size)
    }

    @Test
    fun shelfGroupingPlacesProductsAndHandlesMissingShelf() {
        val topId = DefaultShelves.defaultId(StorageLocation.Fridge, "top")
        val onTop = TestData.product(id = "a", shelfId = topId)
        val orphan = TestData.product(id = "b", shelfId = "deleted-shelf")
        val grouped = ShelfUtils.groupByShelf(listOf(onTop, orphan), shelves, StorageLocation.Fridge)
        assertTrue(grouped[topId]!!.any { it.id == "a" })
        assertTrue(grouped[ShelfUtils.UNASSIGNED_SHELF_ID]!!.any { it.id == "b" })
    }

    @Test
    fun missingShelfNameFallback() {
        assertEquals("Shelf unavailable", ShelfUtils.shelfName(shelves, "does-not-exist"))
        assertEquals("Unassigned", ShelfUtils.shelfName(shelves, ""))
    }

    @Test
    fun storageFilterWorks() {
        val fridge = TestData.product(id = "f", location = StorageLocation.Fridge)
        val pantry = TestData.product(id = "p", location = StorageLocation.Pantry)
        val q = ProductQuery(locations = setOf(StorageLocation.Pantry))
        val result = SearchUtils.query(listOf(fridge, pantry), shelves, q, 3)
        assertEquals(1, result.size)
        assertEquals("p", result.first().id)
    }

    @Test
    fun expiredFilterAndSorting() {
        val expired = TestData.product(id = "e", expiryDate = "2026-07-01")
        val fresh = TestData.product(id = "fr", expiryDate = "2026-07-30")
        val q = ProductQuery(statuses = setOf(StatusFilter.Expired))
        val onlyExpired = SearchUtils.query(listOf(expired, fresh), shelves, q, 3)
        assertEquals(listOf("e"), onlyExpired.map { it.id })

        // Nearest expiry: expired (2026-07-01) sorts before fresh (2026-07-30).
        val sorted = SearchUtils.sort(listOf(fresh, expired), shelves, ProductSort.ExpiryNearest, 3)
        assertEquals("e", sorted.first().id)
    }

    @Test
    fun searchByNameAndNote() {
        val a = TestData.product(id = "a", name = "Cheddar")
        val b = TestData.product(id = "b", name = "Yogurt")
        val q = ProductQuery(text = "ched")
        val result = SearchUtils.query(listOf(a, b), shelves, q, 3)
        assertEquals(1, result.size)
        assertEquals("a", result.first().id)
    }

    @Test
    fun runningLowFilter() {
        val low = TestData.product(id = "low", quantity = 1.0, minimumQuantity = 2.0)
        val ok = TestData.product(id = "ok", quantity = 5.0, minimumQuantity = 2.0)
        val q = ProductQuery(statuses = setOf(StatusFilter.RunningLow))
        val result = SearchUtils.query(listOf(low, ok), shelves, q, 3)
        assertTrue(result.any { it.id == "low" })
        assertFalse(result.any { it.id == "ok" })
    }
}
