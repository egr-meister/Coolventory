package com.coolventory.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.DateUtils
import com.coolventory.app.util.QuantityUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    viewModel: MainViewModel,
    eventId: String?,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val event = remember(eventId, state.history) { state.historyEvent(eventId) }

    if (event == null) {
        MissingRecordScaffold(title = "Activity", message = "Activity record not found", onBack = onBack)
        return
    }

    val productExists = remember(event.productId, state.products) { state.product(event.productId) != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Product", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(
                    event.productName.ifBlank { "Deleted product" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            DetailLine("Event type", event.eventType.label)
            DetailLine("Event date", DateUtils.displayDate(event.eventDate))
            DetailLine("Event time", event.eventTime.ifBlank { "—" })
            val qtyChange = when {
                event.quantityBefore != null && event.quantityAfter != null ->
                    "${QuantityUtils.format(event.quantityBefore, com.coolventory.app.model.QuantityUnit.Pieces)} → " +
                        QuantityUtils.format(event.quantityAfter, com.coolventory.app.model.QuantityUnit.Pieces)
                event.quantityAfter != null -> QuantityUtils.format(event.quantityAfter, com.coolventory.app.model.QuantityUnit.Pieces)
                else -> "—"
            }
            DetailLine("Quantity change", qtyChange)
            if (event.description.isNotBlank()) DetailLine("Description", event.description)

            Spacer(Modifier.height(8.dp))
            if (productExists) {
                TextButton(onClick = { onOpenProduct(event.productId) }) { Text("Open product") }
            } else {
                Text(
                    "Deleted product — the related product is no longer in your inventory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(140.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}
