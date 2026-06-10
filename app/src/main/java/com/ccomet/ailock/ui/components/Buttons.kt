package com.ccomet.ailock.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppBackground
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.PandaOrange


@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(AILockLayout.buttonHeight),
        shape = AILockShape.control,
        colors = ButtonDefaults.buttonColors(
            containerColor = PandaOrange,
            contentColor = AppSurface,
            disabledContainerColor = AppSurfaceMuted.copy(alpha = 0.55f),
            disabledContentColor = AppTextSubtle,
        ),
        border = BorderStroke(1.dp, PandaOrange),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = AILockSpacing.buttonContentHorizontal),
    ) {
        if (icon != null || iconRes != null) {
            if (iconRes != null) {
                Icon(painterResource(id = iconRes), contentDescription = null)
            } else if (icon != null) {
                Icon(icon, contentDescription = null)
            }
            Spacer(Modifier.width(AILockSpacing.buttonIconGap))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(AILockLayout.buttonHeight),
        shape = AILockShape.control,
        border = BorderStroke(1.dp, AppBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppSurface,
            contentColor = AppTextStrong,
        ),
        contentPadding = PaddingValues(horizontal = AILockSpacing.buttonContentHorizontal),
    ) {
        if (icon != null || iconRes != null) {
            if (iconRes != null) {
                Icon(painterResource(id = iconRes), contentDescription = null)
            } else if (icon != null) {
                Icon(icon, contentDescription = null)
            }
            Spacer(Modifier.width(AILockSpacing.buttonIconGap))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FloatingBottomActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
    animateOnMount: Boolean = true,
) {
    var visible by remember { mutableStateOf(!animateOnMount) }
    LaunchedEffect(animateOnMount) {
        if (animateOnMount) visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(170),
            initialOffsetY = { it / 2 },
        ) + fadeIn(animationSpec = tween(120)),
        modifier = modifier,
    ) {
        BottomActionBar {
            PrimaryButton(
                text = text,
                onClick = onClick,
                enabled = enabled,
                icon = icon,
                iconRes = iconRes,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val bottomPadding = balancedSafeDrawingBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AILockLayout.bottomActionAreaHeight)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.42f to AppBackground.copy(alpha = 0.68f),
                    1f to AppBackground,
                ),
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = AILockSpacing.screenHorizontal)
                .padding(top = AILockSpacing.bottomChromeTopPadding, bottom = bottomPadding)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
