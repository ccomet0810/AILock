package com.ccomet.ailock.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.UserProfile
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.PermissionCards
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.SecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: AILockUiState,
    onProfile: () -> Unit,
    onPermissions: () -> Unit,
    onRestartOnboarding: () -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onSaveBaseUrl: () -> Unit,
    onMockChange: (Boolean) -> Unit,
    onStartMonitor: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("설정") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsCard {
                Text(uiState.userProfile.name.ifBlank { "이름을 알려줘" }, style = MaterialTheme.typography.titleLarge)
                Text("AILock 사용자 정보", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton("개인정보 수정", onClick = onProfile, modifier = Modifier.weight(1f), icon = Icons.Default.Person)
                    SecondaryButton("권한 관리", onClick = onPermissions, modifier = Modifier.weight(1f), icon = Icons.Default.Settings)
                }
                SecondaryButton(
                    "온보딩 다시 보기",
                    onClick = onRestartOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Refresh,
                )
            }
            SettingsCard {
                Text("백엔드 연결", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = uiState.backendBaseUrlInput,
                    onValueChange = onBaseUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    singleLine = true,
                    supportingText = {
                        Text("에뮬레이터: http://10.0.2.2:8080/ 또는 /testFinal까지 입력해도 자동 보정. 실제 폰은 서버 PC의 같은 Wi-Fi IP를 사용해요.")
                    },
                )
                PrimaryButton("주소 저장", onClick = onSaveBaseUrl, modifier = Modifier.fillMaxWidth())
            }
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mock AI mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "끄면 실제 백엔드 /testFinal로 요청하고, 실패할 때만 mock으로 대체합니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = uiState.mockAiMode, onCheckedChange = onMockChange)
                }
                PrimaryButton("사용 모니터 서비스 시작", onClick = onStartMonitor, modifier = Modifier.fillMaxWidth())
            }
            SettingsCard {
                Text("앱 정보", style = MaterialTheme.typography.titleMedium)
                Text("AILock은 강제 차단보다 자기 인식, 약속, 부드러운 개입을 먼저 설계한 HCI 과제용 네이티브 Android 앱입니다.")
                uiState.statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    uiState: AILockUiState,
    onBack: () -> Unit,
    onProfileChange: (UserProfile) -> Unit,
    onSave: () -> Unit,
) {
    val profile = uiState.profileDraft
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("개인정보 수정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = profile.name,
                onValueChange = { onProfileChange(profile.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("이름") },
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.age?.toString().orEmpty(),
                onValueChange = { onProfileChange(profile.copy(age = it.toIntOrNull())) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("나이") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.gender,
                onValueChange = { onProfileChange(profile.copy(gender = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("성별") },
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.job,
                onValueChange = { onProfileChange(profile.copy(job = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("직업") },
                singleLine = true,
            )
            PrimaryButton("저장", onClick = onSave, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagementScreen(
    uiState: AILockUiState,
    onBack: () -> Unit,
    onUsagePermission: () -> Unit,
    onOverlayPermission: () -> Unit,
    onAccessibilityPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("권한 관리") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PermissionCards(
                permissionState = uiState.permissions,
                onUsage = onUsagePermission,
                onOverlay = onOverlayPermission,
                onAccessibility = onAccessibilityPermission,
                onNotification = onNotificationPermission,
            )
            PrimaryButton("권한 상태 새로고침", onClick = onRefresh, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}
