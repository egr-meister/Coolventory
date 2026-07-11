package com.coolventory.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.model.ProductHistoryEvent
import com.coolventory.app.model.ProductHistoryEventType
import com.coolventory.app.ui.components.EmptyState
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onOpenEvent: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Used", "Discarded", "All Activity")

    val filtered = remember(state.history, tab) {
        when (tab) {
            0 -> state.history.filter { it.eventType == ProductHistoryEventType.MarkedUsed }
            1 -> state.history.filter { it.eventType == ProductHistoryEventType.MarkedDiscarded }
            else -> state.history
        }.sortedByDescending { it.createdAt }
    }

    // Group by display date preserving order.
    val grouped = remember(filtered) {
        LinkedHashMap<String, MutableList<ProductHistoryEvent>>().apply {
            filtered.forEach { ev ->
                val key = DateUtils.displayDate(ev.eventDate)
                getOrPut(key) { mutableListOf() }.add(ev)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("History") }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                }
            }
            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.History,
                    title = "No activity yet.",
                    message = "Actions on your products appear here as a neutral record.",
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                ) {
                    grouped.forEach { (dateLabel, events) ->
                        item(key = "header_$dateLabel") {
                            Text(
                                dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                        items(events.size, key = { events[it].id }) { i ->
                            HistoryRow(events[i], onClick = { onOpenEvent(events[i].id) })
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(event: ProductHistoryEvent, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(
                event.productName.ifBlank { "Deleted product" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(event.eventTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${event.eventType.label}${if (event.description.isNotBlank()) " · ${event.description}" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
