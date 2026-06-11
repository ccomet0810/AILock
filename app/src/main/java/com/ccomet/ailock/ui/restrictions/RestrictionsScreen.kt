package com.ccomet.ailock.ui.restrictions

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.ActiveLockTimerList
import com.ccomet.ailock.ui.components.AilockCard
import com.ccomet.ailock.ui.components.FloatingBottomActionButton
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.SecondaryButton
import com.ccomet.ailock.ui.components.StickyCollapsingScreenHeader
import com.ccomet.ailock.ui.components.rememberAILockHeaderMotionState
import com.ccomet.ailock.ui.components.rememberAILockHeaderNestedScrollConnection
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle

@Composable
fun RestrictionsScreen(
    uiState: AILockUiState,
    onDeleteModeChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onConfigureHardLimits: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleteApp: (Long, () -> Unit) -> Unit,
) {
    LaunchedEffect(Unit) {
        onDeleteModeChange(false)
    }

    val listState = rememberLazyListState()
    val headerMotion = rememberAILockHeaderMotionState(label = "restrictionsHeaderMotion")
    val headerNestedScrollConnection = rememberAILockHeaderNestedScrollConnection(headerMotion, listState)
    val installedAppsByPackage = uiState.installedApps.associateBy { it.packageName }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
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
                item {
                    AddRestrictionRow(
                        onClick = onAdd,
                        modifier = Modifier.padding(horizontal = AILockSpacing.screenHorizontal),
                    )
                }
                item {
                    ActiveLockTimerList(
                        configs = uiState.lockedApps,
                        installedAppsByPackage = installedAppsByPackage,
                        modifier = Modifier.padding(horizontal = AILockSpacing.screenHorizontal),
                    )
                }
                if (uiState.lockedApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .padding(horizontal = 34.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            PandaSpeechBubble(
                                "아직 설정된 강경시간이 없어.\n위 버튼에서 앱을 골라 기본 2시간으로 시작해볼까?",
                                emotion = PandaEmotion.ENCOURAGING,
                            )
                        }
                    }
                }
            }
            StickyCollapsingScreenHeader(
                title = "제한",
                subtitle = "잠금 타이머를 시작하거나 강경시간을 설정할 수 있어요.",
                collapseFraction = headerMotion.collapseFraction,
                modifier = Modifier.align(Alignment.TopCenter),
                actions = {
                    IconButton(onClick = onConfigureHardLimits) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "강경시간 제한 설정",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
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
                Text("앱을 골라 잠금 타이머를 시작해요", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
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
            modifier = Modifier.padding(horizontal = AILockSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        ) {
            Text("제한 앱을 삭제할까?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "$appName 제한을 삭제하면 AI 판단 대상에서 제외됩니다.",
                color = AppTextSubtle,
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryButton(
                text = "다시 생각해볼게",
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
