package com.dmc.mongoclient.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Blue-grey brand palette (Material 3 tokens, hand-tuned around Material's
// Blue Grey 700 / 200 anchors).

private val BrandPrimaryLight = Color(0xFF455A64)         // Blue Grey 700
private val BrandPrimaryContainerLight = Color(0xFFD9E3EA)
private val BrandSecondaryLight = Color(0xFF546E7A)
private val BrandSecondaryContainerLight = Color(0xFFDDE6EB)
private val BrandTertiaryLight = Color(0xFF00838F)        // Teal accent for highlights
private val BrandTertiaryContainerLight = Color(0xFFB2EBF2)
private val SurfaceLight = Color(0xFFF6F8FA)
private val SurfaceVariantLight = Color(0xFFDFE3E7)
private val OnSurfaceLight = Color(0xFF161A1D)

private val BrandPrimaryDark = Color(0xFFB0BEC5)          // Blue Grey 200
private val BrandPrimaryContainerDark = Color(0xFF37474F)
private val BrandSecondaryDark = Color(0xFFB0C0CB)
private val BrandSecondaryContainerDark = Color(0xFF3D4F58)
private val BrandTertiaryDark = Color(0xFF4DD0E1)
private val BrandTertiaryContainerDark = Color(0xFF004F57)
private val SurfaceDark = Color(0xFF161A1D)
private val SurfaceVariantDark = Color(0xFF42474B)
private val OnSurfaceDark = Color(0xFFE1E3E6)

val SextantLightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = Color(0xFF1B2A33),
    secondary = BrandSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondaryContainerLight,
    onSecondaryContainer = Color(0xFF1B2A33),
    tertiary = BrandTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = BrandTertiaryContainerLight,
    onTertiaryContainer = Color(0xFF002B33),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF43474B),
    outline = Color(0xFF73777C),
    outlineVariant = Color(0xFFC3C7CB),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

val SextantDarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF1B2A33),
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFCFD8DC),
    secondary = BrandSecondaryDark,
    onSecondary = Color(0xFF1B2A33),
    secondaryContainer = BrandSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFCCD9DE),
    tertiary = BrandTertiaryDark,
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = BrandTertiaryContainerDark,
    onTertiaryContainer = Color(0xFFB2EBF2),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC3C7CB),
    outline = Color(0xFF8D9296),
    outlineVariant = Color(0xFF42474B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)
