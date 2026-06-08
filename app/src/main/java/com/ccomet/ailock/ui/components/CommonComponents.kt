package com.ccomet.ailock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.data.model.PermissionState
import com.ccomet.ailock.ui.theme.AppBackground
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppBorderStrong
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.PandaCream
import com.ccomet.ailock.ui.theme.PandaOrange
import com.ccomet.ailock.ui.theme.SoftGreenContainer
import com.ccomet.ailock.ui.theme.SoftRedContainer
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.util.TimeUtils

@Composable
fun RedPandaMascot(
    emotion: PandaEmotion,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.red_panda),
            contentDescription = emotionLabel(emotion),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PandaSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    emotion: PandaEmotion = PandaEmotion.DEFAULT,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpeechBubbleCard(text = text, modifier = Modifier.fillMaxWidth())
        RedPandaMascot(
            emotion = emotion,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun SpeechBubbleCard(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppSurface,
            shape = AILockShape.card,
            border = BorderStroke(1.dp, AppBorder),
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTextStrong,
                    )
                }
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (title == null) AppTextStrong else AppTextSubtle,
                    )
                }
            }
        }
        Canvas(
            modifier = Modifier
                .offset(y = (-1).dp)
                .size(width = 22.dp, height = 12.dp),
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width / 2f, size.height)
                lineTo(size.width, 0f)
                close()
            }
            val stroke = 1.dp.toPx()
            val halfStroke = stroke / 2f
            drawPath(path, AppSurface)
            drawLine(
                color = AppBorder,
                start = Offset(halfStroke, 0f),
                end = Offset(size.width / 2f, size.height - halfStroke),
                strokeWidth = stroke,
            )
            drawLine(
                color = AppBorder,
                start = Offset(size.width - halfStroke, 0f),
                end = Offset(size.width / 2f, size.height - halfStroke),
                strokeWidth = stroke,
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
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
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
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
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = AILockShape.control,
        border = BorderStroke(1.dp, AppBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppSurface,
            contentColor = AppTextStrong,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
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
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.42f to AppBackground.copy(alpha = 0.68f),
                    1f to AppBackground,
                ),
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth()
                .height(64.dp)
                .clip(AILockShape.card)
                .background(AppSurface)
                .border(1.dp, AppBorderStrong, AILockShape.card)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) AppTextStrong else AppTextSubtle.copy(alpha = 0.45f),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) AppTextStrong else AppTextSubtle.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun FloatingBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(AILockShape.card)
            .background(AppSurfaceMuted)
            .border(1.dp, AppBorderStrong, AILockShape.card),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                BottomItem("home", "홈", Icons.Outlined.Home, Icons.Default.Home),
                BottomItem("records", "기록", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
                BottomItem("restrictions", "제한", Icons.Outlined.Lock, Icons.Default.Lock),
                BottomItem("settings", "설정", Icons.Outlined.Settings, Icons.Default.Settings),
            ).forEach { item ->
                BottomNavButton(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavButton(
    item: BottomItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(52.dp)
            .clip(AILockShape.control)
            .background(AppSurfaceMuted)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            modifier = Modifier.size(22.dp),
            tint = if (selected) PandaOrange else AppTextSubtle,
        )
        Text(
            text = item.label,
            modifier = Modifier.padding(top = 2.dp),
            color = if (selected) AppTextStrong else AppTextSubtle,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    icon: ImageVector,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !granted, onClick = onOpenSettings)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = if (granted) SoftGreenContainer else SoftRedContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SecondaryButton(
            text = if (granted) "허용됨" else "설정",
            onClick = onOpenSettings,
            enabled = !granted,
            modifier = Modifier.width(82.dp),
        )
    }
}

