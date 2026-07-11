package com.coolventory.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.model.StorageShelf
import com.coolventory.app.ui.components.ConfirmDialog
import com.coolventory.app.ui.components.SectionHeader
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.ShelfUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var addFor by remember { mutableStateOf<StorageLocation?>(null) }
    var renameShelf by remember { mutableStateOf<StorageShelf?>(null) }
    var showRestore by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shelf Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = { TextButton(onClick = { showRestore = true }) { Text("Restore defaults") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
        ) {
            StorageLocation.entries.forEach { location ->
                val shelves = ShelfUtils.allShelvesFor(state.shelves, location)
                SectionHeader(
                    title = location.label,
                    trailing = {
                        IconButton(onClick = { addFor = location }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add custom shelf to ${location.label}")
                        }
                    },
                )
                shelves.forEachIndexed { index, shelf ->
                    ShelfManagementRow(
                        shelf = shelf,
                        isFirst = index == 0,
                        isLast = index == shelves.lastIndex,
                        onRename = { renameShelf = shelf },
                        onUp = { viewModel.reorderShelf(shelf.id, moveUp = true) },
                        onDown = { viewModel.reorderShelf(shelf.id, moveUp = false) },
                        onToggleHidden = { viewModel.setShelfEnabled(shelf.id, !shelf.enabled) },
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(10.dp))
            }
            Text(
                "Default shelves cannot be hidden or deleted. Use “Restore defaults” to reset the layout while keeping custom shelves.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    addFor?.let { location ->
        ShelfNameDialog(
            title = "Add custom shelf",
            initial = "",
            onConfirm = { name ->
                addFor = null
                viewModel.addCustomShelf(location, name)
            },
            onDismiss = { addFor = null },
        )
    }

    renameShelf?.let { shelf ->
        ShelfNameDialog(
            title = "Rename shelf",
            initial = shelf.name,
            onConfirm = { name ->
                renameShelf = null
                viewModel.renameShelf(shelf.id, name)
            },
            onDismiss = { renameShelf = null },
        )
    }

    if (showRestore) {
        ConfirmDialog(
            title = "Restore default shelves?",
            message = "This re-adds and re-enables the default shelves. Your custom shelves are kept.",
            confirmLabel = "Restore",
            onConfirm = { showRestore = false; viewModel.restoreDefaultShelves() },
            onDismiss = { showRestore = false },
        )
    }
}

@Composable
private fun ShelfManagementRow(
    shelf: StorageShelf,
    isFirst: Boolean,
    isLast: Boolean,
    onRename: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onToggleHidden: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                shelf.name + if (!shelf.enabled) " (hidden)" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                if (shelf.isDefault) "Default shelf" else "Custom shelf",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onUp, enabled = !isFirst) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
        }
        IconButton(onClick = onDown, enabled = !isLast) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
        }
        IconButton(onClick = onRename) {
            Icon(Icons.Filled.Edit, contentDescription = "Rename shelf")
        }
        if (!shelf.isDefault) {
            IconButton(onClick = onToggleHidden) {
                Icon(
                    if (shelf.enabled) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (shelf.enabled) "Hide shelf" else "Show shelf",
                )
            }
        }
    }
}

@Composable
private fun ShelfNameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(name, { name = it }, label = { Text("Shelf name") }, singleLine = true)
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
