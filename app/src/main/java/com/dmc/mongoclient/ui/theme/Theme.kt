package com.dmc.mongoclient.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dmc.mongoclient.data.settings.ThemeMode

@Composable
fun MongoClientTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Default off so the Sextant brand palette wins; users on Android 12+
    // can opt in by passing true if they prefer wallpaper-derived colours.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> SextantDarkColorScheme
        else -> SextantLightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