@Composable
fun PermissionCards(
    permissionState: PermissionState,
    onUsage: () -> Unit,
    onOverlay: () -> Unit,
    onAccessibility: () -> Unit,
    onNotification: () -> Unit,
    onBattery: () -> Unit = {},
) {
    val rows = listOf(
        PermissionRow("사용 기록 접근", "현재 사용량과 제한 시간 계산", permissionState.hasUsageAccess, Icons.Default.History, onUsage),
        PermissionRow("접근성 서비스", "제한 앱 실행 감지와 홈 이동 유도", permissionState.isAccessibilityEnabled, Icons.Default.Accessibility, onAccessibility),
        PermissionRow("다른 앱 위에 표시", "제한 앱 위 안내 화면 표시", permissionState.canDrawOverlays, Icons.Default.Layers, onOverlay),
        PermissionRow("배터리 최적화 제외", "백그라운드 감지와 타이머 유지", permissionState.isIgnoringBatteryOptimizations, Icons.Default.BatteryFull, onBattery),
        PermissionRow("알림 권한", "종료 전 안내와 약속 알림", permissionState.hasNotificationPermission, Icons.Default.Notifications, onNotification),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AILockShape.card,
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Column {
            rows.forEachIndexed { index, row ->
                PermissionCard(row.title, row.description, row.granted, row.icon, row.onClick)
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = AppBorder)
                }
            }
        }
    }
}

private data class PermissionRow(
    val title: String,
    val description: String,
    val granted: Boolean,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun StatusPill(text: String, positive: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(top = 6.dp),
        shape = AILockShape.pill,
        color = if (positive) SoftGreenContainer else SoftRedContainer,
        border = BorderStroke(1.dp, if (positive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.error.copy(alpha = 0.28f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun AppUsageBar(label: String, minutes: Int, maxMinutes: Int, modifier: Modifier = Modifier) {
    val progress = if (maxMinutes <= 0) 0f else (minutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(TimeUtils.minutesLabel(minutes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(AILockShape.pill),
            color = PandaOrange,
            trackColor = PandaCream,
        )
    }
}

@Composable
fun LockedAppCard(
    config: LockedAppConfig,
    appInfo: InstalledAppInfo?,
    usedMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val limitMinutes = config.dailyLimitMinutes ?: 120
    val remainingMinutes = (limitMinutes - usedMinutes).coerceAtLeast(0)
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = AILockShape.card,
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (appInfo != null) {
                InstalledAppIcon(appInfo, size = 48.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(AILockShape.card)
                        .background(AppSurfaceMuted)
                        .border(1.dp, AppBorder, AILockShape.card),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(config.appName.take(1), fontWeight = FontWeight.Bold, color = AppTextStrong)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(config.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("총 사용시간 ${TimeUtils.minutesLabel(usedMinutes)}", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
            }
            StatusPill(
                text = "남은 ${TimeUtils.minutesLabel(remainingMinutes)}",
                positive = remainingMinutes > 0,
            )
        }
    }
}

@Composable
fun InstalledAppIcon(app: InstalledAppInfo, size: Dp = 44.dp) {
    val bitmap = remember(app.packageName, app.icon) {
        runCatching { app.icon?.toBitmap(width = 96, height = 96)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = app.appName,
            modifier = Modifier
                .size(size)
                .clip(AILockShape.card),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(AILockShape.card)
                .background(AppSurfaceMuted)
                .border(1.dp, AppBorder, AILockShape.card),
            contentAlignment = Alignment.Center,
        ) {
            Text(app.appName.take(1), fontWeight = FontWeight.Bold, color = AppTextStrong)
        }
    }
}

private fun timerLabel(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return "${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}"
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private fun emotionLabel(emotion: PandaEmotion): String = when (emotion) {
    PandaEmotion.DEFAULT -> "기본"
    PandaEmotion.HAPPY -> "기쁨"
    PandaEmotion.ENCOURAGING -> "응원"
    PandaEmotion.THINKING -> "생각 중"
    PandaEmotion.SUSPICIOUS -> "의심"
    PandaEmotion.ANGRY -> "단호"
    PandaEmotion.SAD -> "속상"
    PandaEmotion.DISAPPOINTED -> "실망"
}

