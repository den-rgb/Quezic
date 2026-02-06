package com.quezic.ui.theme

import androidx.compose.ui.graphics.Color

// Quezic Theme - Black, Green, Purple with gradients

// Primary - Vibrant Green accent (main action color)
val Primary = Color(0xFF00E676)
val OnPrimary = Color(0xFF003300)
val PrimaryContainer = Color(0xFF004D1A)
val OnPrimaryContainer = Color(0xFFB9F6CA)

// Secondary - Vibrant Purple accent
val Secondary = Color(0xFFBB86FC)
val OnSecondary = Color(0xFF1F0033)
val SecondaryContainer = Color(0xFF3D0066)
val OnSecondaryContainer = Color(0xFFE8D5FF)

// Tertiary - Lighter purple/violet
val Tertiary = Color(0xFF7C4DFF)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFF2D1066)
val OnTertiaryContainer = Color(0xFFD4BBFF)

// Error
val Error = Color(0xFFFF453A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFF3D0007)
val OnErrorContainer = Color(0xFFFFD9D9)

// Light Theme (we'll make this dark-ish too for consistency)
val BackgroundLight = Color(0xFF0D0D0D)
val OnBackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFF1A1A1A)
val OnSurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFF252525)
val OnSurfaceVariantLight = Color(0xFFAEAEB2)
val OutlineLight = Color(0xFF3A3A3A)

// Dark Theme - True black for OLED
val PrimaryDark = Color(0xFF00E676)
val OnPrimaryDark = Color(0xFF003300)
val PrimaryContainerDark = Color(0xFF00803D)
val OnPrimaryContainerDark = Color(0xFFB9F6CA)

val SecondaryDark = Color(0xFFCF9FFF)
val OnSecondaryDark = Color(0xFF1F0033)
val SecondaryContainerDark = Color(0xFF4A0080)
val OnSecondaryContainerDark = Color(0xFFE8D5FF)

val TertiaryDark = Color(0xFF9E7CFF)
val OnTertiaryDark = Color(0xFFFFFFFF)
val TertiaryContainerDark = Color(0xFF3D1A80)
val OnTertiaryContainerDark = Color(0xFFD4BBFF)

val BackgroundDark = Color(0xFF000000)
val OnBackgroundDark = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF0D0D0D)
val OnSurfaceDark = Color(0xFFFFFFFF)
val SurfaceVariantDark = Color(0xFF1A1A1A)
val OnSurfaceVariantDark = Color(0xFF8E8E93)
val OutlineDark = Color(0xFF3A3A3A)

// Player gradient colors - Green to Purple
val PlayerGradientStart = Color(0xFF0D2818)
val PlayerGradientMiddle = Color(0xFF1A0D26)
val PlayerGradientEnd = Color(0xFF000000)

// Card colors
val CardDark = Color(0xFF0D0D0D)
val CardElevated = Color(0xFF1A1A1A)

// App Accent Colors (use these throughout the app)
val AccentGreen = Color(0xFF00E676)
val AccentGreenLight = Color(0xFF69F0AE)
val AccentGreenDark = Color(0xFF00C853)
val AccentPurple = Color(0xFFBB86FC)
val AccentPurpleLight = Color(0xFFD4BBFF)
val AccentPurpleDark = Color(0xFF7C4DFF)

// Gradient definitions
val GradientGreenPurple = listOf(AccentGreen, AccentPurple)
val GradientPurpleGreen = listOf(AccentPurple, AccentGreen)
val GradientGreenDark = listOf(AccentGreen, Color(0xFF004D1A))
val GradientPurpleDark = listOf(AccentPurple, Color(0xFF3D0066))

// iOS System Colors (keeping some for compatibility)
val SystemBlue = Color(0xFF0A84FF)
val SystemGreen = Color(0xFF00E676)  // Updated to match AccentGreen
val SystemIndigo = Color(0xFF7C4DFF)  // Updated to match purple theme
val SystemOrange = Color(0xFFFF9F0A)
val SystemPink = Color(0xFFBB86FC)  // Now maps to purple for theme consistency
val SystemPurple = Color(0xFFBB86FC)
val SystemRed = Color(0xFFFF453A)
val SystemTeal = Color(0xFF64D2FF)
val SystemYellow = Color(0xFFFFD60A)

// Grayscale
val Gray1 = Color(0xFF8E8E93)
val Gray2 = Color(0xFF636366)
val Gray3 = Color(0xFF3A3A3A)
val Gray4 = Color(0xFF2A2A2A)
val Gray5 = Color(0xFF1A1A1A)
val Gray6 = Color(0xFF0D0D0D)
