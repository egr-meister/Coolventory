package com.coolventory.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = CoolTeal,
    onPrimary = Color.White,
    primaryContainer = FrostMint,
    onPrimaryContainer = DeepTeal,
    secondary = DeepTeal,
    onSecondary = Color.White,
    secondaryContainer = PaleIce,
    onSecondaryContainer = FreezerDeepBlue,
    tertiary = PantryBrown,
    onTertiary = Color.White,
    tertiaryContainer = PantryCream,
    onTertiaryContainer = PantryBrown,
    background = AppBackground,
    onBackground = DeepText,
    surface = Surface,
    onSurface = DeepText,
    surfaceVariant = FrostMint,
    onSurfaceVariant = SecondaryText,
    outline = ShelfFrame,
    outlineVariant = Divider,
    error = ExpiredRed,
    onError = Color.White,
)

// A restrained dark scheme (portrait, calm). Colors remain distinguishable and accessible.
private val DarkColors = darkColorScheme(
    primary = IceBlue,
    onPrimary = Color(0xFF10262B),
    primaryContainer = DeepTeal,
    onPrimaryContainer = FrostMint,
    secondary = IceBlue,
    onSecondary = Color(0xFF10262B),
    tertiary = PantrySand,
    onTertiary = Color(0xFF2A2114),
    background = Color(0xFF12191A),
    onBackground = Color(0xFFE7EDEC),
    surface = Color(0xFF1A2223),
    onSurface = Color(0xFFE7EDEC),
    surfaceVariant = Color(0xFF243031),
    onSurfaceVariant = Color(0xFFB6C0C0),
    outline = Color(0xFF3A4849),
    outlineVariant = Color(0xFF2C3838),
    error = Color(0xFFE08D8B),
    onError = Color(0xFF2A1211),
)

private val CoolventoryTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun CoolventoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = CoolventoryTypography,
        content = content,
    )
}
