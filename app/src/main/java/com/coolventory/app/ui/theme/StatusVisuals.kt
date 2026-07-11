package com.coolventory.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.coolventory.app.model.ProductDateStatus
import com.coolventory.app.model.ProductQuantityStatus
import com.coolventory.app.model.StorageLocation

/** Visual mapping (color + icon + text) for statuses. Color is never the only indicator. */
data class StatusVisual(val color: Color, val icon: ImageVector, val label: String)

object StatusVisuals {

    fun forDateStatus(status: ProductDateStatus): StatusVisual = when (status) {
        ProductDateStatus.Fresh -> StatusVisual(FreshGreen, Icons.Filled.CheckCircle, "Fresh")
        ProductDateStatus.Soon -> StatusVisual(SoonAmber, Icons.Filled.Schedule, "Review soon")
        ProductDateStatus.OpenedSoon -> StatusVisual(SoonAmber, Icons.Filled.Schedule, "Opened — review soon")
        ProductDateStatus.Expired -> StatusVisual(ExpiredRed, Icons.Filled.ErrorOutline, "Expiry date passed")
        ProductDateStatus.NoExpiryDate -> StatusVisual(NoExpiryGray, Icons.Filled.HelpOutline, "No expiry date")
        ProductDateStatus.Used -> StatusVisual(UsedBlueGray, Icons.Filled.CheckCircle, "Used")
        ProductDateStatus.Discarded -> StatusVisual(DiscardedCharcoal, Icons.Filled.DeleteOutline, "Discarded")
    }

    fun forQuantityStatus(status: ProductQuantityStatus): StatusVisual? = when (status) {
        ProductQuantityStatus.RunningLow -> StatusVisual(RunningLowOrange, Icons.Filled.WarningAmber, "Running low")
        ProductQuantityStatus.Empty -> StatusVisual(RunningLowOrange, Icons.Filled.WarningAmber, "Empty")
        ProductQuantityStatus.Available -> null
    }

    /** Accent color for each storage location's frame. */
    fun locationAccent(location: StorageLocation): Color = when (location) {
        StorageLocation.Fridge -> CoolTeal
        StorageLocation.Freezer -> FreezerDeepBlue
        StorageLocation.Pantry -> PantryBrown
    }

    fun locationSurface(location: StorageLocation): Color = when (location) {
        StorageLocation.Fridge -> FrostMint
        StorageLocation.Freezer -> PaleIce
        StorageLocation.Pantry -> PantryCream
    }
}
