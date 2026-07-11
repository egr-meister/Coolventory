package com.coolventory.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.ui.theme.CoolTeal
import com.coolventory.app.ui.theme.FreezerDeepBlue
import com.coolventory.app.ui.theme.PantryBrown
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.StatsUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = remember(state.products, state.history, state.buyAgain) {
        StatsUtils.compute(state.products, state.history, state.buyAgain.size)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatLine("Active products", stats.activeProducts.toString())

            Text("Storage distribution", style = MaterialTheme.typography.titleMedium)
            val total = (stats.fridgeCount + stats.freezerCount + stats.pantryCount).coerceAtLeast(1)
            DistributionStrip("Fridge", stats.fridgeCount, total, CoolTeal)
            DistributionStrip("Freezer", stats.freezerCount, total, FreezerDeepBlue)
            DistributionStrip("Pantry", stats.pantryCount, total, PantryBrown)

            Spacer(Modifier.height(4.dp))
            Text("This month", style = MaterialTheme.typography.titleMedium)
            StatLine("Recorded as used", stats.usedThisMonth.toString())
            StatLine("Recorded as discarded", stats.discardedThisMonth.toString())

            Spacer(Modifier.height(4.dp))
            StatLine("Added to Buy Again", stats.buyAgainCount.toString())
            StatLine("Most used category", stats.mostUsedCategory)
            StatLine(
                "Average active duration",
                stats.averageActiveDurationDays?.let { "$it days" } ?: "Not enough data",
            )
            StatLine("No expiry date entered", stats.withoutExpiryDate.toString())

            Spacer(Modifier.height(8.dp))
            Text(
                "These are neutral local counts only. Coolventory does not score health, nutrition, or waste.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DistributionStrip(label: String, count: Int, total: Int, color: Color) {
    val fraction = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}
