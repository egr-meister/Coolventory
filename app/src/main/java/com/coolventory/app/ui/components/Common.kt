package com.coolventory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.model.ProductQuantityStatus
import com.coolventory.app.ui.theme.StatusVisual
import com.coolventory.app.ui.theme.StatusVisuals

/** Manual tracking disclaimer, reused throughout the app. */
const val MANUAL_DISCLAIMER =
    "Coolventory is a manual food inventory organizer. Product names, quantities, dates, " +
        "locations, statuses, and notes are entered by the user. The app does not inspect food, " +
        "detect spoilage, guarantee food safety, or provide dietary, nutritional, or medical advice."

/** Expiry disclaimer, reused on expiry-related screens and Settings. */
const val EXPIRY_DISCLAIMER =
    "Date-based statuses are organizational reminders only. Always follow package instructions, " +
        "storage guidance, and your own judgment. When in doubt about food condition or safety, " +
        "discard it or consult an appropriate professional source."

@Composable
fun DisclaimerCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Compact status pill combining color, icon and text (never color alone). */
@Composable
fun StatusChip(
    status: ProductDateStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val visual = StatusVisuals.forDateStatus(status)
    StatusPill(visual, modifier, compact)
}

@Composable
fun QuantityStatusChip(
    status: ProductQuantityStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val visual = StatusVisuals.forQuantityStatus(status) ?: return
    StatusPill(visual, modifier, compact)
}

@Composable
private fun StatusPill(
    visual: StatusVisual,
    modifier: Modifier = Modifier,
    compact: Boolean,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(visual.color.copy(alpha = 0.16f))
            .border(1.dp, visual.color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 3.dp)
            .semantics { contentDescription = "Status: ${visual.label}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = null,
            tint = visual.color,
            modifier = Modifier.size(if (compact) 12.dp else 14.dp),
        )
        if (!compact) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = visual.label,
                style = MaterialTheme.typography.labelSmall,
                color = visual.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Small neutral tag, e.g. quantity "2 Pieces". */
@Composable
fun InfoTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        trailing?.invoke()
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.size(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.size(12.dp))
            action()
        }
    }
}

/** A single count chip used in the status rail. */
@Composable
fun CountChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $count" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

/** Decorative helper — hides a purely decorative box from screen readers. */
fun Modifier.decorative(): Modifier = this.clearAndSetSemantics { }

@Composable
fun DecorativeBox(color: Color, modifier: Modifier) {
    Box(modifier = modifier.background(color).decorative())
}
