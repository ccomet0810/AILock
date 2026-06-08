package com.ccomet.ailock.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.PandaSpeechBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uiState: AILockUiState) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("홈", modifier = Modifier.padding(start = 4.dp), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 34.dp),
            contentAlignment = Alignment.Center,
        ) {
            PandaSpeechBubble(
                text = homeMessage(uiState.lockedApps.size),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun homeMessage(lockedAppCount: Int): String =
    if (lockedAppCount == 0) {
        "관리할 앱을 정하면 내가 옆에서 도와줄게."
    } else {
        "오늘도 약속한 만큼만 써보자."
    }
