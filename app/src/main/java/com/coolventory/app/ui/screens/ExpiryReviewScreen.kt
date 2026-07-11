package com.coolventory.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.model.ProductLifecycleState
import com.coolventory.app.ui.components.DisclaimerCard
import com.coolventory.app.ui.components.EXPIRY_DISCLAIMER
import com.coolventory.app.ui.components.EmptyState
import com.coolventory.app.ui.components.ProductRow
import com.coolventory.app.ui.components.SectionHeader
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.DateUtils
import com.coolventory.app.util.StatusUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryReviewScreen(
    viewModel: MainViewModel,
    onOpenProduct: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val soon = state.settings.soonThresholdDays

    val active = remember(state.products) {
        state.products.filter { it.lifecycleState == ProductLifecycleState.Active }
    }

    val expired = mutableListOf<FoodProduct>()
    val dueToday = mutableListOf<FoodProduct>()
    val reviewSoon = mutableListOf<FoodProduct>()
    val openedSoon = mutableListOf<FoodProduct>()
    val noExpiry = mutableListOf<FoodProduct>()

    active.forEach { p ->
        when (StatusUtils.dateStatus(p, soon)) {
            ProductDateStatus.Expired -> expired += p
            ProductDateStatus.Soon -> if (StatusUtils.daysUntilReview(p) == 0L) dueToday += p else reviewSoon += p
            ProductDateStatus.OpenedSoon -> if (StatusUtils.daysUntilReview(p) == 0L) dueToday += p else openedSoon += p
            ProductDateStatus.NoExpiryDate -> noExpiry += p
            else -> Unit
        }
    }

    val anything = expired.isNotEmpty() || dueToday.isNotEmpty() || reviewSoon.isNotEmpty() ||
        openedSoon.isNotEmpty() || noExpiry.isNotEmpty()

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Expiry Review") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = { viewModel.dismissRemindersForSession() }) { Text("Dismiss") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            DisclaimerCard(text = EXPIRY_DISCLAIMER)
            Spacer(Modifier.height(8.dp))

            if (!anything) {
                EmptyState(
                    icon = Icons.Filled.EventAvailable,
                    title = "Nothing to review.",
                    message = "No products are expired, due, or missing a date right now.",
                )
            }

            ReviewSection("Expiry date passed", expired, state, soon, onOpenProduct)
            ReviewSection("Due today", dueToday, state, soon, onOpenProduct)
            ReviewSection("Review soon", reviewSoon, state, soon, onOpenProduct)
            ReviewSection("Opened-product review", openedSoon, state, soon, onOpenProduct)
            ReviewSection("No expiry date", noExpiry, state, soon, onOpenProduct)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReviewSection(
    title: String,
    products: List<FoodProduct>,
    state: com.coolventory.app.ui.vm.CoolventoryUiState,
    soon: Int,
    onOpenProduct: (String) -> Unit,
) {
    if (products.isEmpty()) return
    SectionHeader(title = "$title (${products.size})")
    products.forEach { p ->
        ProductRow(
            product = p,
            shelves = state.shelves,
            soonThresholdDays = soon,
            onClick = { onOpenProduct(p.id) },
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
    Spacer(Modifier.height(8.dp))
}
