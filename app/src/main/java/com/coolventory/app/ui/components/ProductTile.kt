package com.coolventory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coolventory.app.model.FoodProduct
import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.ui.theme.StatusVisuals
import com.coolventory.app.util.QuantityUtils
import com.coolventory.app.util.StatusUtils

/**
 * Compact "container on a shelf" tile. Rendered as an abstract jar/box shape (a rounded cap over a
 * body) plus a label. Status is shown by color AND icon AND text so color is never the only cue.
 */
@Composable
fun ProductTile(
    product: FoodProduct,
    soonThresholdDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 132.dp,
) {
    val status = StatusUtils.dateStatus(product, soonThresholdDays)
    val visual = StatusVisuals.forDateStatus(status)
    val runningLow = StatusUtils.isRunningLow(product)

    val expiryLabel = when (status) {
        ProductDateStatus.NoExpiryDate -> "No expiry date"
        else -> StatusUtils.reviewSummary(product, soonThresholdDays)
    }
    val qtyText = "${QuantityUtils.format(product.quantity, product.quantityUnit)} ${product.unitDisplay()}"

    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(product.name.ifBlank { "Unnamed product" })
                    append(", $qtyText")
                    append(", ${visual.label}")
                    append(", $expiryLabel")
                    if (runningLow) append(", running low")
                    if (product.buyAgain) append(", on Buy Again")
                }
            }
            .padding(8.dp),
    ) {
        // Abstract container: cap + body colored by status.
        Row(verticalAlignment = Alignment.CenterVertically) {
            AbstractContainer(color = visual.color)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = product.name.ifBlank { "Unnamed" },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = qtyText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (product.buyAgain) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.color,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = expiryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = visual.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (runningLow) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Running low",
                style = MaterialTheme.typography.labelSmall,
                color = com.coolventory.app.ui.theme.RunningLowOrange,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Small abstract "jar/box" glyph drawn with two shapes. */
@Composable
private fun AbstractContainer(color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .width(14.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(color.copy(alpha = 0.85f)),
        )
        Box(
            Modifier
                .size(width = 22.dp, height = 24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.25f))
                .border(1.5.dp, color.copy(alpha = 0.7f), RoundedCornerShape(4.dp)),
        )
    }
}
