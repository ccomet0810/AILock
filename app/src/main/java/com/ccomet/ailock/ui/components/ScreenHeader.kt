package com.ccomet.ailock.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockSpacing

@Composable
fun CollapsingScreenHeader(
    title: String,
    subtitle: String,
    collapsed: Boolean,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    val height = if (collapsed) AILockLayout.collapsedHeaderHeight else AILockLayout.expandedHeaderHeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = AILockSpacing.screenHorizontal),
    ) {
        if (actions != null) {
            Box(
                modifier = Modifier
                    .align(if (collapsed) Alignment.CenterEnd else Alignment.TopEnd)
                    .padding(top = if (collapsed) 0.dp else 12.dp),
            ) {
                actions()
            }
        }
        Column(
            modifier = Modifier
                .align(if (collapsed) Alignment.CenterStart else Alignment.BottomStart)
                .padding(
                    top = 0.dp,
                    bottom = if (collapsed) 0.dp else 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        ) {
            Text(
                text = title,
                color = AppTextStrong,
                style = if (collapsed) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.displaySmall
                },
                fontWeight = FontWeight.ExtraBold,
            )
            AnimatedVisibility(visible = !collapsed) {
                Text(
                    text = subtitle,
                    color = AppTextStrong,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = AppBorder,
        )
    }
}

@Composable
fun StickyCollapsingScreenHeader(
    title: String,
    subtitle: String,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    val progress = collapseFraction.coerceIn(0f, 1f)
    val headerHeight = lerpDp(AILockLayout.expandedHeaderHeight, AILockLayout.collapsedHeaderHeight, progress)
    val titleTop = lerpDp(78.dp, 18.dp, progress)
    val titleSize = lerpFloat(34f, 24f, progress).sp
    val titleLineHeight = lerpFloat(42f, 28f, progress).sp
    val subtitleTop = lerpDp(126.dp, 48.dp, progress)
    val subtitleAlpha = (1f - progress * 1.7f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
            .padding(horizontal = AILockSpacing.screenHorizontal),
    ) {
        Text(
            text = title,
            color = AppTextStrong,
            fontSize = titleSize,
            lineHeight = titleLineHeight,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = titleTop),
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = AppTextStrong,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .offset(y = subtitleTop)
                    .graphicsLayer { alpha = subtitleAlpha },
            )
        }
        if (actions != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .height(AILockLayout.collapsedHeaderHeight),
                contentAlignment = Alignment.CenterEnd,
            ) {
                actions()
            }
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = AppBorder.copy(alpha = progress),
        )
    }
}

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp =
    start + (stop - start) * fraction

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

@Composable
fun PinnedHeaderActionSlot(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AILockLayout.collapsedHeaderHeight)
            .padding(horizontal = AILockSpacing.screenHorizontal),
        contentAlignment = Alignment.CenterEnd,
    ) {
        content()
    }
}
