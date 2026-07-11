package com.coolventory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.model.StorageLocation
import com.coolventory.app.model.StorageShelf
import com.coolventory.app.ui.theme.ExpiredRed
import com.coolventory.app.ui.theme.RunningLowOrange
import com.coolventory.app.ui.theme.SoonAmber
import com.coolventory.app.ui.theme.StatusVisuals
import com.coolventory.app.util.ShelfUtils
import com.coolventory.app.util.StatusUtils

/** Top switcher: Fridge | Freezer | Pantry. */
@Composable
fun LocationSwitcher(
    selected: StorageLocation,
    onSelect: (StorageLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StorageLocation.entries.forEach { location ->
            val active = location == selected
            val accent = StatusVisuals.locationAccent(location)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) accent else Color.Transparent)
                    .clickable { onSelect(location) }
                    .semantics { contentDescription = "${location.label} storage${if (active) ", selected" else ""}" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = location.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * The open shelf frame for a location. Draws an outer frame with a cooling header, then a stack of
 * shelf rows. Each shelf shows its name, per-shelf counts, product tiles (horizontally scrollable),
 * and an empty state with an inline Add action. Empty shelves remain visible.
 */
@Composable
fun ShelfFrame(
    location: StorageLocation,
    shelves: List<StorageShelf>,
    grouped: Map<String, List<FoodProduct>>,
    soonThresholdDays: Int,
    onProductClick: (String) -> Unit,
    onAddToShelf: (shelfId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = StatusVisuals.locationAccent(location)
    val frameSurface = StatusVisuals.locationSurface(location)
    val orderedShelves = ShelfUtils.shelvesFor(shelves, location)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(frameSurface)
            .border(3.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(10.dp),
    ) {
        // Cooling header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when (location) {
                    StorageLocation.Fridge -> "Fridge — cooling"
                    StorageLocation.Freezer -> "Freezer — frozen"
                    StorageLocation.Pantry -> "Pantry — dry storage"
                },
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .size(width = 14.dp, height = 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.4f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (orderedShelves.isEmpty()) {
            Text(
                "No shelves configured for this area.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        } else {
            orderedShelves.forEachIndexed { index, shelf ->
                ShelfRow(
                    shelf = shelf,
                    products = grouped[shelf.id].orEmpty(),
                    soonThresholdDays = soonThresholdDays,
                    accent = accent,
                    onProductClick = onProductClick,
                    onAddToShelf = { onAddToShelf(shelf.id) },
                )
                if (index != orderedShelves.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }

        // Any unassigned products for this location.
        grouped[ShelfUtils.UNASSIGNED_SHELF_ID]?.takeIf { it.isNotEmpty() }?.let { unassigned ->
            Spacer(Modifier.height(8.dp))
            UnassignedRow(unassigned, soonThresholdDays, accent, onProductClick)
        }
    }
}

@Composable
private fun ShelfRow(
    shelf: StorageShelf,
    products: List<FoodProduct>,
    soonThresholdDays: Int,
    accent: Color,
    onProductClick: (String) -> Unit,
    onAddToShelf: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .padding(8.dp),
    ) {
        // Shelf label + counts
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = shelf.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            ShelfCounts(products, soonThresholdDays)
        }
        // Shelf plank line
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .padding(top = 0.dp)
                .background(accent.copy(alpha = 0.25f)),
        )
        Spacer(Modifier.height(6.dp))

        if (products.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Empty shelf",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AddChip(onClick = onAddToShelf)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                products.forEach { product ->
                    ProductTile(
                        product = product,
                        soonThresholdDays = soonThresholdDays,
                        onClick = { onProductClick(product.id) },
                    )
                }
                AddTile(onClick = onAddToShelf)
            }
        }
    }
}

@Composable
private fun UnassignedRow(
    products: List<FoodProduct>,
    soonThresholdDays: Int,
    accent: Color,
    onProductClick: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .padding(8.dp),
    ) {
        Text(
            "Unassigned",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            products.forEach { product ->
                ProductTile(product, soonThresholdDays, onClick = { onProductClick(product.id) })
            }
        }
    }
}

@Composable
private fun ShelfCounts(products: List<FoodProduct>, soonThresholdDays: Int) {
    var soon = 0
    var expired = 0
    var low = 0
    products.forEach { p ->
        when (StatusUtils.dateStatus(p, soonThresholdDays)) {
            ProductDateStatus.Soon, ProductDateStatus.OpenedSoon -> soon++
            ProductDateStatus.Expired -> expired++
            else -> Unit
        }
        if (StatusUtils.isRunningLow(p)) low++
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        MiniCount("${products.size}", MaterialTheme.colorScheme.onSurfaceVariant, "items")
        if (soon > 0) MiniCount("$soon", SoonAmber, "review soon")
        if (expired > 0) MiniCount("$expired", ExpiredRed, "expiry passed")
        if (low > 0) MiniCount("$low", RunningLowOrange, "running low")
    }
}

@Composable
private fun MiniCount(text: String, color: Color, desc: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
            .semantics { contentDescription = "$text $desc" },
    )
}

@Composable
private fun AddChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .semantics { contentDescription = "Add product to this shelf" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Add Product", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Add product to this shelf" }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Add", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
