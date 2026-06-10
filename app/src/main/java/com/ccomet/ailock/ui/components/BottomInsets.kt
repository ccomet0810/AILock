package com.ccomet.ailock.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.ui.theme.AILockSpacing

@Composable
internal fun balancedNavigationBottomPadding(): Dp {
    val density = LocalDensity.current
    val navigationBottomPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    return (AILockSpacing.screenHorizontal - navigationBottomPadding).coerceAtLeast(0.dp)
}

@Composable
internal fun balancedSafeDrawingBottomPadding(): Dp {
    val density = LocalDensity.current
    val safeDrawingBottomPadding = with(density) {
        WindowInsets.safeDrawing.getBottom(this).toDp()
    }
    return (AILockSpacing.screenHorizontal - safeDrawingBottomPadding).coerceAtLeast(0.dp)
}
