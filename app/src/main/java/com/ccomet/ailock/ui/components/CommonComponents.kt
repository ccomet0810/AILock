package com.ccomet.ailock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import com.ccomet.ailock.data.model.RestrictionType
import com.ccomet.ailock.ui.theme.BarkBrown
import com.ccomet.ailock.ui.theme.CreamDeep
import com.ccomet.ailock.ui.theme.PandaOrange
import com.ccomet.ailock.util.TimeUtils
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable
fun RedPandaMascot(
    emotion: PandaEmotion,
    modifier: Modifier = Modifier,
) {
    val background = when (emotion) {
        PandaEmotion.HAPPY, PandaEmotion.ENCOURAGING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
        PandaEmotion.ANGRY, PandaEmotion.DISAPPOINTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        PandaEmotion.SAD -> MaterialTheme.colorScheme.secondaryContainer
        PandaEmotion.THINKING, PandaEmotion.SUSPICIOUS -> CreamDeep
        PandaEmotion.DEFAULT -> MaterialTheme.colorScheme.primaryContainer
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.red_panda),
                contentDescription = "레서판다",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            )
        }
        Text(
            text = emotionLabel(emotion),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PandaSpeechBubble(
    text: String,
    emotion: PandaEmotion = PandaEmotion.DEFAULT,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RedPandaMascot(emotion = emotion, modifier = Modifier.width(104.dp))
        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PandaOrange),
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
        shape = RoundedCornerShape(8.dp),
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
fun FloatingBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        listOf(
            BottomItem("home", "홈", Icons.Default.Home),
            BottomItem("records", "기록", Icons.Default.List),
            BottomItem("restrictions", "제한", Icons.Default.Lock),
            BottomItem("settings", "설정", Icons.Default.Settings),
        ).forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = if (granted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusPill(if (granted) "허용됨" else "필요함", granted)
            }
            SecondaryButton(
                text = "설정",
                onClick = onOpenSettings,
                modifier = Modifier.width(82.dp),
            )
        }
    }
}

@Composable
fun PermissionCards(
    permissionState: PermissionState,
    onUsage: () -> Unit,
    onOverlay: () -> Unit,
    onAccessibility: () -> Unit,
    onNotification: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PermissionCard("사용 기록 접근", "현재 사용량과 제한 시간 계산", permissionState.hasUsageAccess, onUsage)
        PermissionCard("다른 앱 위에 표시", "제한 앱 위 레서판다 팝업 표시", permissionState.canDrawOverlays, onOverlay)
        PermissionCard("접근성 서비스", "제한 앱 실행 감지와 홈 이동 유도", permissionState.isAccessibilityEnabled, onAccessibility)
        PermissionCard("알림 권한", "종료 전 안내와 약속 알림", permissionState.hasNotificationPermission, onNotification)
    }
}

@Composable
fun StatusPill(text: String, positive: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(top = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (positive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
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
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = listOf(
        DayOfWeek.MONDAY to "월",
        DayOfWeek.TUESDAY to "화",
        DayOfWeek.WEDNESDAY to "수",
        DayOfWeek.THURSDAY to "목",
        DayOfWeek.FRIDAY to "금",
        DayOfWeek.SATURDAY to "토",
        DayOfWeek.SUNDAY to "일",
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { (day, label) ->
            FilterChip(
                selected = day in selectedDays,
                onClick = { onToggle(day) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun TimeLimitPicker(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("사용 가능 시간", fontWeight = FontWeight.SemiBold)
            Text("${minutes}분", color = BarkBrown, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = minutes.toFloat(),
            onValueChange = { onMinutesChange(it.roundToInt().coerceIn(5, 180)) },
            valueRange = 5f..180f,
            steps = 34,
        )
    }
}

@Composable
fun LockedAppCard(
    config: LockedAppConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CreamDeep)
                    .border(1.dp, PandaOrange.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(config.appName.take(1), fontWeight = FontWeight.Bold, color = BarkBrown)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(config.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(config.restrictionType.label, style = MaterialTheme.typography.bodySmall)
                Text(
                    config.lockReasonFinal.ifBlank { "아직 이유가 비어 있어요" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(
                text = if (config.restrictionType == RestrictionType.TIME_LIMIT) {
                    "${config.dailyLimitMinutes ?: 0}분"
                } else {
                    "즉시"
                },
                positive = true,
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
                .clip(RoundedCornerShape(8.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(CreamDeep),
            contentAlignment = Alignment.Center,
        ) {
            Text(app.appName.take(1), fontWeight = FontWeight.Bold, color = BarkBrown)
        }
    }
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private fun emotionLabel(emotion: PandaEmotion): String = when (emotion) {
    PandaEmotion.DEFAULT -> "기본"
    PandaEmotion.HAPPY -> "뿌듯"
    PandaEmotion.ENCOURAGING -> "응원"
    PandaEmotion.THINKING -> "생각 중"
    PandaEmotion.SUSPICIOUS -> "의심"
    PandaEmotion.ANGRY -> "단호"
    PandaEmotion.SAD -> "속상"
    PandaEmotion.DISAPPOINTED -> "실망"
}
