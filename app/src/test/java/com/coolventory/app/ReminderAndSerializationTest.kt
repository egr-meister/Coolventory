package com.coolventory.app

import com.coolventory.app.data.CoolventoryJson
import com.coolventory.app.model.AppSettings
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ReminderSettings
import com.coolventory.app.util.ReminderUtils
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderAndSerializationTest {

    private val today = LocalDate.parse(TestData.TODAY)
    private val json = CoolventoryJson.instance

    @Test
    fun reminderEvaluationBuckets() {
        val expired = TestData.product(id = "e", expiryDate = "2026-07-01")
        val dueToday = TestData.product(id = "d", expiryDate = TestData.TODAY)
        val soon = TestData.product(id = "s", expiryDate = "2026-07-12")
        val low = TestData.product(id = "l", quantity = 1.0, minimumQuantity = 2.0, expiryDate = "2026-09-01")
        val settings = AppSettings(soonThresholdDays = 3, reminderSettings = ReminderSettings())

        val snapshot = ReminderUtils.evaluate(listOf(expired, dueToday, soon, low), emptyList(), settings, today)
        assertTrue(snapshot.expired.any { it.id == "e" })
        assertTrue(snapshot.dueToday.any { it.id == "d" })
        assertTrue(snapshot.soon.any { it.id == "s" })
        assertTrue(snapshot.runningLow.any { it.id == "l" })
        assertTrue(snapshot.hasAnything)
    }

    @Test
    fun disabledRemindersProduceEmptySnapshot() {
        val settings = AppSettings(reminderSettings = ReminderSettings(enabled = false))
        val snapshot = ReminderUtils.evaluate(listOf(TestData.product(expiryDate = "2026-07-01")), emptyList(), settings, today)
        assertEquals(0, snapshot.totalProductsNeedingReview)
    }

    @Test
    fun roundTripSerialization() {
        val list = listOf(TestData.product(id = "x"), TestData.product(id = "y"))
        val encoded = json.encodeToString(ListSerializer(FoodProduct.serializer()), list)
        val decoded = json.decodeFromString(ListSerializer(FoodProduct.serializer()), encoded)
        assertEquals(2, decoded.size)
        assertEquals("x", decoded.first().id)
    }

    @Test
    fun corruptedJsonItemLevelRecovery() {
        // Structurally-valid array where one element (a bare number) cannot decode into a product.
        // Whole-list decode fails, but item-level recovery keeps the valid object.
        val raw = """[{"id":"good","name":"Milk"}, 12345]"""
        val recovered = decodeListWithRecovery(raw)
        assertTrue(recovered.any { it.id == "good" })
    }

    @Test
    fun emptyAndBlankJsonFallBackToEmptyList() {
        assertTrue(decodeListWithRecovery("").isEmpty())
        assertTrue(decodeListWithRecovery("not json").isEmpty())
    }

    @Test
    fun unknownEnumValueCoercesToDefault() {
        val raw = """[{"id":"z","name":"Item","category":"NotARealCategory"}]"""
        val decoded = decodeListWithRecovery(raw)
        assertEquals(1, decoded.size)
        // coerceInputValues => falls back to declared default (Other)
        assertEquals(com.coolventory.app.model.FoodCategory.Other, decoded.first().category)
    }

    /** Mirrors the repository's decode-with-recovery strategy for test purposes. */
    private fun decodeListWithRecovery(raw: String?): List<FoodProduct> {
        if (raw.isNullOrBlank()) return emptyList()
        try {
            return json.decodeFromString(ListSerializer(FoodProduct.serializer()), raw)
        } catch (_: Exception) {
        }
        return try {
            val element = json.parseToJsonElement(raw)
            if (element !is JsonArray) return emptyList()
            element.mapNotNull { item ->
                try {
                    json.decodeFromJsonElement(FoodProduct.serializer(), item)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
