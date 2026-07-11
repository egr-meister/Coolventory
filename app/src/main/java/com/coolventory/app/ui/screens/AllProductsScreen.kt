package com.coolventory.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolventory.app.ui.components.EmptyState
import com.coolventory.app.ui.components.FilterControls
import com.coolventory.app.ui.components.ProductRow
import com.coolventory.app.ui.vm.MainViewModel
import com.coolventory.app.util.ProductQuery
import com.coolventory.app.util.SearchUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllProductsScreen(
    viewModel: MainViewModel,
    onOpenProduct: (String) -> Unit,
    onAddProduct: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf(ProductQuery()) }
    var showFilters by remember { mutableStateOf(false) }

    val results = remember(state.products, state.shelves, query, state.settings.soonThresholdDays) {
        SearchUtils.query(state.products, state.shelves, query, state.settings.soonThresholdDays)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Products") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Toggle filters")
                    }
                    IconButton(onClick = onAddProduct) {
                        Icon(Icons.Filled.Add, contentDescription = "Add product")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp)) {
            if (showFilters) {
                Spacer(Modifier.height(8.dp))
                FilterControls(query = query, onChange = { query = it })
                Spacer(Modifier.height(8.dp))
            }
            if (results.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = "No matching products.",
                    message = "Adjust filters or add a new product.",
                )
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
                    items(results, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            shelves = state.shelves,
                            soonThresholdDays = state.settings.soonThresholdDays,
                            onClick = { onOpenProduct(product.id) },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
