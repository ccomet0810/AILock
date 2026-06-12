package com.ccomet.ailock.ui.settings

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.UserProfile
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.components.AilockCard
import com.ccomet.ailock.ui.components.AilockOutlinedTextField
import com.ccomet.ailock.ui.components.FloatingBottomActionButton
import com.ccomet.ailock.ui.components.PermissionCards
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.SecondaryButton
import com.ccomet.ailock.ui.components.StickyCollapsingScreenHeader
import com.ccomet.ailock.ui.components.rememberAILockHeaderMotionState
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.PandaOrange

private const val CREATOR_PASSWORD = "0514"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: AILockUiState,
    onProfile: () -> Unit,
    onPermissions: () -> Unit,
    onAdmin: () -> Unit,
) {
    val headerMotion = rememberAILockHeaderMotionState(label = "settingsHeaderMotion")
    val headerDragState = rememberDraggableState { delta ->
        headerMotion.onDragDelta(delta)
    }
    var showCreatorDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .draggable(
                    state = headerDragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> headerMotion.settleAfterDrag(velocity) },
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AILockSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap),
            ) {
                Spacer(modifier = Modifier.height(headerMotion.currentHeaderHeight))
                SettingsCard {
                    Text(
                        uiState.userProfile.name.ifBlank { "이름을 입력해줘" },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text("AILock 사용자 정보", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SecondaryButton(
                            "개인정보 수정",
                            onClick = onProfile,
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_action_profile,
                        )
                        SecondaryButton(
                            "권한 관리",
                            onClick = onPermissions,
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_action_permissions,
                        )
                    }
                }
            }
            StickyCollapsingScreenHeader(
                title = "설정",
                subtitle = "프로필과 권한을 관리할 수 있어요",
                collapseFraction = headerMotion.collapseFraction,
                modifier = Modifier.align(Alignment.TopCenter),
                actions = {
                    IconButton(
                        onClick = {
                            passwordInput = ""
                            passwordError = false
                            showCreatorDialog = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "만든이",
                            tint = PandaOrange,
                        )
                    }
                },
            )
            if (showCreatorDialog) {
                CreatorAccessDialog(
                    password = passwordInput,
                    hasError = passwordError,
                    onPasswordChange = {
                        passwordInput = it.filter(Char::isDigit).take(4)
                        passwordError = false
                    },
                    onDismiss = {
                        showCreatorDialog = false
                        passwordInput = ""
                        passwordError = false
                    },
                    onConfirm = {
                        if (passwordInput == CREATOR_PASSWORD) {
                            showCreatorDialog = false
                            passwordInput = ""
                            passwordError = false
                            onAdmin()
                        } else {
                            passwordError = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CreatorAccessDialog(
    password: String,
    hasError: Boolean,
    onPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AilockCard(
            modifier = Modifier.padding(horizontal = AILockSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        ) {
            Text("만든이", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppTextStrong)
            Text("CNU 서민규, 최혜성, 허민경", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AppTextSubtle)
            Text("비밀번호를 입력하면 관리자 설정으로 이동해요.", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
            Column(verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("비밀번호") },
                    singleLine = true,
                    isError = hasError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = AILockShape.control,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PandaOrange,
                        unfocusedBorderColor = AppBorder,
                        focusedContainerColor = AppSurface,
                        unfocusedContainerColor = AppSurface,
                    ),
                )
                if (hasError) {
                    Text(
                        text = "비밀번호가 맞지 않아요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap)) {
                SecondaryButton(
                    text = "닫기",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "확인",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(
    uiState: AILockUiState,
    onBack: () -> Unit,
    onRestartOnboarding: () -> Unit,
    onDebugModeChange: (Boolean) -> Unit,
    onBackendBaseUrlSave: (String) -> Unit,
    onBackendConnectionTest: (String) -> Unit,
) {
    var backendUrlInput by remember(uiState.backendBaseUrl) { mutableStateOf(uiState.backendBaseUrl) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("관리자 설정", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
                .padding(horizontal = AILockSpacing.screenHorizontal)
                .padding(top = AILockSpacing.sectionGap, bottom = AILockLayout.scrollContentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap),
        ) {
            SettingsCard {
                Text("온보딩", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SecondaryButton(
                    text = "온보딩 다시 보기",
                    onClick = onRestartOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SettingsCard {
                Text("디버깅 모드", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (uiState.debugModeEnabled) "활성화됨" else "비활성화됨",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.debugModeEnabled) {
                    SecondaryButton(
                        text = "디버깅 모드 끄기",
                        onClick = { onDebugModeChange(false) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PrimaryButton(
                        text = "디버깅 모드 켜기",
                        onClick = { onDebugModeChange(true) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            SettingsCard {
                Text("백엔드 주소", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("현재 테스트할 백엔드 주소", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = backendUrlInput,
                    onValueChange = { backendUrlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Backend URL") },
                    singleLine = true,
                    shape = AILockShape.control,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PandaOrange,
                        unfocusedBorderColor = AppBorder,
                        focusedContainerColor = AppSurface,
                        unfocusedContainerColor = AppSurface,
                    ),
                )
                SecondaryButton(
                    text = "연결 테스트",
                    onClick = { onBackendConnectionTest(backendUrlInput) },
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    text = "저장",
                    onClick = { onBackendBaseUrlSave(backendUrlInput) },
                    modifier = Modifier.align(Alignment.End),
                )
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
                title = { Text("개인정보 수정", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AILockSpacing.screenHorizontal)
                    .padding(top = AILockSpacing.sectionGap, bottom = AILockLayout.scrollContentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
            ) {
                AilockOutlinedTextField(
                    value = profile.name,
                    onValueChange = { onProfileChange(profile.copy(name = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "이름",
                )
            }
            FloatingBottomActionButton(
                text = "저장",
                onClick = onSave,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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
    onBatteryPermission: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("권한 관리", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AILockSpacing.screenHorizontal)
                    .padding(top = AILockSpacing.sectionGap)
                    .padding(bottom = AILockLayout.scrollContentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.listGap),
            ) {
                PermissionCards(
                    permissionState = uiState.permissions,
                    onUsage = onUsagePermission,
                    onOverlay = onOverlayPermission,
                    onAccessibility = onAccessibilityPermission,
                    onNotification = onNotificationPermission,
                    onBattery = onBatteryPermission,
                )
            }
            FloatingBottomActionButton(
                text = "권한 상태 새로고침",
                onClick = onRefresh,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AilockCard(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap),
        content = content,
    )
}
