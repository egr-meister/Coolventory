package com.coolventory.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.model.DiscardReason
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.model.QuantityUnit
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.ui.components.ConfirmDialog
import com.coolventory.app.ui.components.DisclaimerCard
import com.coolventory.app.ui.components.DropdownField
import com.coolventory.app.ui.components.InfoTag
import com.coolventory.app.ui.components.StatusChip
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.DateUtils
import com.coolventory.app.util.QuantityUtils
import com.coolventory.app.util.ShelfUtils
import com.coolventory.app.util.StatusUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    viewModel: MainViewModel,
    productId: String?,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onOpenEvent: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val product = remember(productId, state.products) { state.product(productId) }

    if (product == null) {
        MissingRecordScaffold(title = "Product", message = "Product not found", onBack = onBack)
        return
    }

    val soon = state.settings.soonThresholdDays
    val status = StatusUtils.dateStatus(product, soon)
    val shelfName = ShelfUtils.shelfName(state.shelves, product.shelfId)
    val reviewDate = StatusUtils.effectiveReviewDate(product)

    var showDelete by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var showUsed by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showRestore by remember { mutableStateOf(false) }
    var showSetQty by remember { mutableStateOf(false) }
    var showZeroSheet by remember { mutableStateOf(false) }

    val isActive = product.lifecycleState == ProductLifecycleState.Active

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name.ifBlank { "Unnamed product" }, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { onEdit(product.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                StatusChip(status = status)
                Spacer(Modifier.width(8.dp))
                if (StatusUtils.isRunningLow(product)) {
                    InfoTag("Running low", color = com.coolventory.app.ui.theme.RunningLowOrange)
                }
                if (product.buyAgain) {
                    Spacer(Modifier.width(8.dp))
                    InfoTag("On Buy Again", color = MaterialTheme.colorScheme.tertiary)
                }
            }

            DetailRow("Storage location", product.storageLocation.label)
            DetailRow("Shelf / zone", shelfName)
            DetailRow("Category", product.categoryDisplay())
            DetailRow("Quantity", "${QuantityUtils.format(product.quantity, product.quantityUnit)} ${product.unitDisplay()}")
            product.minimumQuantity?.let { DetailRow("Minimum quantity", QuantityUtils.format(it, product.quantityUnit)) }
            DetailRow("Added date", DateUtils.displayDate(product.addedDate))
            DetailRow("Expiry date", if (product.expiryDate.isBlank()) "No expiry date" else DateUtils.displayDate(product.expiryDate))
            if (product.openedDate.isNotBlank()) DetailRow("Opened date", DateUtils.displayDate(product.openedDate))
            product.consumeWithinDaysAfterOpening?.let { DetailRow("Consume within", "$it days after opening") }
            DetailRow("Effective review date", reviewDate?.let { DateUtils.displayDate(DateUtils.format(it)) } ?: "—")
            DetailRow("Status summary", StatusUtils.reviewSummary(product, soon))
            if (product.note.isNotBlank()) DetailRow("Note", product.note)
            DetailRow("Created", DateUtils.displayDate(product.createdAt.take(10)))
            DetailRow("Updated", DateUtils.displayDate(product.updatedAt.take(10)))

            Spacer(Modifier.height(4.dp))

            if (isActive) {
                Text("Quantity", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val next = QuantityUtils.decrement(product.quantity, product.quantityUnit)
                        viewModel.setQuantity(product.id, next)
                        if (QuantityUtils.isZero(next)) showZeroSheet = true
                    }) { Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity") }
                    Text(
                        "${QuantityUtils.format(product.quantity, product.quantityUnit)} ${product.unitDisplay()}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedButton(onClick = {
                        viewModel.setQuantity(product.id, QuantityUtils.increment(product.quantity, product.quantityUnit))
                    }) { Icon(Icons.Filled.Add, contentDescription = "Increase quantity") }
                    TextButton(onClick = { showSetQty = true }) { Text("Set") }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Actions", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isActive) {
                    FilledTonalButton(onClick = { showUsed = true }) { Text("Mark Used") }
                    FilledTonalButton(onClick = { showDiscard = true }) { Text("Mark Discarded") }
                    OutlinedButton(onClick = { showMove = true }) { Text("Move") }
                    OutlinedButton(onClick = { viewModel.setBuyAgain(product.id, !product.buyAgain) }) {
                        Text(if (product.buyAgain) "Remove Buy Again" else "Add to Buy Again")
                    }
                } else {
                    FilledTonalButton(onClick = { showRestore = true }) { Text("Restore") }
                }
                OutlinedButton(onClick = { onEdit(product.id) }) { Text("Edit") }
                TextButton(onClick = { showDelete = true }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(8.dp))
            DisclaimerCard(text = "Statuses are based on manually entered dates and are not food safety assessments.")

            // Related history quick links
            val related = remember(state.history, product.id) { state.history.filter { it.productId == product.id }.take(6) }
            if (related.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Recent activity", style = MaterialTheme.typography.titleMedium)
                related.forEach { ev ->
                    TextButton(onClick = { onOpenEvent(ev.id) }) {
                        Text("${ev.eventType.label} · ${DateUtils.displayDate(ev.eventDate)}")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // -------- Dialogs --------

    if (showDelete) {
        ConfirmDialog(
            title = "Delete product?",
            message = "This permanently removes ${product.name.ifBlank { "this product" }} from your inventory. History records are kept.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDelete = false
                viewModel.deleteProduct(product.id)
                onDeleted()
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showSetQty) {
        SetQuantityDialog(
            initial = QuantityUtils.format(product.quantity, product.quantityUnit),
            unitLabel = product.unitDisplay(),
            onConfirm = { value ->
                showSetQty = false
                viewModel.setQuantity(product.id, value)
                if (QuantityUtils.isZero(value)) showZeroSheet = true
            },
            onDismiss = { showSetQty = false },
        )
    }

    if (showZeroSheet) {
        ZeroQuantityDialog(
            onMarkUsed = { showZeroSheet = false; showUsed = true },
            onAddBuyAgain = { showZeroSheet = false; viewModel.setBuyAgain(product.id, true) },
            onKeepActive = { showZeroSheet = false },
            onDismiss = { showZeroSheet = false },
        )
    }

    if (showMove) {
        MoveDialog(
            product = product,
            shelvesFor = { loc -> ShelfUtils.shelvesFor(state.shelves, loc).map { it.id to it.name } },
            onConfirm = { loc, shelf ->
                showMove = false
                viewModel.moveProduct(product.id, loc, shelf)
            },
            onDismiss = { showMove = false },
        )
    }

    if (showUsed) {
        MarkUsedDialog(
            product = product,
            onConfirm = { usedDate, finalQty, note, keepBuyAgain ->
                showUsed = false
                viewModel.markUsed(product.id, usedDate, finalQty, note, keepBuyAgain)
            },
            onDismiss = { showUsed = false },
        )
    }

    if (showDiscard) {
        MarkDiscardedDialog(
            onConfirm = { date, reason, note ->
                showDiscard = false
                viewModel.markDiscarded(product.id, date, reason, note)
            },
            onDismiss = { showDiscard = false },
        )
    }

    if (showRestore) {
        RestoreDialog(
            product = product,
            shelvesFor = { loc -> ShelfUtils.shelvesFor(state.shelves, loc).map { it.id to it.name } },
            onConfirm = { loc, shelf, qty ->
                showRestore = false
                viewModel.restoreProduct(product.id, loc, shelf, qty)
            },
            onDismiss = { showRestore = false },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun SetQuantityDialog(
    initial: String,
    unitLabel: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val parsed = QuantityUtils.parseQuantityOrNull(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set quantity") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Quantity ($unitLabel)") },
                isError = parsed == null,
                supportingText = { if (parsed == null) Text("Enter a number ≥ 0") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        },
        confirmButton = { TextButton(enabled = parsed != null, onClick = { parsed?.let(onConfirm) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ZeroQuantityDialog(
    onMarkUsed: () -> Unit,
    onAddBuyAgain: () -> Unit,
    onKeepActive: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quantity reached zero.") },
        text = { Text("Choose what to do with this product. It will not be marked used automatically.") },
        confirmButton = {
            Column {
                TextButton(onClick = onMarkUsed) { Text("Mark Used") }
                TextButton(onClick = onAddBuyAgain) { Text("Add to Buy Again") }
                TextButton(onClick = onKeepActive) { Text("Keep Active") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MoveDialog(
    product: FoodProduct,
    shelvesFor: (StorageLocation) -> List<Pair<String, String>>,
    onConfirm: (StorageLocation, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var location by remember { mutableStateOf(product.storageLocation) }
    val options = remember(location) { listOf("" to "Unassigned") + shelvesFor(location) }
    var shelfId by remember(location) { mutableStateOf(if (options.any { it.first == product.shelfId }) product.shelfId else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownField(
                    label = "Location",
                    options = StorageLocation.entries,
                    selected = location,
                    labelFor = { it.label },
                    onSelected = { location = it; shelfId = "" },
                )
                DropdownField(
                    label = "Shelf / zone",
                    options = options,
                    selected = options.firstOrNull { it.first == shelfId } ?: options.first(),
                    labelFor = { it.second },
                    onSelected = { shelfId = it.first },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(location, shelfId) }) { Text("Move") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MarkUsedDialog(
    product: FoodProduct,
    onConfirm: (usedDate: String, finalQty: Double?, note: String, keepBuyAgain: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var usedDate by remember { mutableStateOf(DateUtils.todayString()) }
    var finalQty by remember { mutableStateOf(QuantityUtils.format(product.quantity, product.quantityUnit)) }
    var note by remember { mutableStateOf("") }
    var keepBuyAgain by remember { mutableStateOf(product.buyAgain) }
    val dateValid = DateUtils.isValidDate(usedDate)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark this product as used?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This will remove it from active storage and add it to history.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(usedDate, { usedDate = it }, label = { Text("Used date (YYYY-MM-DD)") }, isError = !dateValid, singleLine = true)
                OutlinedTextField(finalQty, { finalQty = it }, label = { Text("Final quantity (optional)") }, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(note, { note = it }, label = { Text("History note (optional)") })
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = keepBuyAgain, onCheckedChange = { keepBuyAgain = it })
                    Text("Keep on Buy Again", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = dateValid, onClick = {
                onConfirm(usedDate, QuantityUtils.parseQuantityOrNull(finalQty), note, keepBuyAgain)
            }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MarkDiscardedDialog(
    onConfirm: (date: String, reason: DiscardReason, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var date by remember { mutableStateOf(DateUtils.todayString()) }
    var reason by remember { mutableStateOf(DiscardReason.NotNeeded) }
    var note by remember { mutableStateOf("") }
    val dateValid = DateUtils.isValidDate(date)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark discarded") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Discarded manually. The app does not determine the reason automatically.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, isError = !dateValid, singleLine = true)
                DropdownField(
                    label = "Reason (optional)",
                    options = DiscardReason.entries,
                    selected = reason,
                    labelFor = { it.label },
                    onSelected = { reason = it },
                )
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") })
            }
        },
        confirmButton = { TextButton(enabled = dateValid, onClick = { onConfirm(date, reason, note) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RestoreDialog(
    product: FoodProduct,
    shelvesFor: (StorageLocation) -> List<Pair<String, String>>,
    onConfirm: (StorageLocation, String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var location by remember { mutableStateOf(product.storageLocation) }
    val options = remember(location) { listOf("" to "Unassigned") + shelvesFor(location) }
    var shelfId by remember(location) { mutableStateOf(if (options.any { it.first == product.shelfId }) product.shelfId else "") }
    var qty by remember { mutableStateOf(QuantityUtils.format(product.quantity.coerceAtLeast(1.0), product.quantityUnit)) }
    val parsed = QuantityUtils.parseQuantityOrNull(qty)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore this product to active storage?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownField("Location", StorageLocation.entries, location, { it.label }, { location = it; shelfId = "" })
                DropdownField(
                    "Shelf / zone", options,
                    options.firstOrNull { it.first == shelfId } ?: options.first(),
                    { it.second }, { shelfId = it.first },
                )
                OutlinedTextField(qty, { qty = it }, label = { Text("Quantity (${product.unitDisplay()})") }, isError = parsed == null, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = { TextButton(enabled = parsed != null, onClick = { parsed?.let { onConfirm(location, shelfId, it) } }) { Text("Restore") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
