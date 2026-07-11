package com.coolventory.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.model.BuyAgainItem
import com.coolventory.app.model.FoodCategory
import com.coolventory.app.model.QuantityUnit
import com.coolventory.app.ui.components.BuyAgainSlip
import com.coolventory.app.ui.components.ConfirmDialog
import com.coolventory.app.ui.components.DropdownField
import com.coolventory.app.ui.components.EmptyState
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.QuantityUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyAgainScreen(
    viewModel: MainViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BuyAgainItem?>(null) }

    val unchecked = remember(state.buyAgain) { state.buyAgain.filterNot { it.checked }.sortedByDescending { it.updatedAt } }
    val checked = remember(state.buyAgain) { state.buyAgain.filter { it.checked }.sortedByDescending { it.updatedAt } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buy Again") },
                actions = {
                    if (checked.isNotEmpty()) {
                        TextButton(onClick = { showClear = true }) { Text("Clear checked") }
                    }
                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = "Add custom item") }
                },
            )
        },
    ) { padding ->
        if (state.buyAgain.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.ShoppingBag,
                    title = "Your Buy Again list is empty.",
                    message = "Flag products you want to buy again, or add a custom item.",
                    action = { TextButton(onClick = { showAdd = true }) { Text("Add custom item") } },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            ) {
                if (unchecked.isNotEmpty()) {
                    item(key = "hdr_todo") {
                        SectionLabel("To buy (${unchecked.size})")
                    }
                    items(unchecked.size, key = { unchecked[it].id }) { i ->
                        val item = unchecked[i]
                        BuyAgainSlip(
                            item = item,
                            onToggleChecked = { viewModel.setBuyAgainChecked(item.id, it) },
                            onClick = { editing = item },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
                if (checked.isNotEmpty()) {
                    item(key = "hdr_done") {
                        Spacer(Modifier.height(8.dp))
                        SectionLabel("Purchased (${checked.size})")
                    }
                    items(checked.size, key = { checked[it].id }) { i ->
                        val item = checked[i]
                        BuyAgainSlip(
                            item = item,
                            onToggleChecked = { viewModel.setBuyAgainChecked(item.id, it) },
                            onClick = { editing = item },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        BuyAgainEditorDialog(
            initial = null,
            onSave = { item ->
                showAdd = false
                viewModel.addCustomBuyAgain(item)
            },
            onDismiss = { showAdd = false },
        )
    }

    editing?.let { item ->
        BuyAgainEditorDialog(
            initial = item,
            onSave = { updated ->
                editing = null
                viewModel.updateBuyAgain(updated)
            },
            onDelete = {
                editing = null
                viewModel.deleteBuyAgain(item.id)
            },
            onDismiss = { editing = null },
        )
    }

    if (showClear) {
        ConfirmDialog(
            title = "Clear checked items?",
            message = "This removes all items you have marked as purchased.",
            confirmLabel = "Clear",
            destructive = true,
            onConfirm = { showClear = false; viewModel.clearCheckedBuyAgain() },
            onDismiss = { showClear = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

@Composable
private fun BuyAgainEditorDialog(
    initial: BuyAgainItem?,
    onSave: (BuyAgainItem) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.productName ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: FoodCategory.Other) }
    var qty by remember { mutableStateOf(initial?.preferredQuantity?.let { QuantityUtils.format(it, initial.preferredUnit) } ?: "") }
    var unit by remember { mutableStateOf(initial?.preferredUnit ?: QuantityUnit.Pieces) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    val nameValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Buy Again item" else "Edit item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Item name *") }, isError = !nameValid, singleLine = true)
                DropdownField("Category", FoodCategory.entries, category, { it.label }, { category = it })
                OutlinedTextField(
                    qty, { qty = it },
                    label = { Text("Preferred quantity (optional)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                DropdownField("Preferred unit", QuantityUnit.entries, unit, { it.label }, { unit = it })
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") })
            }
        },
        confirmButton = {
            TextButton(enabled = nameValid, onClick = {
                val base = initial ?: BuyAgainItem()
                onSave(
                    base.copy(
                        productName = name.trim(),
                        category = category,
                        preferredQuantity = QuantityUtils.parseQuantityOrNull(qty),
                        preferredUnit = unit,
                        note = note.trim(),
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = {
            Column {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
