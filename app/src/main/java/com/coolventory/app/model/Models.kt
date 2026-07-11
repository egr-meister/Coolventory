package com.coolventory.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------------------------------
 * Enumerations
 *
 * Every enum carries a stable serialized name plus a human-readable [label] for the UI so that
 * on-disk names never change even if display text is tweaked.
 * ---------------------------------------------------------------------------------------------- */

@Serializable
enum class StorageLocation(val label: String) {
    Fridge("Fridge"),
    Freezer("Freezer"),
    Pantry("Pantry");

    companion object {
        fun fromNameOrDefault(name: String?, default: StorageLocation = Fridge): StorageLocation =
            entries.firstOrNull { it.name == name } ?: default
    }
}

@Serializable
enum class FoodCategory(val label: String) {
    Dairy("Dairy"),
    Meat("Meat"),
    Fish("Fish"),
    Vegetables("Vegetables"),
    Fruit("Fruit"),
    Drinks("Drinks"),
    Condiments("Condiments"),
    Leftovers("Leftovers"),
    FrozenFood("Frozen Food"),
    Grains("Grains"),
    CannedFood("Canned Food"),
    Snacks("Snacks"),
    Baking("Baking"),
    Other("Other");

    companion object {
        fun fromNameOrDefault(name: String?, default: FoodCategory = Other): FoodCategory =
            entries.firstOrNull { it.name == name } ?: default
    }
}

@Serializable
enum class QuantityUnit(val label: String, val defaultStep: Double, val maxDecimals: Int) {
    Pieces("Pieces", 1.0, 1),
    Packages("Packages", 1.0, 1),
    Bottles("Bottles", 1.0, 1),
    Cans("Cans", 1.0, 1),
    Jars("Jars", 1.0, 1),
    Grams("Grams", 50.0, 1),
    Kilograms("Kilograms", 0.1, 2),
    Ounces("Ounces", 1.0, 1),
    Pounds("Pounds", 0.1, 2),
    Milliliters("Milliliters", 100.0, 1),
    Liters("Liters", 0.1, 2),
    Custom("Custom", 1.0, 2);

    companion object {
        fun fromNameOrDefault(name: String?, default: QuantityUnit = Pieces): QuantityUnit =
            entries.firstOrNull { it.name == name } ?: default
    }
}

@Serializable
enum class ProductLifecycleState {
    Active,
    Used,
    Discarded;

    companion object {
        fun fromNameOrDefault(name: String?, default: ProductLifecycleState = Active): ProductLifecycleState =
            entries.firstOrNull { it.name == name } ?: default
    }
}

@Serializable
enum class ShelfType {
    StandardShelf,
    Drawer,
    DoorShelf,
    Basket,
    Custom;

    companion object {
        fun fromNameOrDefault(name: String?, default: ShelfType = StandardShelf): ShelfType =
            entries.firstOrNull { it.name == name } ?: default
    }
}

@Serializable
enum class ProductHistoryEventType(val label: String) {
    Created("Added"),
    Updated("Edited"),
    QuantityChanged("Quantity changed"),
    Moved("Moved"),
    MarkedUsed("Marked used"),
    MarkedDiscarded("Marked discarded"),
    Restored("Restored"),
    AddedToBuyAgain("Added to Buy Again"),
    RemovedFromBuyAgain("Removed from Buy Again");
}

@Serializable
enum class DiscardReason(val label: String) {
    ExpiryDatePassed("Expiry date passed"),
    QualityConcern("Quality concern"),
    NotNeeded("Not needed"),
    StorageCleanup("Storage cleanup"),
    Other("Other");
}

/* ------------------------------------------------------------------------------------------------
 * Derived (calculated) statuses — never persisted, always computed from current values.
 * ---------------------------------------------------------------------------------------------- */

