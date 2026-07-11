package com.coolventory.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.model.QuantityUnit
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.ui.components.ConfirmDialog
import com.coolventory.app.ui.components.DisclaimerCard
import com.coolventory.app.ui.components.DropdownField
import com.coolventory.app.ui.components.EXPIRY_DISCLAIMER
import com.coolventory.app.ui.components.MANUAL_DISCLAIMER
import com.coolventory.app.ui.components.ToggleRow
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.StatusUtils

private const val PRIVACY_NOTE =
    "Coolventory stores product names, storage locations, shelves, quantities, dates, statuses, " +
        "notes, Buy Again items, history, and settings locally on this device. The app has no " +
        "account, no cloud sync, no internet access, no ads, no analytics, no payments, no camera " +
        "access, no barcode scanner, no nutrition service, and no background monitoring."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenShelfManagement: () -> Unit,
    onOpenStatistics: () -> Unit,
    onShowOnboarding: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    val reminders = settings.reminderSettings

    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header("Preferences")
            DropdownField(
                label = "Default storage location",
                options = StorageLocation.entries,
                selected = settings.defaultStorageLocation,
                labelFor = { it.label },
                onSelected = { loc -> viewModel.updateSettings { it.copy(defaultStorageLocation = loc) } },
            )
            DropdownField(
                label = "Soon threshold (days)",
                options = StatusUtils.SOON_THRESHOLD_OPTIONS,
                selected = settings.soonThresholdDays.takeIf { it in StatusUtils.SOON_THRESHOLD_OPTIONS } ?: 3,
                labelFor = { "$it day${if (it == 1) "" else "s"}" },
                onSelected = { days -> viewModel.updateSettings { it.copy(soonThresholdDays = days) } },
            )
            DropdownField(
                label = "Default quantity unit",
                options = QuantityUnit.entries,
                selected = settings.defaultQuantityUnit,
                labelFor = { it.label },
                onSelected = { u -> viewModel.updateSettings { it.copy(defaultQuantityUnit = u) } },
            )
            ToggleRow(
                label = "Start week on Monday",
                supporting = "Used when grouping monthly history",
                checked = settings.firstDayMonday,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(firstDayMonday = v) } },
            )

            HorizontalDivider()
            Header("In-app reminders")
            DisclaimerCard(text = "Coolventory reminders appear inside the app. The app does not send push notifications or run in the background.")
            ToggleRow(
                label = "Enable in-app reminders",
                checked = reminders.enabled,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(reminderSettings = it.reminderSettings.copy(enabled = v)) } },
            )
            ToggleRow(
                label = "Expiry-passed reminder",
                checked = reminders.showExpired,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(reminderSettings = it.reminderSettings.copy(showExpired = v)) } },
            )
            ToggleRow(
                label = "Due-today reminder",
                checked = reminders.showDueToday,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(reminderSettings = it.reminderSettings.copy(showDueToday = v)) } },
            )
            ToggleRow(
                label = "Soon reminder",
                checked = reminders.showSoon,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(reminderSettings = it.reminderSettings.copy(showSoon = v)) } },
            )
            ToggleRow(
                label = "Low-quantity reminder",
                checked = reminders.showLowQuantity,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(reminderSettings = it.reminderSettings.copy(showLowQuantity = v)) } },
            )
            ToggleRow(
                label = "Buy Again reminder",
                checked = reminders.showBuyAgain,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(reminderSettings = it.reminderSettings.copy(showBuyAgain = v)) } },
            )

            HorizontalDivider()
            Header("Manage")
            SettingsButton("Shelf management") { onOpenShelfManagement() }
            SettingsButton("Statistics") { onOpenStatistics() }
            SettingsButton("Show onboarding again") {
                viewModel.showOnboardingAgain(); onShowOnboarding()
            }
            SettingsButton("Restore default shelves") { confirm = ConfirmAction.RestoreShelves }
            SettingsButton("Clear checked Buy Again items") { confirm = ConfirmAction.ClearBuyAgain }

            HorizontalDivider()
            Header("Data")
            SettingsButton("Delete used-product history", destructive = true) { confirm = ConfirmAction.DeleteUsed }
            SettingsButton("Delete discarded-product history", destructive = true) { confirm = ConfirmAction.DeleteDiscarded }
            SettingsButton("Delete all active products", destructive = true) { confirm = ConfirmAction.DeleteActive }
            SettingsButton("Reset all local data", destructive = true) { confirm = ConfirmAction.ResetAll }

            HorizontalDivider()
            Header("About & privacy")
            Text("Coolventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("A manual, offline food inventory and expiry organizer. Version 1.0.0.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DisclaimerCard(text = MANUAL_DISCLAIMER)
            DisclaimerCard(text = EXPIRY_DISCLAIMER)
            DisclaimerCard(text = PRIVACY_NOTE)
            Spacer(Modifier.height(24.dp))
        }
    }

    confirm?.let { action ->
        when (action) {
            ConfirmAction.RestoreShelves -> ConfirmDialog(
                title = "Restore default shelves?",
                message = "This re-adds and re-enables the default shelves. Custom shelves are kept.",
                confirmLabel = "Restore",
                onConfirm = { confirm = null; viewModel.restoreDefaultShelves() },
                onDismiss = { confirm = null },
            )
            ConfirmAction.ClearBuyAgain -> ConfirmDialog(
                title = "Clear checked Buy Again items?",
                message = "This removes all items marked as purchased.",
                confirmLabel = "Clear",
                destructive = true,
                onConfirm = { confirm = null; viewModel.clearCheckedBuyAgain() },
                onDismiss = { confirm = null },
            )
            ConfirmAction.DeleteUsed -> ConfirmDialog(
                title = "Delete used-product history?",
                message = "This permanently removes all 'marked used' history events.",
                confirmLabel = "Delete",
                destructive = true,
                onConfirm = { confirm = null; viewModel.deleteUsedHistory() },
                onDismiss = { confirm = null },
            )
            ConfirmAction.DeleteDiscarded -> ConfirmDialog(
                title = "Delete discarded-product history?",
                message = "This permanently removes all 'marked discarded' history events.",
                confirmLabel = "Delete",
                destructive = true,
                onConfirm = { confirm = null; viewModel.deleteDiscardedHistory() },
                onDismiss = { confirm = null },
            )
            ConfirmAction.DeleteActive -> ConfirmDialog(
                title = "Delete all active products?",
                message = "This permanently removes every active product. Used and discarded records are kept.",
                confirmLabel = "Delete",
                destructive = true,
                onConfirm = { confirm = null; viewModel.deleteAllActiveProducts() },
                onDismiss = { confirm = null },
            )
            ConfirmAction.ResetAll -> ConfirmDialog(
                title = "Reset all local data?",
                message = "This will permanently remove every product, quantity, date, shelf, note, Buy Again item, history record, and setting stored by Coolventory on this device.",
                confirmLabel = "Reset everything",
                destructive = true,
                onConfirm = { confirm = null; viewModel.resetAllData() },
                onDismiss = { confirm = null },
            )
        }
    }
}

private enum class ConfirmAction { RestoreShelves, ClearBuyAgain, DeleteUsed, DeleteDiscarded, DeleteActive, ResetAll }

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SettingsButton(text: String, destructive: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
