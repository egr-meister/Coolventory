package com.coolventory.app.util

import com.coolventory.app.model.ShelfType
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.model.StorageShelf

/**
 * Default shelf layout for the three storage locations. IDs are stable and deterministic so that
 * default shelves are never duplicated across relaunches and product references remain valid.
 */
object DefaultShelves {

    private data class Spec(val key: String, val name: String, val type: ShelfType)

    private val fridge = listOf(
        Spec("top", "Top Shelf", ShelfType.StandardShelf),
        Spec("middle", "Middle Shelf", ShelfType.StandardShelf),
        Spec("bottom", "Bottom Shelf", ShelfType.StandardShelf),
        Spec("produce", "Produce Drawer", ShelfType.Drawer),
        Spec("door", "Door Shelf", ShelfType.DoorShelf),
    )

    private val freezer = listOf(
        Spec("upper", "Upper Drawer", ShelfType.Drawer),
        Spec("middle", "Middle Drawer", ShelfType.Drawer),
        Spec("lower", "Lower Drawer", ShelfType.Drawer),
        Spec("door", "Door Compartment", ShelfType.DoorShelf),
    )

    private val pantry = listOf(
        Spec("upper", "Upper Shelf", ShelfType.StandardShelf),
        Spec("eye", "Eye-Level Shelf", ShelfType.StandardShelf),
        Spec("lower", "Lower Shelf", ShelfType.StandardShelf),
        Spec("basket", "Basket", ShelfType.Basket),
        Spec("cabinet", "Cabinet Door", ShelfType.DoorShelf),
    )

    /** Deterministic default shelf id for a given location + key. */
    fun defaultId(location: StorageLocation, key: String): String =
        "default_${location.name.lowercase()}_$key"

    fun build(nowIso: String): List<StorageShelf> {
        val result = mutableListOf<StorageShelf>()
        fun addAll(location: StorageLocation, specs: List<Spec>) {
            specs.forEachIndexed { index, spec ->
                result += StorageShelf(
                    id = defaultId(location, spec.key),
                    storageLocation = location,
                    name = spec.name,
                    sortOrder = index,
                    shelfType = spec.type,
                    enabled = true,
                    isDefault = true,
                    createdAt = nowIso,
                    updatedAt = nowIso,
                )
            }
        }
        addAll(StorageLocation.Fridge, fridge)
        addAll(StorageLocation.Freezer, freezer)
        addAll(StorageLocation.Pantry, pantry)
        return result
    }
}