enum class ProductDateStatus(val label: String) {
    Fresh("Fresh"),
    Soon("Review soon"),
    Expired("Expiry date passed"),
    NoExpiryDate("No expiry date"),
    OpenedSoon("Opened — review soon"),
    Used("Used"),
    Discarded("Discarded"),
}

enum class ProductQuantityStatus(val label: String) {
    Available("Available"),
    RunningLow("Running low"),
    Empty("Empty"),
}

/* ------------------------------------------------------------------------------------------------
 * Persisted data models
 *
 * All fields have defaults so that older stored JSON missing newer fields still deserializes
 * (kotlinx.serialization uses defaults for absent keys when isLenient / default-aware decoding).
 * ---------------------------------------------------------------------------------------------- */

@Serializable
data class FoodProduct(
    val id: String = "",
    val name: String = "",
    val storageLocation: StorageLocation = StorageLocation.Fridge,
    val category: FoodCategory = FoodCategory.Other,
    val customCategoryName: String = "",
    val shelfId: String = "",
    val quantity: Double = 0.0,
    val quantityUnit: QuantityUnit = QuantityUnit.Pieces,
    val customQuantityUnit: String = "",
    val addedDate: String = "",
    val expiryDate: String = "",
    val openedDate: String = "",
    val consumeWithinDaysAfterOpening: Int? = null,
    val minimumQuantity: Double? = null,
    val lifecycleState: ProductLifecycleState = ProductLifecycleState.Active,
    val buyAgain: Boolean = false,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
) {
    /** Category label taking a custom "Other" name into account. */
    fun categoryDisplay(): String =
        if (category == FoodCategory.Other && customCategoryName.isNotBlank()) customCategoryName
        else category.label

    /** Unit label taking a custom unit into account. */
    fun unitDisplay(): String =
        if (quantityUnit == QuantityUnit.Custom && customQuantityUnit.isNotBlank()) customQuantityUnit
        else quantityUnit.label
}

@Serializable
data class StorageShelf(
    val id: String = "",
    val storageLocation: StorageLocation = StorageLocation.Fridge,
    val name: String = "",
    val sortOrder: Int = 0,
    val shelfType: ShelfType = ShelfType.StandardShelf,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class BuyAgainItem(
    val id: String = "",
    val sourceProductId: String? = null,
    val productName: String = "",
    val category: FoodCategory = FoodCategory.Other,
    val preferredQuantity: Double? = null,
    val preferredUnit: QuantityUnit = QuantityUnit.Pieces,
    val customQuantityUnit: String = "",
    val checked: Boolean = false,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class ProductHistoryEvent(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val eventType: ProductHistoryEventType = ProductHistoryEventType.Updated,
    val eventDate: String = "",
    val eventTime: String = "",
    val quantityBefore: Double? = null,
    val quantityAfter: Double? = null,
    val description: String = "",
    val createdAt: String = "",
)

@Serializable
data class ReminderSettings(
    val enabled: Boolean = true,
    val showExpired: Boolean = true,
    val showDueToday: Boolean = true,
    val showSoon: Boolean = true,
    val showLowQuantity: Boolean = true,
    val showBuyAgain: Boolean = true,
)

@Serializable
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val defaultStorageLocation: StorageLocation = StorageLocation.Fridge,
    val soonThresholdDays: Int = 3,
    val defaultQuantityUnit: QuantityUnit = QuantityUnit.Pieces,
    val firstDayMonday: Boolean = true,
    val reminderSettings: ReminderSettings = ReminderSettings(),
)

/**
 * Aggregate snapshot of all persisted collections. Stored as separate DataStore keys in practice,
 * but grouped here for repository convenience and tests.
 */
@Serializable
data class AppData(
    val products: List<FoodProduct> = emptyList(),
    val shelves: List<StorageShelf> = emptyList(),
    val buyAgainItems: List<BuyAgainItem> = emptyList(),
    val historyEvents: List<ProductHistoryEvent> = emptyList(),
    val settings: AppSettings = AppSettings(),
)
