package com.ccomet.ailock.ui.restrictions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.data.model.RestrictionType
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.LockedAppDraft
import com.ccomet.ailock.ui.components.DaySelector
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.components.LockedAppCard
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.SecondaryButton
import com.ccomet.ailock.ui.components.TimeLimitPicker
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionsScreen(
    uiState: AILockUiState,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("제한") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 22.dp),
        ) {
            item {
                PrimaryButton(
                    text = "앱 잠금 추가",
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Add,
                )
            }
            if (uiState.lockedApps.isEmpty()) {
                item {
                    PandaSpeechBubble(
                        text = "아직 제한 앱이 없어. 먼저 자꾸 켜게 되는 앱 하나부터 같이 정해보자.",
                        emotion = PandaEmotion.ENCOURAGING,
                    )
                }
            } else {
                items(uiState.lockedApps, key = { it.id }) { config ->
                    LockedAppCard(config = config, onClick = { onEdit(config.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionEditScreen(
    uiState: AILockUiState,
    isEditing: Boolean,
    onBack: () -> Unit,
    onPickApp: () -> Unit,
    onReasonPreset: (String) -> Unit,
    onReasonCustom: (String) -> Unit,
    onRestrictionType: (RestrictionType) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onDailyLimit: (Int) -> Unit,
    onToggleAdvanced: () -> Unit,
    onAdvancedLimit: (DayOfWeek, Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val draft = uiState.draft
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("앱 잠금하기") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
                        }
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SelectedAppSection(draft, onPickApp)
            ReasonSection(draft, onReasonPreset, onReasonCustom)
            RestrictionTypeSection(draft, onRestrictionType)
            if (draft.restrictionType == RestrictionType.TIME_LIMIT) {
                TimeLimitSection(
                    draft = draft,
                    onToggleDay = onToggleDay,
                    onDailyLimit = onDailyLimit,
                    onToggleAdvanced = onToggleAdvanced,
                    onAdvancedLimit = onAdvancedLimit,
                )
            } else {
                Card(shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "이 앱은 실행할 때마다 레서판다가 먼저 이유를 물어봐요. AI가 지금 사용해도 되는 상황인지 판단한 뒤 허용 시간 또는 차단 여부를 알려줍니다.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PrimaryButton(
                text = "완료",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = draft.isValid,
            )
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            appName = draft.appName,
            onDismiss = { showDeleteDialog = false },
            onDelete = {
                showDeleteDialog = false
                onDelete()
            },
        )
    }
}

@Composable
private fun SelectedAppSection(draft: LockedAppDraft, onPickApp: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("선택된 앱", style = MaterialTheme.typography.titleMedium)
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.appName.ifBlank { "아직 선택한 앱이 없어요" },
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = draft.packageName.ifBlank { "앱 선택하기 버튼을 눌러주세요" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SecondaryButton("앱 선택", onClick = onPickApp)
            }
        }
    }
}

@Composable
private fun ReasonSection(
    draft: LockedAppDraft,
    onReasonPreset: (String) -> Unit,
    onReasonCustom: (String) -> Unit,
) {
    val presets = listOf(
        "습관적으로 켜서 시간이 낭비돼요",
        "공부/작업 중 자꾸 방해돼요",
        "자기 전에 너무 오래 보게 돼요",
        "숏폼을 멈추기 어려워요",
        "기분전환으로 켰다가 오래 쓰게 돼요",
        "알림 때문에 자꾸 들어가요",
        LockedAppDraft.DIRECT_INPUT,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("앱을 잠그는 이유", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { preset ->
                FilterChip(
                    selected = draft.lockReasonPreset == preset,
                    onClick = { onReasonPreset(preset) },
                    label = { Text(preset) },
                )
            }
        }
        OutlinedTextField(
            value = draft.lockReasonCustom,
            onValueChange = onReasonCustom,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("직접 입력") },
            minLines = 3,
            placeholder = { Text("이 앱을 왜 줄이고 싶은지 적어줘. 예: 시험기간인데 자꾸 인스타를 보게 돼요.") },
        )
    }
}

@Composable
private fun RestrictionTypeSection(
    draft: LockedAppDraft,
    onRestrictionType: (RestrictionType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("제한 방식", style = MaterialTheme.typography.titleMedium)
        RestrictionType.entries.forEach { type ->
            Card(
                onClick = { onRestrictionType(type) },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (draft.restrictionType == type) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = draft.restrictionType == type,
                        onClick = { onRestrictionType(type) },
                    )
                    Column {
                        Text(type.label, fontWeight = FontWeight.Bold)
                        Text(
                            if (type == RestrictionType.TIME_LIMIT) "요일과 하루 사용 가능 시간을 정해요." else "실행할 때마다 이유를 묻고 AI 판단을 받아요.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeLimitSection(
    draft: LockedAppDraft,
    onToggleDay: (DayOfWeek) -> Unit,
    onDailyLimit: (Int) -> Unit,
    onToggleAdvanced: () -> Unit,
    onAdvancedLimit: (DayOfWeek, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("요일과 시간", style = MaterialTheme.typography.titleMedium)
        DaySelector(selectedDays = draft.selectedDays, onToggle = onToggleDay)
        TimeLimitPicker(minutes = draft.dailyLimitMinutes, onMinutesChange = onDailyLimit)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("요일별 고급 설정")
            Switch(checked = draft.isAdvancedSchedule, onCheckedChange = { onToggleAdvanced() })
        }
        if (draft.isAdvancedSchedule) {
            draft.selectedDays.sortedBy { it.value }.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dayLabel(day), modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold)
                    TimeLimitPicker(
                        minutes = draft.advancedDayLimits[day] ?: draft.dailyLimitMinutes,
                        onMinutesChange = { onAdvancedLimit(day, it) },
                        modifier = Modifier.weight(0.8f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    uiState: AILockUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val selectedPackage = uiState.draft.packageName
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("앱 선택") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
            )
        },
        bottomBar = {
            PrimaryButton(
                text = if (uiState.draft.appName.isBlank()) "앱을 선택해줘" else "${uiState.draft.appName} 잠그기",
                onClick = onConfirm,
                enabled = selectedPackage.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.appQuery,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("앱 이름 검색하기") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                )
            }
            items(uiState.installedApps, key = { it.packageName }) { app ->
                InstalledAppRow(
                    app = app,
                    selected = app.packageName == selectedPackage,
                    onClick = { onSelect(app.packageName) },
                )
            }
        }
    }
}

@Composable
private fun InstalledAppRow(
    app: InstalledAppInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InstalledAppIcon(app)
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${app.category.label} · ${app.packageName}", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    appName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("진짜... 삭제할 거야?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PandaSpeechBubble(
                    text = "$appName 제한을 지운다니 나 조금 아쉬워. 그래도 네 선택은 존중할게.",
                    emotion = PandaEmotion.SAD,
                )
                Text("유지한 기간, 지킨 약속 수, 아낀 시간은 기록 탭에서 계속 확인할 수 있어요.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) { Text("그래도 삭제할래") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("아니, 계속 지킬래") }
        },
    )
}

private fun dayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}
