package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SparkDarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = MidnightBlack,
    primaryContainer = Color(0xFF2B261C),
    onPrimaryContainer = SoftGold,
    secondary = SoftGold,
    onSecondary = MidnightBlack,
    secondaryContainer = ElevatedCharcoal,
    onSecondaryContainer = IceWhite,
    tertiary = DeepBronzeGold,
    onTertiary = IceWhite,
    background = MidnightBlack,
    onBackground = IceWhite,
    surface = SecondarySlate,
    onSurface = IceWhite,
    surfaceVariant = ElevatedCharcoal,
    onSurfaceVariant = SecondaryIce,
    outline = SteelBorder,
    outlineVariant = Color(0xFF26323D),
    error = ErrorRed,
    onError = IceWhite
)

private val SparkLightColorScheme = lightColorScheme(
    primary = DeepBronzeGold,
    onPrimary = IceWhite,
    primaryContainer = Color(0xFFF6EBD2),
    onPrimaryContainer = MidnightBlack,
    secondary = MidnightBlack,
    onSecondary = IceWhite,
    secondaryContainer = LightCardVariant,
    onSecondaryContainer = LightInk,
    tertiary = ChampagneGold,
    onTertiary = MidnightBlack,
    background = LightCanvas,
    onBackground = LightInk,
    surface = LightCardSurface,
    onSurface = LightInk,
    surfaceVariant = LightCardVariant,
    onSurfaceVariant = LightSecondaryInk,
    outline = LightDivider,
    outlineVariant = Color(0xFFE2E8F0),
    error = ErrorRed,
    onError = Color.White
)

val SparkShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SparkAiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SparkDarkColorScheme else SparkLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SparkTypography,
        shapes = SparkShapes,
        content = content
    )
}
