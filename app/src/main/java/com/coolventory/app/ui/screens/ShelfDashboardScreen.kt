package com.coolventory.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.ui.components.BuyAgainTray
import com.coolventory.app.ui.components.CountChip
import com.coolventory.app.ui.components.EmptyState
import com.coolventory.app.ui.components.LocationSwitcher
import com.coolventory.app.ui.components.ProductTile
import com.coolventory.app.ui.components.SectionHeader
import com.coolventory.app.ui.components.ShelfFrame
import com.coolventory.app.ui.theme.ExpiredRed
import com.coolventory.app.ui.theme.FreshGreen
import com.coolventory.app.ui.theme.RunningLowOrange
import com.coolventory.app.ui.theme.SoonAmber
import com.coolventory.app.util.ReminderUtils
import com.coolventory.app.util.ShelfUtils
import com.coolventory.app.util.StatusUtils
import com.coolventory.app.ui.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfDashboardScreen(
    viewModel: MainViewModel,
    onOpenProduct: (String) -> Unit,
    onAddProduct: (location: String, shelfId: String) -> Unit,
    onOpenReview: () -> Unit,
    onOpenAllProducts: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBuyAgain: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dismissed by viewModel.remindersDismissedThisSession.collectAsStateWithLifecycle()

    var selectedLocation by rememberSaveable { mutableStateOf(state.settings.defaultStorageLocation) }
    // Guard against an invalid persisted value.
    val location = remember(selectedLocation) { selectedLocation }

    val soon = state.settings.soonThresholdDays
    val activeAtLocation = ShelfUtils.activeProductsFor(state.products, location)
    val grouped = ShelfUtils.groupByShelf(state.products, state.shelves, location)

    var freshCount = 0
    var soonCount = 0
    var expiredCount = 0
    var lowCount = 0
    activeAtLocation.forEach { p ->
        when (StatusUtils.dateStatus(p, soon)) {
            ProductDateStatus.Fresh -> freshCount++
            ProductDateStatus.Soon, ProductDateStatus.OpenedSoon -> soonCount++
            ProductDateStatus.Expired -> expiredCount++
            else -> Unit
        }
        if (StatusUtils.isRunningLow(p)) lowCount++
    }

    val reminders = ReminderUtils.evaluate(state.products, state.buyAgain, state.settings)
    val reviewSoonList = (reminders.expired + reminders.dueToday + reminders.soon + reminders.openedSoon)
        .distinctBy { it.id }
        .take(10)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coolventory", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search products")
                    }
                    IconButton(onClick = onOpenAllProducts) {
                        Icon(Icons.Filled.ListAlt, contentDescription = "All products list")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddProduct(location.name, "") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Product") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            LocationSwitcher(selected = location, onSelect = { selectedLocation = it })
            Spacer(Modifier.height(12.dp))

            if (state.settings.reminderSettings.enabled && !dismissed && reminders.hasAnything) {
                ReminderBanner(
                    headline = reminders.headline(),
                    detail = reminders.detail(),
                    onReview = onOpenReview,
                    onDismiss = { viewModel.dismissRemindersForSession() },
                )
                Spacer(Modifier.height(12.dp))
            }

            // Status rail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CountChip("Fresh", freshCount, FreshGreen, Modifier.weight(1f))
                CountChip("Review soon", soonCount, SoonAmber, Modifier.weight(1f))
                CountChip("Passed", expiredCount, ExpiredRed, Modifier.weight(1f))
                CountChip("Low", lowCount, RunningLowOrange, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${activeAtLocation.size} active in ${location.label}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (activeAtLocation.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Add,
                    title = "This storage area is empty.",
                    message = "Add a product manually to place it on a shelf.",
                    action = {
                        TextButton(onClick = { onAddProduct(location.name, "") }) { Text("Add Product") }
                    },
                )
                // Still show the empty shelf frame so shelves remain visible.
                Spacer(Modifier.height(8.dp))
            }

            ShelfFrame(
                location = location,
                shelves = state.shelves,
                grouped = grouped,
                soonThresholdDays = soon,
                onProductClick = onOpenProduct,
                onAddToShelf = { shelfId -> onAddProduct(location.name, shelfId) },
            )

            Spacer(Modifier.height(16.dp))

            // Review Soon drawer
            SectionHeader(
                title = "Review Soon",
                trailing = { TextButton(onClick = onOpenReview) { Text("Open") } },
            )
            ReviewSoonDrawer(
                products = reviewSoonList,
                soonThresholdDays = soon,
                onOpenProduct = onOpenProduct,
            )

            Spacer(Modifier.height(16.dp))

            // Buy Again tray
            BuyAgainTray(items = state.buyAgain, onOpen = onOpenBuyAgain)

            Spacer(Modifier.height(90.dp)) // room for FAB
        }
    }
}

@Composable
private fun ReminderBanner(
    headline: String,
    detail: String,
    onReview: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SoonAmber.copy(alpha = 0.14f))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = SoonAmber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(headline, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        }
        if (detail.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onReview) { Text("Review") }
            TextButton(onClick = onDismiss) { Text("Not Now") }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ReviewSoonDrawer(
    products: List<com.coolventory.app.model.FoodProduct>,
    soonThresholdDays: Int,
    onOpenProduct: (String) -> Unit,
) {
    if (products.isEmpty()) {
        Text(
            "Nothing to review right now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            products.forEach { p ->
                ProductTile(
                    product = p,
                    soonThresholdDays = soonThresholdDays,
                    onClick = { onOpenProduct(p.id) },
                )
            }
        }
    }
}
