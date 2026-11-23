package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Official Spark AI Black & Champagne Gold Palette (Zero Purple/Violet)
val MidnightBlack = Color(0xFF101820)
val SecondarySlate = Color(0xFF19232D)
val ElevatedCharcoal = Color(0xFF212E3B)
val DeepObsidian = Color(0xFF0B1117)

val ChampagneGold = Color(0xFFD6AD60)
val SoftGold = Color(0xFFE8C77E)
val DeepBronzeGold = Color(0xFFA8833B)

val IceWhite = Color(0xFFF5F7FA)
val SecondaryIce = Color(0xFFAAB4C0)
val SteelBorder = Color(0xFF34404B)

val SuccessGreen = Color(0xFF22C55E)
val AmberGold = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Light Cinema Mode Tokens (Warm Ivory + Midnight Ink + Champagne Gold)
val LightCanvas = Color(0xFFF5F7FA)
val LightCardSurface = Color(0xFFFFFFFF)
val LightCardVariant = Color(0xFFE9EEF3)
val LightInk = Color(0xFF101820)
val LightSecondaryInk = Color(0xFF4B5866)
val LightDivider = Color(0xFFD0D8E0)

object SparkBrushes {
    val CosmicBackground = Brush.verticalGradient(
        colors = listOf(DeepObsidian, MidnightBlack, SecondarySlate)
    )
    val LightBackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE6ECF2))
    )
    val GoldGradient = Brush.horizontalGradient(
        colors = listOf(DeepBronzeGold, ChampagneGold, SoftGold)
    )
    val HeroCardGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF1A2530), Color(0xFF141D26), Color(0xFF242B35))
    )
    val SubtleGoldSheen = Brush.linearGradient(
        colors = listOf(Color(0x33D6AD60), Color(0x14E8C77E), Color(0x00101820))
    )
}
