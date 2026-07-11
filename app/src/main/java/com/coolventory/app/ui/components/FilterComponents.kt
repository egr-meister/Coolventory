package com.coolventory.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coolventory.app.model.FoodCategory
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.util.ProductQuery
import com.coolventory.app.util.ProductSort
import com.coolventory.app.util.StatusFilter

/** A single scrollable row of toggleable filter chips + a sort selector, editing a [ProductQuery]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterControls(
    query: ProductQuery,
    onChange: (ProductQuery) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Location chips
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StorageLocation.entries.forEach { loc ->
            val selected = loc in query.locations
            FilterChip(
                selected = selected,
                onClick = {
                    val set = query.locations.toMutableSet().apply { if (selected) remove(loc) else add(loc) }
                    onChange(query.copy(locations = set))
                },
                label = { Text(loc.label) },
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    // Status chips
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusFilter.entries.forEach { st ->
            val selected = st in query.statuses
            FilterChip(
                selected = selected,
                onClick = {
                    val set = query.statuses.toMutableSet().apply { if (selected) remove(st) else add(st) }
                    onChange(query.copy(statuses = set))
                },
                label = { Text(st.label) },
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    // Category chips
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FoodCategory.entries.forEach { cat ->
            val selected = cat in query.categories
            FilterChip(
                selected = selected,
                onClick = {
                    val set = query.categories.toMutableSet().apply { if (selected) remove(cat) else add(cat) }
                    onChange(query.copy(categories = set))
                },
                label = { Text(cat.label) },
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    // Sort selector
    SortDropdown(current = query.sort, onSelected = { onChange(query.copy(sort = it)) })
}

@Composable
fun SortDropdown(current: ProductSort, onSelected: (ProductSort) -> Unit) {
    DropdownField(
        label = "Sort by",
        options = ProductSort.entries,
        selected = current,
        labelFor = { it.label },
        onSelected = onSelected,
    )
}
