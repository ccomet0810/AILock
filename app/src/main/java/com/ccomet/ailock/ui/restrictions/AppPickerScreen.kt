package com.ccomet.ailock.ui.restrictions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.AilockCard
import com.ccomet.ailock.ui.components.AilockOutlinedTextField
import com.ccomet.ailock.ui.components.FloatingBottomActionButton
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextSubtle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    uiState: AILockUiState,
    showOnlyUnsetHardLimits: Boolean = false,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val selectedPackage = uiState.draft.packageName
    val hardLimitsByPackage = uiState.lockedApps.associateBy { it.packageName }
    val apps = if (showOnlyUnsetHardLimits) {
        uiState.installedApps.filter { hardLimitsByPackage[it.packageName] == null }
    } else {
        uiState.installedApps
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (showOnlyUnsetHardLimits) "강경시간 설정" else "앱 선택", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AILockSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap),
                contentPadding = PaddingValues(bottom = AILockLayout.scrollContentBottomPadding),
            ) {
                item {
                    AilockOutlinedTextField(
                        value = uiState.appQuery,
                        onValueChange = onQuery,
                        modifier = Modifier.fillMaxWidth(),
                        label = "앱 이름 검색",
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    )
                }
                item {
                    AilockCard(contentPadding = PaddingValues(0.dp)) {
                        Column {
                            if (apps.isEmpty()) {
                                Text(
                                    text = "설정할 앱이 없어요.",
                                    modifier = Modifier.padding(AILockSpacing.itemPadding),
                                    color = AppTextSubtle,
                                )
                            }
                            apps.forEachIndexed { index, app ->
                                InstalledAppRow(
                                    app = app,
                                    selected = app.packageName == selectedPackage,
                                    hardLimitMinutes = hardLimitsByPackage[app.packageName]?.dailyLimitMinutes,
                                    onClick = { onSelect(app.packageName) },
                                )
                                if (index != apps.lastIndex) {
                                    HorizontalDivider(color = AppBorder)
                                }
                            }
                        }
                    }
                }
            }
            FloatingBottomActionButton(
                text = if (uiState.draft.appName.isBlank()) "앱을 선택해 주세요" else "${uiState.draft.appName} 선택 완료",
                onClick = onConfirm,
                enabled = selectedPackage.isNotBlank(),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun InstalledAppRow(
    app: InstalledAppInfo,
    selected: Boolean,
    hardLimitMinutes: Int?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) AppSurfaceMuted else AppSurface)
            .padding(AILockSpacing.itemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
    ) {
        InstalledAppIcon(app)
        Column(modifier = Modifier.weight(1f)) {
            Text(app.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (hardLimitMinutes == null) {
                    "설정하지 않음 · 기본 2시간"
                } else {
                    "강경시간 ${formatTimerLabel(hardLimitMinutes)}"
                },
                color = if (hardLimitMinutes == null) MaterialTheme.colorScheme.error else AppTextSubtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "선택됨", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun formatTimerLabel(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}시간 ${mins}분"
        hours > 0 -> "${hours}시간"
        else -> "${mins}분"
    }
}
