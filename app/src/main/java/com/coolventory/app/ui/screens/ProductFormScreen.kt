package com.coolventory.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.model.FoodCategory
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.QuantityUnit
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.ui.components.DisclaimerCard
import com.coolventory.app.ui.components.DropdownField
import com.coolventory.app.ui.components.FieldLabel
import com.coolventory.app.ui.components.LabeledTextField
import com.coolventory.app.ui.components.MANUAL_DISCLAIMER
import com.coolventory.app.ui.components.ToggleRow
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.DateUtils
import com.coolventory.app.util.QuantityUtils
import com.coolventory.app.util.ShelfUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    viewModel: MainViewModel,
    productId: String?,
    presetLocation: StorageLocation?,
    presetShelfId: String?,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editing = productId != null
    val existing = remember(productId, state.products) { state.product(productId) }

    // Missing-record fallback for an edit target that no longer exists.
    if (editing && existing == null) {
        MissingRecordScaffold(title = "Edit Product", message = "Product not found", onBack = onCancel)
        return
    }

    val defaultUnit = state.settings.defaultQuantityUnit

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var location by remember { mutableStateOf(existing?.storageLocation ?: presetLocation ?: state.settings.defaultStorageLocation) }
    var shelfId by remember { mutableStateOf(existing?.shelfId ?: presetShelfId ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: FoodCategory.Other) }
    var customCategory by remember { mutableStateOf(existing?.customCategoryName ?: "") }
    var quantityText by remember { mutableStateOf(existing?.let { QuantityUtils.format(it.quantity, it.quantityUnit) } ?: "1") }
    var unit by remember { mutableStateOf(existing?.quantityUnit ?: defaultUnit) }
    var customUnit by remember { mutableStateOf(existing?.customQuantityUnit ?: "") }
    var addedDate by remember { mutableStateOf(existing?.addedDate?.ifBlank { DateUtils.todayString() } ?: DateUtils.todayString()) }
    var expiryDate by remember { mutableStateOf(existing?.expiryDate ?: "") }
    var openedDate by remember { mutableStateOf(existing?.openedDate ?: "") }
    var consumeWithin by remember { mutableStateOf(existing?.consumeWithinDaysAfterOpening?.toString() ?: "") }
    var minimumQty by remember { mutableStateOf(existing?.minimumQuantity?.let { QuantityUtils.format(it, unit) } ?: "") }
    var buyAgain by remember { mutableStateOf(existing?.buyAgain ?: false) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var attemptedSave by remember { mutableStateOf(false) }

    // Shelf options for the chosen location, plus an "Unassigned" choice (empty id).
    val shelfOptions = remember(location, state.shelves) {
        listOf("" to "Unassigned") + ShelfUtils.shelvesFor(state.shelves, location).map { it.id to it.name }
    }
    // Effective, always-valid selection for display (does not mutate state during composition).
    val selectedShelfOption = shelfOptions.firstOrNull { it.first == shelfId } ?: shelfOptions.first()

    // Validation
    val nameError = attemptedSave && name.isBlank()
    val quantityValue = QuantityUtils.parseQuantityOrNull(quantityText)
    val quantityError = attemptedSave && quantityValue == null
    val addedValid = DateUtils.isValidDate(addedDate)
    val addedError = attemptedSave && !addedValid
    val expiryError = expiryDate.isNotBlank() && !DateUtils.isValidDate(expiryDate)
    val openedError = openedDate.isNotBlank() && !DateUtils.isValidDate(openedDate)
    val consumeError = consumeWithin.isNotBlank() && ((consumeWithin.toIntOrNull() ?: 0) <= 0)
    val minError = minimumQty.isNotBlank() && (QuantityUtils.parseQuantityOrNull(minimumQty) == null)
    val customCategoryError = attemptedSave && category == FoodCategory.Other && customCategory.isBlank()

    val expiryBeforeAdded = !expiryError && expiryDate.isNotBlank() && addedValid &&
        (DateUtils.parseOrNull(expiryDate)?.isBefore(DateUtils.parseOrNull(addedDate)) == true)

    val hasBlockingError = nameError || quantityError || addedError || expiryError ||
        openedError || consumeError || minError || customCategoryError

    fun save() {
        attemptedSave = true
        val qty = QuantityUtils.parseQuantityOrNull(quantityText)
        val blocking = name.isBlank() || qty == null || !DateUtils.isValidDate(addedDate) ||
            (expiryDate.isNotBlank() && !DateUtils.isValidDate(expiryDate)) ||
            (openedDate.isNotBlank() && !DateUtils.isValidDate(openedDate)) ||
            (consumeWithin.isNotBlank() && (consumeWithin.toIntOrNull() ?: 0) <= 0) ||
            (minimumQty.isNotBlank() && QuantityUtils.parseQuantityOrNull(minimumQty) == null) ||
            (category == FoodCategory.Other && customCategory.isBlank())
        if (blocking || qty == null) return

        // Sanitize shelf: only keep it if it belongs to the selected location.
        val finalShelf = if (shelfOptions.any { it.first == shelfId }) shelfId else ""
        val product = (existing ?: FoodProduct()).copy(
            name = name.trim(),
            storageLocation = location,
            category = category,
            customCategoryName = if (category == FoodCategory.Other) customCategory.trim() else "",
            shelfId = finalShelf,
            quantity = qty,
            quantityUnit = unit,
            customQuantityUnit = if (unit == QuantityUnit.Custom) customUnit.trim() else "",
            addedDate = addedDate.trim(),
            expiryDate = expiryDate.trim(),
            openedDate = openedDate.trim(),
            consumeWithinDaysAfterOpening = consumeWithin.toIntOrNull()?.takeIf { it > 0 },
            minimumQuantity = QuantityUtils.parseQuantityOrNull(minimumQty),
            buyAgain = buyAgain,
            note = note.trim(),
        )
        if (editing) {
            viewModel.updateProduct(product)
            onDone()
        } else {
            viewModel.addProduct(product) { onDone() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit Product" else "Add Product") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DisclaimerCard(text = MANUAL_DISCLAIMER)

            LabeledTextField(
                label = "Product name *",
                value = name,
                onValueChange = { name = it },
                isError = nameError,
                supportingText = if (nameError) "Name is required" else null,
            )

            DropdownField(
                label = "Storage location *",
                options = StorageLocation.entries,
                selected = location,
                labelFor = { it.label },
                onSelected = { location = it },
            )

            DropdownField(
                label = "Shelf / zone",
                options = shelfOptions,
                selected = selectedShelfOption,
                labelFor = { it.second },
                onSelected = { shelfId = it.first },
            )

            DropdownField(
                label = "Category *",
                options = FoodCategory.entries,
                selected = category,
                labelFor = { it.label },
                onSelected = { category = it },
            )
            if (category == FoodCategory.Other) {
                LabeledTextField(
                    label = "Custom category name *",
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    isError = customCategoryError,
                    supportingText = if (customCategoryError) "Enter a category name" else null,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledTextField(
                    label = "Quantity *",
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    keyboardType = KeyboardType.Decimal,
                    isError = quantityError,
                    supportingText = if (quantityError) "Enter a number ≥ 0" else null,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.height(0.dp))
            }
            DropdownField(
                label = "Unit *",
                options = QuantityUnit.entries,
                selected = unit,
                labelFor = { it.label },
                onSelected = { unit = it },
            )
            if (unit == QuantityUnit.Custom) {
                LabeledTextField(
                    label = "Custom unit label",
                    value = customUnit,
                    onValueChange = { customUnit = it },
                    placeholder = "e.g. half jar",
                )
            }

            FieldLabel("Dates (YYYY-MM-DD)")
            LabeledTextField(
                label = "Added date *",
                value = addedDate,
                onValueChange = { addedDate = it },
                placeholder = "2026-07-10",
                isError = addedError,
                supportingText = if (addedError) "Enter a valid date (YYYY-MM-DD)" else null,
            )
            LabeledTextField(
                label = "Expiry date (optional)",
                value = expiryDate,
                onValueChange = { expiryDate = it },
                placeholder = "Leave blank for no expiry date",
                isError = expiryError,
                supportingText = when {
                    expiryError -> "Enter a valid date or leave blank"
                    expiryBeforeAdded -> "Expiry is before the added date — allowed for historical records"
                    else -> null
                },
            )

            FieldLabel("Opened-product timing (optional)")
            DisclaimerCard(text = "Opened-product timing is based only on the values you enter. Always follow package guidance.")
            LabeledTextField(
                label = "Opened date (optional)",
                value = openedDate,
                onValueChange = { openedDate = it },
                placeholder = "YYYY-MM-DD",
                isError = openedError,
                supportingText = if (openedError) "Enter a valid date or leave blank" else null,
            )
            LabeledTextField(
                label = "Consume within days after opening (optional)",
                value = consumeWithin,
                onValueChange = { consumeWithin = it.filter { c -> c.isDigit() } },
                keyboardType = KeyboardType.Number,
                isError = consumeError,
                supportingText = if (consumeError) "Must be greater than 0" else null,
            )

            LabeledTextField(
                label = "Minimum quantity (optional)",
                value = minimumQty,
                onValueChange = { minimumQty = it },
                keyboardType = KeyboardType.Decimal,
                isError = minError,
                supportingText = if (minError) "Enter a number ≥ 0 or leave blank" else "At or below this, the item shows Running Low",
            )

            ToggleRow(
                label = "Add to Buy Again",
                checked = buyAgain,
                onCheckedChange = { buyAgain = it },
            )

            LabeledTextField(
                label = "Note (optional)",
                value = note,
                onValueChange = { note = it },
                singleLine = false,
            )

            if (attemptedSave && hasBlockingError) {
                Text(
                    "Please fix the highlighted fields.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (editing) "Save Changes" else "Save Product")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MissingRecordScaffold(
    title: String,
    message: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "The item you were viewing is no longer available. It may have been deleted or reset.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onBack) { Text("Go Back") }
        }
    }
}
