package com.coolventory.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.coolventory.app.model.AppData
import com.coolventory.app.model.AppSettings
import com.coolventory.app.model.BuyAgainItem
import com.coolventory.app.model.DiscardReason
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductHistoryEvent
import com.coolventory.app.model.ProductHistoryEventType
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.model.QuantityUnit
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.model.StorageShelf
import com.coolventory.app.util.DateUtils
import com.coolventory.app.util.DefaultShelves
import com.coolventory.app.util.IdUtils
import com.coolventory.app.util.QuantityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import java.io.IOException

/** DataStore instance (single file per app). */
val Context.coolventoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "coolventory")

/**
 * Single local repository. Persists all app data as serialized JSON strings inside DataStore
 * Preferences. Every read is fail-soft: corrupted or partially-malformed JSON degrades to recovered
 * items or safe defaults, and never crashes the UI.
 */
class CoolventoryRepository(
    private val dataStore: DataStore<Preferences>,
) {

    private val json = CoolventoryJson.instance

    private object Keys {
        val PRODUCTS = stringPreferencesKey("food_products_json")
        val SHELVES = stringPreferencesKey("storage_shelves_json")
        val BUY_AGAIN = stringPreferencesKey("buy_again_json")
        val HISTORY = stringPreferencesKey("product_history_json")
        val SETTINGS = stringPreferencesKey("settings_json")
        val SHELVES_INITIALIZED = stringPreferencesKey("shelves_initialized_flag")
    }

    /* -------------------------------------------------------------------------------------------
     * Flows
     * ----------------------------------------------------------------------------------------- */

    private fun preferencesFlow(): Flow<Preferences> =
        dataStore.data.catch { e ->
            // On IO problems emit empty preferences rather than crashing the collector.
            if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e
        }

    val productsFlow: Flow<List<FoodProduct>> =
        preferencesFlow().map { decodeList(it[Keys.PRODUCTS], FoodProduct.serializer()) }

    val shelvesFlow: Flow<List<StorageShelf>> =
        preferencesFlow().map { decodeList(it[Keys.SHELVES], StorageShelf.serializer()) }

    val buyAgainFlow: Flow<List<BuyAgainItem>> =
        preferencesFlow().map { decodeList(it[Keys.BUY_AGAIN], BuyAgainItem.serializer()) }

    val historyFlow: Flow<List<ProductHistoryEvent>> =
        preferencesFlow().map { decodeList(it[Keys.HISTORY], ProductHistoryEvent.serializer()) }

    val settingsFlow: Flow<AppSettings> =
        preferencesFlow().map { decodeSettings(it[Keys.SETTINGS]) }

    /* -------------------------------------------------------------------------------------------
     * Safe decoding helpers
     * ----------------------------------------------------------------------------------------- */

    private fun <T> decodeList(raw: String?, serializer: KSerializer<T>): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        // First try a whole-list decode (fast path).
        try {
            return json.decodeFromString(ListSerializer(serializer), raw)
        } catch (_: Exception) {
            // Fall through to item-level recovery.
        }
        // Item-level recovery: decode each array element independently, skipping bad ones.
        return try {
            val element = json.parseToJsonElement(raw)
            if (element !is JsonArray) return emptyList()
            element.mapNotNull { item ->
                try {
                    json.decodeFromJsonElement(serializer, item)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodeSettings(raw: String?): AppSettings {
        if (raw.isNullOrBlank()) return AppSettings()
        return try {
            json.decodeFromString(AppSettings.serializer(), raw)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    /* -------------------------------------------------------------------------------------------
     * Low-level writers
     * ----------------------------------------------------------------------------------------- */

    private suspend fun writeProducts(list: List<FoodProduct>) = dataStore.edit {
        it[Keys.PRODUCTS] = json.encodeToString(ListSerializer(FoodProduct.serializer()), list)
    }

    private suspend fun writeShelves(list: List<StorageShelf>) = dataStore.edit {
        it[Keys.SHELVES] = json.encodeToString(ListSerializer(StorageShelf.serializer()), list)
    }

    private suspend fun writeBuyAgain(list: List<BuyAgainItem>) = dataStore.edit {
        it[Keys.BUY_AGAIN] = json.encodeToString(ListSerializer(BuyAgainItem.serializer()), list)
    }

    private suspend fun writeHistory(list: List<ProductHistoryEvent>) = dataStore.edit {
        it[Keys.HISTORY] = json.encodeToString(ListSerializer(ProductHistoryEvent.serializer()), list)
    }

    private suspend fun writeSettings(settings: AppSettings) = dataStore.edit {
        it[Keys.SETTINGS] = json.encodeToString(AppSettings.serializer(), settings)
    }

    private suspend fun currentProducts(): List<FoodProduct> = productsFlow.first()
    private suspend fun currentShelves(): List<StorageShelf> = shelvesFlow.first()
    private suspend fun currentBuyAgain(): List<BuyAgainItem> = buyAgainFlow.first()
    private suspend fun currentHistory(): List<ProductHistoryEvent> = historyFlow.first()
    suspend fun currentSettings(): AppSettings = settingsFlow.first()

    private suspend fun appendHistory(event: ProductHistoryEvent) {
        val list = currentHistory().toMutableList()
        list.add(0, event)
        writeHistory(list)
    }

    private fun historyEvent(
        product: FoodProduct,
        type: ProductHistoryEventType,
        description: String,
        quantityBefore: Double? = null,
        quantityAfter: Double? = null,
    ): ProductHistoryEvent = ProductHistoryEvent(
        id = IdUtils.newId("hist"),
        productId = product.id,
        productName = product.name,
        eventType = type,
        eventDate = DateUtils.todayString(),
        eventTime = DateUtils.nowTime(),
        quantityBefore = quantityBefore,
        quantityAfter = quantityAfter,
        description = description,
        createdAt = DateUtils.nowIso(),
    )

    /* -------------------------------------------------------------------------------------------
     * Initialization
     * ----------------------------------------------------------------------------------------- */

    /** Initialize default shelves exactly once (idempotent across relaunches). */
    suspend fun ensureDefaultShelves() {
        val prefs = preferencesFlow().first()
        val initialized = prefs[Keys.SHELVES_INITIALIZED] == "true"
        val existing = decodeList(prefs[Keys.SHELVES], StorageShelf.serializer())
        if (initialized && existing.isNotEmpty()) return
        if (existing.isEmpty()) {
            writeShelves(DefaultShelves.build(DateUtils.nowIso()))
        }
        dataStore.edit { it[Keys.SHELVES_INITIALIZED] = "true" }
    }

    /* -------------------------------------------------------------------------------------------
     * Product operations
     * ----------------------------------------------------------------------------------------- */

    /** Add a fully-formed product (id/timestamps filled if blank). Returns the stored product. */
    suspend fun addProduct(product: FoodProduct): FoodProduct {
        val now = DateUtils.nowIso()
        val stored = product.copy(
            id = product.id.ifBlank { IdUtils.newId("prod") },
            quantity = QuantityUtils.sanitize(product.quantity),
            createdAt = product.createdAt.ifBlank { now },
            updatedAt = now,
        )
        val list = currentProducts().toMutableList()
        list.add(stored)
        writeProducts(list)
        appendHistory(
            historyEvent(
                stored,
                ProductHistoryEventType.Created,
                "Added to ${stored.storageLocation.label}",
                quantityAfter = stored.quantity,
            ),
        )
        if (stored.buyAgain) {
            addToBuyAgain(stored)
        }
        return stored
    }

    suspend fun updateProduct(updated: FoodProduct) {
        val list = currentProducts().toMutableList()
        val index = list.indexOfFirst { it.id == updated.id }
        if (index < 0) return
        val previous = list[index]
        val stored = updated.copy(
            quantity = QuantityUtils.sanitize(updated.quantity),
            updatedAt = DateUtils.nowIso(),
            createdAt = previous.createdAt.ifBlank { updated.createdAt },
        )
        list[index] = stored
        writeProducts(list)
        appendHistory(
            historyEvent(stored, ProductHistoryEventType.Updated, "Product details edited"),
        )
        if (previous.quantity != stored.quantity) {
            appendHistory(
                historyEvent(
                    stored,
                    ProductHistoryEventType.QuantityChanged,
                    "Quantity changed",
                    quantityBefore = previous.quantity,
                    quantityAfter = stored.quantity,
                ),
            )
        }
    }

    suspend fun deleteProduct(productId: String) {
        val list = currentProducts().toMutableList()
        val removed = list.firstOrNull { it.id == productId } ?: return
        list.removeAll { it.id == productId }
        writeProducts(list)
        // History is preserved; product name already stored on events.
        removed.let { /* no-op, keeps reference explicit */ }
    }

    suspend fun getProduct(productId: String?): FoodProduct? {
        if (productId.isNullOrBlank()) return null
        return currentProducts().firstOrNull { it.id == productId }
    }

    /** Set an absolute quantity (non-negative). Records a QuantityChanged event when it changes. */
    suspend fun setQuantity(productId: String, newQuantity: Double) {
        val list = currentProducts().toMutableList()
        val index = list.indexOfFirst { it.id == productId }
        if (index < 0) return
        val previous = list[index]
        val sanitized = QuantityUtils.sanitize(newQuantity)
        if (sanitized == previous.quantity) return
        val stored = previous.copy(quantity = sanitized, updatedAt = DateUtils.nowIso())
        list[index] = stored
        writeProducts(list)
        appendHistory(
            historyEvent(
                stored,
                ProductHistoryEventType.QuantityChanged,
                "Quantity set to ${QuantityUtils.format(sanitized, stored.quantityUnit)} ${stored.unitDisplay()}",
                quantityBefore = previous.quantity,
                quantityAfter = sanitized,
            ),
        )
    }

    suspend fun adjustQuantity(productId: String, delta: Double) {
        val product = getProduct(productId) ?: return
        setQuantity(productId, product.quantity + delta)
    }

    suspend fun moveProduct(productId: String, location: StorageLocation, shelfId: String) {
        val list = currentProducts().toMutableList()
        val index = list.indexOfFirst { it.id == productId }
        if (index < 0) return
        val previous = list[index]
        val stored = previous.copy(
            storageLocation = location,
            shelfId = shelfId,
            updatedAt = DateUtils.nowIso(),
        )
        list[index] = stored
        writeProducts(list)
        appendHistory(
            historyEvent(
                stored,
                ProductHistoryEventType.Moved,
                "Moved from ${previous.storageLocation.label} to ${location.label}",
            ),
        )
    }

    suspend fun markUsed(
        productId: String,
        usedDate: String,
        finalQuantity: Double?,
        note: String,
        keepBuyAgain: Boolean,
    ) {
        val list = currentProducts().toMutableList()
        val index = list.indexOfFirst { it.id == productId }
        if (index < 0) return
        val previous = list[index]
        val stored = previous.copy(
            lifecycleState = ProductLifecycleState.Used,
            quantity = finalQuantity?.let { QuantityUtils.sanitize(it) } ?: previous.quantity,
            buyAgain = if (keepBuyAgain) previous.buyAgain else false,
            updatedAt = DateUtils.nowIso(),
        )
        list[index] = stored
        writeProducts(list)
        val desc = buildString {
            append("Recorded as used")
            if (note.isNotBlank()) append(" — $note")
        }
        appendHistory(
            historyEvent(stored, ProductHistoryEventType.MarkedUsed, desc)
                .copy(eventDate = DateUtils.parseOrNullFallback(usedDate)),
        )
        if (keepBuyAgain && !stored.buyAgain) addToBuyAgain(stored)
    }

    suspend fun markDiscarded(
        productId: String,
        discardedDate: String,
        reason: DiscardReason,
        note: String,
    ) {
        val list = currentProducts().toMutableList()
        val index = list.indexOfFirst { it.id == productId }
        if (index < 0) return
        val previous = list[index]
        val stored = previous.copy(
            lifecycleState = ProductLifecycleState.Discarded,
            updatedAt = DateUtils.nowIso(),
        )
        list[index] = stored
        writeProducts(list)
        val desc = buildString {
            append("Discarded manually — ${reason.label}")
            if (note.isNotBlank()) append(" — $note")
        }
        appendHistory(
            historyEvent(stored, ProductHistoryEventType.MarkedDiscarded, desc)
                .copy(eventDate = DateUtils.parseOrNullFallback(discardedDate)),
        )
    }

    suspend fun restoreProduct(
        productId: String,
        location: StorageLocation,
        shelfId: String,
        quantity: Double,
    ) {
        val list = currentProducts().toMutableList()
        val index = list.indexOfFirst { it.id == productId }
        if (index < 0) return
        val previous = list[index]
        val stored = previous.copy(
            lifecycleState = ProductLifecycleState.Active,
            storageLocation = location,
            shelfId = shelfId,
            quantity = QuantityUtils.sanitize(quantity),
            updatedAt = DateUtils.nowIso(),
        )
        list[index] = stored
        writeProducts(list)
        appendHistory(
            historyEvent(
                stored,
                ProductHistoryEventType.Restored,
                "Restored to ${location.label}",
                quantityAfter = stored.quantity,
            ),
        )
    }

    /* -------------------------------------------------------------------------------------------
     * Buy Again operations
     * ----------------------------------------------------------------------------------------- */

    suspend fun setBuyAgainFlag(productId: String, enabled: Boolean) {
        val list = currentProducts().toMutableList()
        val index = list.indexOfFirst { it.id == productId }
        if (index < 0) return
        val previous = list[index]
        if (previous.buyAgain == enabled) {
            if (enabled) addToBuyAgain(previous) else removeBuyAgainForProduct(productId)
            return
        }
        val stored = previous.copy(buyAgain = enabled, updatedAt = DateUtils.nowIso())
        list[index] = stored
        writeProducts(list)
        if (enabled) {
            addToBuyAgain(stored)
            appendHistory(historyEvent(stored, ProductHistoryEventType.AddedToBuyAgain, "Added to Buy Again"))
        } else {
            removeBuyAgainForProduct(productId)
            appendHistory(historyEvent(stored, ProductHistoryEventType.RemovedFromBuyAgain, "Removed from Buy Again"))
        }
    }

    private suspend fun addToBuyAgain(product: FoodProduct) {
        val list = currentBuyAgain().toMutableList()
        if (list.any { it.sourceProductId == product.id }) return
        val now = DateUtils.nowIso()
        list.add(
            BuyAgainItem(
                id = IdUtils.newId("buy"),
                sourceProductId = product.id,
                productName = product.name,
                category = product.category,
                preferredQuantity = product.quantity.takeIf { it > 0 },
                preferredUnit = product.quantityUnit,
                customQuantityUnit = product.customQuantityUnit,
                checked = false,
                note = "",
                createdAt = now,
                updatedAt = now,
            ),
        )
        writeBuyAgain(list)
    }

    suspend fun addCustomBuyAgain(item: BuyAgainItem): BuyAgainItem {
        val now = DateUtils.nowIso()
        val stored = item.copy(
            id = item.id.ifBlank { IdUtils.newId("buy") },
            createdAt = item.createdAt.ifBlank { now },
            updatedAt = now,
        )
        val list = currentBuyAgain().toMutableList()
        list.add(stored)
        writeBuyAgain(list)
        return stored
    }

    suspend fun updateBuyAgain(item: BuyAgainItem) {
        val list = currentBuyAgain().toMutableList()
        val index = list.indexOfFirst { it.id == item.id }
        if (index < 0) return
        list[index] = item.copy(updatedAt = DateUtils.nowIso())
        writeBuyAgain(list)
    }

    suspend fun setBuyAgainChecked(itemId: String, checked: Boolean) {
        val list = currentBuyAgain().toMutableList()
        val index = list.indexOfFirst { it.id == itemId }
        if (index < 0) return
        list[index] = list[index].copy(checked = checked, updatedAt = DateUtils.nowIso())
        writeBuyAgain(list)
    }

    suspend fun deleteBuyAgain(itemId: String) {
        val list = currentBuyAgain().toMutableList()
        list.removeAll { it.id == itemId }
        writeBuyAgain(list)
    }

    private suspend fun removeBuyAgainForProduct(productId: String) {
        val list = currentBuyAgain().toMutableList()
        val changed = list.removeAll { it.sourceProductId == productId }
        if (changed) writeBuyAgain(list)
    }

    suspend fun clearCheckedBuyAgain() {
        val list = currentBuyAgain().filterNot { it.checked }
        writeBuyAgain(list)
    }

    /* -------------------------------------------------------------------------------------------
     * Shelf management
     * ----------------------------------------------------------------------------------------- */

    suspend fun addCustomShelf(location: StorageLocation, name: String) {
        val trimmed = name.trim().ifBlank { "Custom Shelf" }
        val list = currentShelves().toMutableList()
        val maxOrder = list.filter { it.storageLocation == location }.maxOfOrNull { it.sortOrder } ?: -1
        val now = DateUtils.nowIso()
        list.add(
            StorageShelf(
                id = IdUtils.newId("shelf"),
                storageLocation = location,
                name = trimmed,
                sortOrder = maxOrder + 1,
                shelfType = com.coolventory.app.model.ShelfType.Custom,
                enabled = true,
                isDefault = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        writeShelves(list)
    }

    suspend fun renameShelf(shelfId: String, newName: String) {
        val list = currentShelves().toMutableList()
        val index = list.indexOfFirst { it.id == shelfId }
        if (index < 0) return
        list[index] = list[index].copy(name = newName.trim().ifBlank { list[index].name }, updatedAt = DateUtils.nowIso())
        writeShelves(list)
    }

    /** Move a shelf up or down within its location's ordering. */
    suspend fun reorderShelf(shelfId: String, moveUp: Boolean) {
        val all = currentShelves().toMutableList()
        val target = all.firstOrNull { it.id == shelfId } ?: return
        val siblings = all.filter { it.storageLocation == target.storageLocation }
            .sortedBy { it.sortOrder }
            .toMutableList()
        val index = siblings.indexOfFirst { it.id == shelfId }
        val swapWith = if (moveUp) index - 1 else index + 1
        if (index < 0 || swapWith < 0 || swapWith >= siblings.size) return
        val a = siblings[index]
        val b = siblings[swapWith]
        siblings[index] = b
        siblings[swapWith] = a
        // Reassign sequential sort orders.
        siblings.forEachIndexed { i, shelf ->
            val pos = all.indexOfFirst { it.id == shelf.id }
            if (pos >= 0) all[pos] = shelf.copy(sortOrder = i, updatedAt = DateUtils.nowIso())
        }
        writeShelves(all)
    }

    /** Hide (disable) a custom shelf. Default shelves cannot be hidden this way. */
    suspend fun setShelfEnabled(shelfId: String, enabled: Boolean) {
        val list = currentShelves().toMutableList()
        val index = list.indexOfFirst { it.id == shelfId }
        if (index < 0) return
        val shelf = list[index]
        if (shelf.isDefault && !enabled) return // never hide defaults
        list[index] = shelf.copy(enabled = enabled, updatedAt = DateUtils.nowIso())
        writeShelves(list)
    }

    /** Restore default shelves, keeping any custom shelves and re-enabling defaults. */
    suspend fun restoreDefaultShelves() {
        val now = DateUtils.nowIso()
        val defaults = DefaultShelves.build(now)
        val existing = currentShelves()
        val customs = existing.filter { !it.isDefault }
        // Reassign custom sort orders after defaults for each location.
        val merged = mutableListOf<StorageShelf>()
        merged.addAll(defaults)
        StorageLocation.entries.forEach { location ->
            val defaultCount = defaults.count { it.storageLocation == location }
            customs.filter { it.storageLocation == location }
                .sortedBy { it.sortOrder }
                .forEachIndexed { i, shelf ->
                    merged.add(shelf.copy(sortOrder = defaultCount + i, enabled = true, updatedAt = now))
                }
        }
        writeShelves(merged)
        dataStore.edit { it[Keys.SHELVES_INITIALIZED] = "true" }
    }

    /* -------------------------------------------------------------------------------------------
     * Settings
     * ----------------------------------------------------------------------------------------- */

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = currentSettings()
        writeSettings(transform(current))
    }

    suspend fun completeOnboarding() = updateSettings { it.copy(onboardingCompleted = true) }

    suspend fun showOnboardingAgain() = updateSettings { it.copy(onboardingCompleted = false) }

    /* -------------------------------------------------------------------------------------------
     * Destructive operations
     * ----------------------------------------------------------------------------------------- */

    suspend fun deleteUsedHistory() {
        val kept = currentHistory().filterNot { it.eventType == ProductHistoryEventType.MarkedUsed }
        writeHistory(kept)
    }

    suspend fun deleteDiscardedHistory() {
        val kept = currentHistory().filterNot { it.eventType == ProductHistoryEventType.MarkedDiscarded }
        writeHistory(kept)
    }

    suspend fun deleteAllActiveProducts() {
        val kept = currentProducts().filterNot { it.lifecycleState == ProductLifecycleState.Active }
        writeProducts(kept)
    }

    /** Full reset: clears every stored collection and settings, then re-seeds default shelves. */
    suspend fun resetAllData() {
        dataStore.edit { it.clear() }
        writeProducts(emptyList())
        writeBuyAgain(emptyList())
        writeHistory(emptyList())
        writeSettings(AppSettings())
        writeShelves(DefaultShelves.build(DateUtils.nowIso()))
        dataStore.edit { it[Keys.SHELVES_INITIALIZED] = "true" }
    }

    /** Convenience snapshot for tests / one-shot reads. */
    suspend fun snapshot(): AppData = AppData(
        products = currentProducts(),
        shelves = currentShelves(),
        buyAgainItems = currentBuyAgain(),
        historyEvents = currentHistory(),
        settings = currentSettings(),
    )
}

/** Small helper: return a valid date string or today's date as a fallback. */
private fun DateUtils.parseOrNullFallback(value: String): String =
    if (isValidDate(value)) value else todayString()
