package com.ccomet.ailock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AILockShape

@Composable
fun InstalledAppIcon(app: InstalledAppInfo, size: Dp = AILockLayout.appIconSize) {
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
