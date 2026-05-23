package com.dmc.mongoclient.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Per-token-type colours for JSON syntax highlighting. Distinct hues from the
 * blue-grey brand palette so tokens are immediately distinguishable; tuned to
 * meet ~4.5:1 contrast against the surface color in both modes.
 */
@Immutable
data class JsonSyntaxColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val keyword: Color,
)

private val LightJsonColors = JsonSyntaxColors(
    key = Color(0xFF1565C0),     // Blue 800 — keys stand out against blue-grey surfaces
    string = Color(0xFF00695C),  // Teal 800
    number = Color(0xFFB85E00),  // Amber 900
    keyword = Color(0xFF6A1B9A), // Purple 800 — for true/false/null
)

private val DarkJsonColors = JsonSyntaxColors(
    key = Color(0xFF90CAF9),     // Blue 200
    string = Color(0xFF80CBC4),  // Teal 200
    number = Color(0xFFFFCC80),  // Amber 200
    keyword = Color(0xFFCE93D8), // Purple 200
)

/** Picks the dark or light palette based on the current surface luminance. */
@Composable
fun rememberJsonSyntaxColors(): JsonSyntaxColors {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return remember(dark) { if (dark) DarkJsonColors else LightJsonColors }
}
