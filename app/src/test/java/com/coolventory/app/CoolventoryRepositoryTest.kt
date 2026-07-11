package com.coolventory.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.coolventory.app.data.CoolventoryRepository
import com.coolventory.app.model.DiscardReason
import com.coolventory.app.model.ProductHistoryEventType
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.model.StorageLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class CoolventoryRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun TestScopeStore(scopeFile: java.io.File, scope: kotlinx.coroutines.CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { scopeFile }

    @Test
    fun defaultShelvesInitializeOnceWithoutDuplication() = runTest {
        val file = java.io.File(tempFolder.root,"t1.preferences_pb")
        val repo = CoolventoryRepository(TestScopeStore(file, backgroundScope))
        repo.ensureDefaultShelves()
        val firstCount = repo.snapshot().shelves.size
        assertTrue(firstCount > 0)
        repo.ensureDefaultShelves()
        assertEquals(firstCount, repo.snapshot().shelves.size)
    }

    @Test
    fun addProductCreatesHistory() = runTest {
        val file = java.io.File(tempFolder.root,"t2.preferences_pb")
        val repo = CoolventoryRepository(TestScopeStore(file, backgroundScope))
        val stored = repo.addProduct(TestData.product(id = "", name = "Milk"))
        val snap = repo.snapshot()
        assertEquals(1, snap.products.size)
        assertTrue(stored.id.isNotBlank())
        assertTrue(snap.historyEvents.any { it.eventType == ProductHistoryEventType.Created })
    }

    @Test
    fun markUsedThenRestore() = runTest {
        val file = java.io.File(tempFolder.root,"t3.preferences_pb")
        val repo = CoolventoryRepository(TestScopeStore(file, backgroundScope))
        val p = repo.addProduct(TestData.product(id = "", name = "Yogurt"))
        repo.markUsed(p.id, TestData.TODAY, finalQuantity = 0.0, note = "", keepBuyAgain = false)
        assertEquals(ProductLifecycleState.Used, repo.getProduct(p.id)!!.lifecycleState)
        assertTrue(repo.snapshot().historyEvents.any { it.eventType == ProductHistoryEventType.MarkedUsed })

        repo.restoreProduct(p.id, StorageLocation.Fridge, "", 2.0)
        val restored = repo.getProduct(p.id)!!
        assertEquals(ProductLifecycleState.Active, restored.lifecycleState)
        assertEquals(2.0, restored.quantity, 0.0001)
        assertTrue(repo.snapshot().historyEvents.any { it.eventType == ProductHistoryEventType.Restored })
    }

    @Test
    fun markDiscardedRecordsReason() = runTest {
        val file = java.io.File(tempFolder.root,"t4.preferences_pb")
        val repo = CoolventoryRepository(TestScopeStore(file, backgroundScope))
        val p = repo.addProduct(TestData.product(id = "", name = "Spinach"))
        repo.markDiscarded(p.id, TestData.TODAY, DiscardReason.NotNeeded, "cleanup")
        assertEquals(ProductLifecycleState.Discarded, repo.getProduct(p.id)!!.lifecycleState)
        assertTrue(repo.snapshot().historyEvents.any { it.eventType == ProductHistoryEventType.MarkedDiscarded })
    }

    @Test
    fun quantityNeverGoesNegative() = runTest {
        val file = java.io.File(tempFolder.root,"t5.preferences_pb")
        val repo = CoolventoryRepository(TestScopeStore(file, backgroundScope))
        val p = repo.addProduct(TestData.product(id = "", quantity = 1.0))
        repo.adjustQuantity(p.id, -10.0)
        assertEquals(0.0, repo.getProduct(p.id)!!.quantity, 0.0001)
    }

    @Test
    fun buyAgainFlagCreatesItem() = runTest {
        val file = java.io.File(tempFolder.root,"t6.preferences_pb")
        val repo = CoolventoryRepository(TestScopeStore(file, backgroundScope))
        val p = repo.addProduct(TestData.product(id = "", name = "Butter"))
        repo.setBuyAgainFlag(p.id, true)
        assertEquals(1, repo.snapshot().buyAgainItems.size)
        repo.setBuyAgainFlag(p.id, false)
        assertTrue(repo.snapshot().buyAgainItems.isEmpty())
    }

    @Test
    fun buyAgainCheckedClearing() = runTest {
        val file = java.io.File(tempFolder.root,"t7.preferences_pb")
        val repo = CoolventoryRepository(TestScopeStore(file, backgroundScope))
        val item = repo.addCustomBuyAgain(com.coolventory.app.model.BuyAgainItem(productName = "Eggs"))
        repo.setBuyAgainChecked(item.id, true)
        assertTrue(repo.snapshot().buyAgainItems.first().checked)
        repo.clearCheckedBuyAgain()
        assertTrue(repo.snapshot().buyAgainItems.isEmpty())
    }

    @Test
    fun resetClearsDataAndReseedsShelves() = runTest {
        val file = java.io.File(tempFolder.root,"t8.preferences_pb")
        val repo = CoolventoryRepository(TestScopeStore(file, backgroundScope))
        repo.addProduct(TestData.product(id = "", name = "Ham"))
        repo.resetAllData()
        val snap = repo.snapshot()
        assertTrue(snap.products.isEmpty())
        assertFalse(snap.shelves.isEmpty())
    }
}
