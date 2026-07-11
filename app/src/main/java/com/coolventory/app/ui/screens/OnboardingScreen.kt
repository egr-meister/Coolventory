package com.coolventory.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolventory.app.ui.components.DisclaimerCard
import com.coolventory.app.ui.components.MANUAL_DISCLAIMER
import com.coolventory.app.ui.theme.CoolTeal
import com.coolventory.app.ui.theme.ExpiredRed
import com.coolventory.app.ui.theme.FreshGreen
import com.coolventory.app.ui.theme.SoonAmber

@Composable
fun OnboardingScreen(
    onAddFirst: () -> Unit,
    onExplore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        ShelfIllustration()
        Spacer(Modifier.height(20.dp))
        Text(
            "Coolventory",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "See your storage as organized shelves.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        OnboardingPoint("Manual product entry", "Add products manually and place them in the fridge, freezer, or pantry.")
        OnboardingPoint("Fridge, freezer & pantry", "Each area is shown as an open shelf dashboard you can browse.")
        OnboardingPoint("Date-based statuses", "Review products by the dates you enter: Fresh, Review Soon, Expiry Date Passed, No Expiry Date.")
        OnboardingPoint("Quantity & Buy Again", "Track quantity, mark running low, and build a Buy Again list.")
        OnboardingPoint("Used-product history", "Keep a neutral record of what was used or discarded.")
        OnboardingPoint("In-app reminders", "Reminders appear inside the app only — no push notifications, no background work.")
        OnboardingPoint("Offline storage", "Your inventory stays on this device. No account, no cloud, no internet.")
        OnboardingPoint("No scanner or camera", "No barcode scanner, camera, receipt scanning, or food recognition.")

        Spacer(Modifier.height(12.dp))
        DisclaimerCard(text = MANUAL_DISCLAIMER)
        Spacer(Modifier.height(8.dp))
        DisclaimerCard(
            text = "Coolventory does not inspect food or provide dietary, nutritional, medical, or food safety advice.",
        )

        Spacer(Modifier.height(20.dp))
        Button(onClick = onAddFirst, modifier = Modifier.fillMaxWidth()) {
            Text("Add First Product")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) {
            Text("Explore Shelves")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingPoint(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Simplified open-shelf illustration (no mascot, no photo) built from Compose shapes. */
@Composable
private fun ShelfIllustration() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(3.dp, CoolTeal.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IllustrationShelf(listOf(FreshGreen, SoonAmber))
        IllustrationShelf(listOf(ExpiredRed, FreshGreen, CoolTeal))
    }
}

@Composable
private fun IllustrationShelf(colors: List<androidx.compose.ui.graphics.Color>) {
    Column(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { c ->
                Box(
                    Modifier
                        .size(width = 26.dp, height = 32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(c.copy(alpha = 0.35f))
                        .border(1.5.dp, c.copy(alpha = 0.75f), RoundedCornerShape(4.dp)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(CoolTeal.copy(alpha = 0.4f)),
        )
    }
}
