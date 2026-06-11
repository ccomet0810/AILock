package com.ccomet.ailock.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.ActiveLockTimerList
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uiState: AILockUiState) {
    val installedAppsByPackage = uiState.installedApps.associateBy { it.packageName }
    val hasActiveLockTimer = uiState.lockedApps.any { (it.lockUntilAt ?: 0L) > System.currentTimeMillis() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = if (hasActiveLockTimer) Alignment.TopCenter else Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AILockSpacing.screenHorizontal)
                    .padding(
                        top = if (hasActiveLockTimer) AILockSpacing.sectionGap else 0.dp,
                        bottom = AILockLayout.scrollContentBottomPadding,
                    ),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.sectionGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PandaSpeechBubble(
                        text = homeMessage(uiState.lockedApps.size),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ActiveLockTimerList(
                    configs = uiState.lockedApps,
                    installedAppsByPackage = installedAppsByPackage,
                    title = "작동 중인 잠금",
                )
            }
        }
    }
}

private fun homeMessage(lockedAppCount: Int): String =
    if (lockedAppCount == 0) {
        "관리할 앱을 정하면 내가 옆에서 도와줄게."
    } else {
        "오늘도 약속한 만큼만 써보자."
    }
