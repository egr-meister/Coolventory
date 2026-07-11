package com.coolventory.app

import com.coolventory.app.model.FoodCategory
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.model.QuantityUnit
import com.coolventory.app.model.StorageLocation

/** Shared test fixtures. TODAY is fixed so date-based tests are deterministic. */
object TestData {
    const val TODAY = "2026-07-10"

    fun product(
        id: String = "p1",
        name: String = "Milk",
        location: StorageLocation = StorageLocation.Fridge,
        category: FoodCategory = FoodCategory.Dairy,
        shelfId: String = "",
        quantity: Double = 1.0,
        unit: QuantityUnit = QuantityUnit.Pieces,
        addedDate: String = "2026-07-01",
        expiryDate: String = "",
        openedDate: String = "",
        consumeWithin: Int? = null,
        minimumQuantity: Double? = null,
        lifecycle: ProductLifecycleState = ProductLifecycleState.Active,
        buyAgain: Boolean = false,
    ): FoodProduct = FoodProduct(
        id = id,
        name = name,
        storageLocation = location,
        category = category,
        shelfId = shelfId,
        quantity = quantity,
        quantityUnit = unit,
        addedDate = addedDate,
        expiryDate = expiryDate,
        openedDate = openedDate,
        consumeWithinDaysAfterOpening = consumeWithin,
        minimumQuantity = minimumQuantity,
        lifecycleState = lifecycle,
        buyAgain = buyAgain,
        createdAt = "2026-07-01T09:00:00",
        updatedAt = "2026-07-01T09:00:00",
    )
}
