package com.coolventory.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.coolventory.app.CoolventoryApplication
import com.coolventory.app.data.CoolventoryRepository
import com.coolventory.app.model.AppSettings
import com.coolventory.app.model.BuyAgainItem
import com.coolventory.app.model.DiscardReason
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductHistoryEvent
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.model.StorageShelf
import com.coolventory.app.util.ReminderSnapshot
import com.coolventory.app.util.ReminderUtils
import com.coolventory.app.util.SearchUtils.query
import com.coolventory.app.util.ProductQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Aggregated, immutable UI state shared across screens. */
data class CoolventoryUiState(
    val loading: Boolean = true,
    val products: List<FoodProduct> = emptyList(),
    val shelves: List<StorageShelf> = emptyList(),
    val buyAgain: List<BuyAgainItem> = emptyList(),
    val history: List<ProductHistoryEvent> = emptyList(),
    val settings: AppSettings = AppSettings(),
) {
    fun product(id: String?): FoodProduct? =
        if (id.isNullOrBlank()) null else products.firstOrNull { it.id == id }

    fun buyAgainItem(id: String?): BuyAgainItem? =
        if (id.isNullOrBlank()) null else buyAgain.firstOrNull { it.id == id }

    fun historyEvent(id: String?): ProductHistoryEvent? =
        if (id.isNullOrBlank()) null else history.firstOrNull { it.id == id }
}

/**
 * The single app-scoped ViewModel. Observes repository flows and exposes action methods. Uses
 * StateFlow for observable state, immutable [CoolventoryUiState], and coroutines for writes.
 */
class MainViewModel(
    private val repository: CoolventoryRepository,
) : ViewModel() {

    val uiState: StateFlow<CoolventoryUiState> = combine(
        repository.productsFlow,
        repository.shelvesFlow,
        repository.buyAgainFlow,
        repository.historyFlow,
        repository.settingsFlow,
    ) { products, shelves, buyAgain, history, settings ->
        CoolventoryUiState(
            loading = false,
            products = products,
            shelves = shelves,
            buyAgain = buyAgain,
            history = history,
            settings = settings,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoolventoryUiState(),
    )

    // Transient UI-only state.
    private val _searchQuery = MutableStateFlow(ProductQuery())
    val searchQuery: StateFlow<ProductQuery> = _searchQuery.asStateFlow()

    private val _remindersDismissedThisSession = MutableStateFlow(false)
    val remindersDismissedThisSession: StateFlow<Boolean> = _remindersDismissedThisSession.asStateFlow()

    init {
        viewModelScope.launch { repository.ensureDefaultShelves() }
    }

    /* --------------------------- Reminders (in-app, evaluated on demand) --------------------- */

    fun currentReminders(): ReminderSnapshot {
        val s = uiState.value
        return ReminderUtils.evaluate(s.products, s.buyAgain, s.settings)
    }

    fun dismissRemindersForSession() {
        _remindersDismissedThisSession.value = true
    }

    fun resetReminderDismissal() {
        _remindersDismissedThisSession.value = false
    }

    /* --------------------------- Search --------------------------- */

    fun updateSearchQuery(transform: (ProductQuery) -> ProductQuery) {
        _searchQuery.value = transform(_searchQuery.value)
    }

    fun searchResults(): List<FoodProduct> {
        val s = uiState.value
        return query(s.products, s.shelves, _searchQuery.value, s.settings.soonThresholdDays)
    }

    /* --------------------------- Product actions --------------------------- */

    fun addProduct(product: FoodProduct, onDone: (FoodProduct) -> Unit = {}) = launch {
        val stored = repository.addProduct(product)
        onDone(stored)
    }

    fun updateProduct(product: FoodProduct) = launch { repository.updateProduct(product) }
    fun deleteProduct(id: String) = launch { repository.deleteProduct(id) }
    fun setQuantity(id: String, qty: Double) = launch { repository.setQuantity(id, qty) }
    fun adjustQuantity(id: String, delta: Double) = launch { repository.adjustQuantity(id, delta) }
    fun moveProduct(id: String, location: StorageLocation, shelfId: String) =
        launch { repository.moveProduct(id, location, shelfId) }

    fun markUsed(id: String, usedDate: String, finalQty: Double?, note: String, keepBuyAgain: Boolean) =
        launch { repository.markUsed(id, usedDate, finalQty, note, keepBuyAgain) }

    fun markDiscarded(id: String, date: String, reason: DiscardReason, note: String) =
        launch { repository.markDiscarded(id, date, reason, note) }

    fun restoreProduct(id: String, location: StorageLocation, shelfId: String, qty: Double) =
        launch { repository.restoreProduct(id, location, shelfId, qty) }

    fun setBuyAgain(id: String, enabled: Boolean) = launch { repository.setBuyAgainFlag(id, enabled) }

    /* --------------------------- Buy Again list --------------------------- */

    fun addCustomBuyAgain(item: BuyAgainItem) = launch { repository.addCustomBuyAgain(item) }
    fun updateBuyAgain(item: BuyAgainItem) = launch { repository.updateBuyAgain(item) }
    fun setBuyAgainChecked(id: String, checked: Boolean) = launch { repository.setBuyAgainChecked(id, checked) }
    fun deleteBuyAgain(id: String) = launch { repository.deleteBuyAgain(id) }
    fun clearCheckedBuyAgain() = launch { repository.clearCheckedBuyAgain() }

    /* --------------------------- Shelves --------------------------- */

    fun addCustomShelf(location: StorageLocation, name: String) = launch { repository.addCustomShelf(location, name) }
    fun renameShelf(id: String, name: String) = launch { repository.renameShelf(id, name) }
    fun reorderShelf(id: String, moveUp: Boolean) = launch { repository.reorderShelf(id, moveUp) }
    fun setShelfEnabled(id: String, enabled: Boolean) = launch { repository.setShelfEnabled(id, enabled) }
    fun restoreDefaultShelves() = launch { repository.restoreDefaultShelves() }

    /* --------------------------- Settings --------------------------- */

    fun updateSettings(transform: (AppSettings) -> AppSettings) = launch { repository.updateSettings(transform) }
    fun completeOnboarding() = launch { repository.completeOnboarding() }
    fun showOnboardingAgain() = launch { repository.showOnboardingAgain() }

    /* --------------------------- Destructive --------------------------- */

    fun deleteUsedHistory() = launch { repository.deleteUsedHistory() }
    fun deleteDiscardedHistory() = launch { repository.deleteDiscardedHistory() }
    fun deleteAllActiveProducts() = launch { repository.deleteAllActiveProducts() }
    fun resetAllData() = launch { repository.resetAllData() }

    private inline fun launch(crossinline block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CoolventoryApplication
                MainViewModel(app.repository)
            }
        }
    }
}
