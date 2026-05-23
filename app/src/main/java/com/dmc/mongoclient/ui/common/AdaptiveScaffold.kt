package com.dmc.mongoclient.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * For Phase 1, the adaptive scaffold only caps width on tablet/wide layouts.
 * Phase 2 will extend it into a three-pane container.
 */
@Composable
fun AdaptiveScreenContainer(
    modifier: Modifier = Modifier,
    adaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    content: @Composable () -> Unit,
) {
    val widthClass = adaptiveInfo.windowSizeClass.windowWidthSizeClass
    val maxWidth = when (widthClass) {
        WindowWidthSizeClass.COMPACT -> Modifier.fillMaxWidth()
        else -> Modifier.widthIn(max = 720.dp)
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = maxWidth.fillMaxSize(), content = { content() })
    }
}
