package com.ccomet.ailock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.PermissionState
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.SoftGreenContainer
import com.ccomet.ailock.ui.theme.SoftRedContainer


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
            .padding(AILockSpacing.itemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
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
