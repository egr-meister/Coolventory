package com.coolventory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.StorageShelf
import com.coolventory.app.ui.theme.StatusVisuals
import com.coolventory.app.util.QuantityUtils
import com.coolventory.app.util.ShelfUtils
import com.coolventory.app.util.StatusUtils

/**
 * Shelf-flavored list row (a colored status "spine" on the left, like a labeled container edge on a
 * shelf) — intentionally not a generic rounded card.
 */
@Composable
fun ProductRow(
    product: FoodProduct,
    shelves: List<StorageShelf>,
    soonThresholdDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = StatusUtils.dateStatus(product, soonThresholdDays)
    val visual = StatusVisuals.forDateStatus(status)
    val shelfName = ShelfUtils.shelfName(shelves, product.shelfId)
    val qtyText = "${QuantityUtils.format(product.quantity, product.quantityUnit)} ${product.unitDisplay()}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        // Status spine
        Box(
            Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(visual.color),
        )
        Column(Modifier.weight(1f).padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text(
                    text = product.name.ifBlank { "Unnamed product" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(status = status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${product.storageLocation.label} · $shelfName · ${product.categoryDisplay()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                InfoTag(qtyText)
                Text(
                    text = StatusUtils.reviewSummary(product, soonThresholdDays),
                    style = MaterialTheme.typography.labelSmall,
                    color = visual.color,
                )
                if (StatusUtils.isRunningLow(product)) {
                    QuantityStatusChip(status = StatusUtils.quantityStatus(product), compact = false)
                }
            }
        }
    }
}
