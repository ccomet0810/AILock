package com.ccomet.ailock.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import com.ccomet.ailock.ui.theme.AILockLayout

const val AILockHeaderCollapseDistancePx = 220f
const val AILockHeaderCollapseSensitivity = 1f
const val AILockHeaderExpandResistance = 0.42f
const val AILockHeaderSnapThresholdPx = 84f
const val AILockHeaderSnapVelocityPx = 260f

class AILockHeaderMotionState internal constructor(
    private val targetOffsetState: androidx.compose.runtime.MutableFloatState,
    private val settlingState: MutableState<Boolean>,
    val offsetPx: Float,
) {
    val collapseFraction: Float
        get() = (offsetPx / AILockHeaderCollapseDistancePx).coerceIn(0f, 1f)

    val currentHeaderHeight: Dp
        get() = AILockLayout.expandedHeaderHeight -
            (AILockLayout.expandedHeaderHeight - AILockLayout.collapsedHeaderHeight) * collapseFraction

    private val targetOffsetPx: Float
        get() = targetOffsetState.floatValue

    fun onDragDelta(delta: Float) {
        settlingState.value = false
        val factor = if (delta < 0f) AILockHeaderCollapseSensitivity else AILockHeaderExpandResistance
        targetOffsetState.floatValue = (targetOffsetPx - delta * factor)
            .coerceIn(0f, AILockHeaderCollapseDistancePx)
    }

    fun settleAfterDrag(velocity: Float) {
        settlingState.value = true
        targetOffsetState.floatValue = snapTarget(velocityY = velocity, atTop = true) ?: targetOffsetPx
    }

    fun onNestedPreScroll(dy: Float, atTop: Boolean): Boolean {
        if (dy < 0f && targetOffsetPx < AILockHeaderCollapseDistancePx) {
            settlingState.value = false
            targetOffsetState.floatValue = (targetOffsetPx - dy * AILockHeaderCollapseSensitivity)
                .coerceIn(0f, AILockHeaderCollapseDistancePx)
            return true
        }

        if (dy > 0f && targetOffsetPx > 0f && atTop) {
            settlingState.value = false
            targetOffsetState.floatValue = (targetOffsetPx - dy * AILockHeaderExpandResistance)
                .coerceIn(0f, AILockHeaderCollapseDistancePx)
            return true
        }

        return false
    }

    fun settleAfterNestedFling(velocityY: Float, atTop: Boolean): Boolean {
        val target = snapTarget(velocityY = velocityY, atTop = atTop) ?: return false
        settlingState.value = true
        targetOffsetState.floatValue = target
        return true
    }

    private fun snapTarget(velocityY: Float, atTop: Boolean): Float? =
        when {
            velocityY < -AILockHeaderSnapVelocityPx && targetOffsetPx < AILockHeaderCollapseDistancePx -> {
                AILockHeaderCollapseDistancePx
            }
            velocityY > AILockHeaderSnapVelocityPx && atTop && targetOffsetPx > 0f -> {
                0f
            }
            atTop && targetOffsetPx in 0f..AILockHeaderCollapseDistancePx -> {
                if (targetOffsetPx > AILockHeaderSnapThresholdPx) AILockHeaderCollapseDistancePx else 0f
            }
            else -> null
        }
}

@Composable
fun rememberAILockHeaderMotionState(label: String): AILockHeaderMotionState {
    val targetOffsetState = rememberSaveable { mutableFloatStateOf(0f) }
    val settlingState = remember { mutableStateOf(false) }
    val animatedOffsetPx by animateFloatAsState(
        targetValue = targetOffsetState.floatValue,
        animationSpec = if (settlingState.value) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        } else {
            snap()
        },
        label = label,
    )

    return AILockHeaderMotionState(
        targetOffsetState = targetOffsetState,
        settlingState = settlingState,
        offsetPx = animatedOffsetPx,
    )
}

@Composable
fun rememberAILockHeaderNestedScrollConnection(
    headerMotion: AILockHeaderMotionState,
    listState: LazyListState,
): NestedScrollConnection =
    remember(headerMotion, listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                return if (headerMotion.onNestedPreScroll(dy = available.y, atTop = atTop)) {
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                return if (headerMotion.settleAfterNestedFling(velocityY = available.y, atTop = atTop)) {
                    available
                } else {
                    Velocity.Zero
                }
            }
        }
    }
