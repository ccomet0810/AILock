package com.ccomet.ailock.ui.restrictions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.app.usage.UsageStatsManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.AilockCard
import com.ccomet.ailock.ui.components.FloatingBottomActionButton
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.SectionTitle
import com.ccomet.ailock.ui.components.SecondaryButton
import com.ccomet.ailock.ui.components.StickyCollapsingScreenHeader
import com.ccomet.ailock.ui.components.rememberAILockHeaderMotionState
import com.ccomet.ailock.ui.components.rememberAILockHeaderNestedScrollConnection
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.PandaOrange
import com.ccomet.ailock.util.TimeUtils
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionsScreen(
    uiState: AILockUiState,
    onDeleteModeChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleteApp: (Long, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var deleteMode by remember { mutableStateOf(false) }
    var selectedDeleteId by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val selectedDeleteApp = uiState.lockedApps.firstOrNull { it.id == selectedDeleteId }
    val listState = rememberLazyListState()
    val headerMotion = rememberAILockHeaderMotionState(label = "restrictionsHeaderMotion")
    val headerNestedScrollConnection = rememberAILockHeaderNestedScrollConnection(headerMotion, listState)

    LaunchedEffect(deleteMode) {
        onDeleteModeChange(deleteMode)
        if (!deleteMode) selectedDeleteId = null
    }

    var usageMinutesByPackage by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(uiState.lockedApps, uiState.permissions.hasUsageAccess) {
        if (uiState.permissions.hasUsageAccess) {
            val manager = context.getSystemService(UsageStatsManager::class.java)
            usageMinutesByPackage = withContext(Dispatchers.IO) {
                todayUsageMinutesByPackage(manager)
            }
        } else {
            usageMinutesByPackage = emptyMap()
        }
    }
    val installedAppsByPackage = remember(uiState.installedApps) {
        uiState.installedApps.associateBy { it.packageName }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(headerNestedScrollConnection),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap),
                contentPadding = PaddingValues(bottom = AILockLayout.scrollContentBottomPadding),
            ) {
                item {
                    Spacer(modifier = Modifier.height(headerMotion.currentHeaderHeight))
                }
                if (uiState.lockedApps.isEmpty()) {
                    item {
                        AddRestrictionRow(
                            onClick = onAdd,
                            modifier = Modifier.padding(horizontal = AILockSpacing.screenHorizontal),
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .padding(horizontal = 34.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            PandaSpeechBubble(
                                "아직 제한 앱이 없어요. 먼저 관리할 앱을 골라볼까요?",
                                emotion = PandaEmotion.ENCOURAGING,
                            )
                        }
                    }
                } else {
                    item {
                        AddRestrictionRow(
                            onClick = onAdd,
                            modifier = Modifier.padding(horizontal = AILockSpacing.screenHorizontal),
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = AILockSpacing.screenHorizontal)
                                .padding(top = 6.dp),
                        ) {
                            SectionTitle(title = "제한한 앱")
                            LockedAppsList(
                                configs = uiState.lockedApps,
                                installedAppsByPackage = installedAppsByPackage,
                                usageMinutesByPackage = usageMinutesByPackage,
                                deleteMode = deleteMode,
                                selectedDeleteId = selectedDeleteId,
                                onRowClick = { config ->
                                    if (deleteMode) {
                                        selectedDeleteId = if (selectedDeleteId == config.id) null else config.id
                                    } else {
                                        onEdit(config.id)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            StickyCollapsingScreenHeader(
                title = "제한",
                subtitle = "관리할 앱을 추가하고 정리할 수 있어요",
                collapseFraction = headerMotion.collapseFraction,
                modifier = Modifier.align(Alignment.TopCenter),
                actions = {
                    IconButton(
                        onClick = {
                            deleteMode = !deleteMode
                            if (!deleteMode) selectedDeleteId = null
                        },
                        enabled = uiState.lockedApps.isNotEmpty(),
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (deleteMode) {
                                    R.drawable.ic_action_delete_filled
                                } else {
                                    R.drawable.ic_action_delete_outlined
                                },
                            ),
                            contentDescription = if (deleteMode) "삭제 모드 종료" else "제한 앱 삭제",
                            tint = if (deleteMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            AnimatedVisibility(
                visible = deleteMode,
                enter = slideInVertically(
                    animationSpec = tween(150),
                    initialOffsetY = { it },
                ) + fadeIn(animationSpec = tween(90)),
                exit = slideOutVertically(
                    animationSpec = tween(150),
                    targetOffsetY = { it },
                ) + fadeOut(animationSpec = tween(90)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                DeleteModeBottomBar(
                    selectedAppName = selectedDeleteApp?.appName,
                    onDelete = { if (selectedDeleteApp != null) showDeleteDialog = true },
                )
            }
        }
    }

    if (showDeleteDialog && selectedDeleteApp != null) {
        DeleteAppConfirmDialog(
            appName = selectedDeleteApp.appName,
            onDismiss = { showDeleteDialog = false },
            onDelete = {
                val id = selectedDeleteApp.id
                showDeleteDialog = false
                onDeleteApp(id) {
                    selectedDeleteId = null
                    deleteMode = false
                }
            },
        )
    }
}

@Composable
private fun AddRestrictionRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AilockCard(modifier = modifier, contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(AILockSpacing.itemPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .border(1.dp, AppBorder, AILockShape.control),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("제한 앱 추가", fontWeight = FontWeight.Bold, color = AppTextStrong)
                Text("관리할 앱을 선택해요", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
            }
        }
    }
}

@Composable
private fun LockedAppsList(
    configs: List<LockedAppConfig>,
    installedAppsByPackage: Map<String, InstalledAppInfo>,
    usageMinutesByPackage: Map<String, Int>,
    deleteMode: Boolean,
    selectedDeleteId: Long?,
    onRowClick: (LockedAppConfig) -> Unit,
) {
    AilockCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            configs.forEachIndexed { index, config ->
                LockedAppListRow(
                    config = config,
                    appInfo = installedAppsByPackage[config.packageName],
                    usedMinutes = usageMinutesByPackage[config.packageName] ?: 0,
                    deleteMode = deleteMode,
                    selected = selectedDeleteId == config.id,
                    onClick = { onRowClick(config) },
                )
                if (index != configs.lastIndex) {
                    HorizontalDivider(color = AppBorder)
                }
            }
        }
    }
}

@Composable
private fun LockedAppListRow(
    config: LockedAppConfig,
    appInfo: InstalledAppInfo?,
    usedMinutes: Int,
    deleteMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val limitMinutes = config.dailyLimitMinutes ?: 120
    val remainingMinutes = (limitMinutes - usedMinutes).coerceAtLeast(0)
    val progress = if (limitMinutes <= 0) 0f else (usedMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f)
    val rotation = if (deleteMode && selected) {
        val infiniteTransition = rememberInfiniteTransition(label = "delete-row-shake")
        val animatedRotation by infiniteTransition.animateFloat(
            initialValue = -3.2f,
            targetValue = 3.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 48, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "delete-icon-rotation",
        )
        animatedRotation
    } else {
        0f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) AppSurfaceMuted else AppSurface)
            .padding(AILockSpacing.itemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
    ) {
        Box(modifier = Modifier.graphicsLayer(rotationZ = rotation)) {
            if (appInfo != null) {
                InstalledAppIcon(appInfo, size = 48.dp)
            } else {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .background(AppSurfaceMuted, AILockShape.control),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(config.appName.take(1), fontWeight = FontWeight.Bold, color = AppTextStrong)
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    config.appName,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(AILockSpacing.buttonIconGap))
                Text(
                    formatRemainingTime(remainingMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTextSubtle,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(AILockShape.pill)
                    .background(AppSurfaceMuted),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .clip(AILockShape.pill)
                        .background(PandaOrange),
                )
            }
        }
    }
}

@Composable
private fun DeleteModeBottomBar(
    selectedAppName: String?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingBottomActionButton(
        text = selectedAppName?.let { "$it 삭제하기" } ?: "삭제할 앱을 선택해 주세요",
        onClick = onDelete,
        enabled = selectedAppName != null,
        modifier = modifier,
        animateOnMount = false,
    )
}

@Composable
internal fun DeleteAppConfirmDialog(
    appName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AilockCard(
            modifier = Modifier
                .padding(horizontal = AILockSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        ) {
            Text("제한 앱을 삭제할까요?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "$appName 제한을 삭제하면 AI 판단 대상에서 제외됩니다.",
                color = AppTextSubtle,
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryButton(
                text = "다시 한번 생각해볼게",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "삭제하기",
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun formatRemainingTime(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val mins = safeMinutes % 60
    return "%02d시간 %02d분 00초 남음".format(hours, mins)
}


private fun todayUsageMinutesByPackage(manager: UsageStatsManager): Map<String, Int> {
    val start = TimeUtils.todayStartMillis()
    val now = System.currentTimeMillis()
    return manager.queryAndAggregateUsageStats(start, now)
        .mapValues { (_, stats) -> (stats.totalTimeInForeground / 60_000L).toInt() }
}
