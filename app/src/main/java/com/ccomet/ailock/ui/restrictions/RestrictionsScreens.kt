package com.ccomet.ailock.ui.restrictions

import android.app.usage.UsageStatsManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.data.model.PandaEmotion
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.LockedAppDraft
import com.ccomet.ailock.ui.components.FloatingBottomActionButton
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.components.PandaSpeechBubble
import com.ccomet.ailock.ui.components.PrimaryButton
import com.ccomet.ailock.ui.components.SecondaryButton
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppSurface
import com.ccomet.ailock.ui.theme.AppSurfaceMuted
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.PandaOrange
import com.ccomet.ailock.util.TimeUtils
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.DialogProperties

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

    LaunchedEffect(deleteMode) {
        onDeleteModeChange(deleteMode)
        if (!deleteMode) selectedDeleteId = null
    }

    val usageMinutesByPackage = remember(uiState.lockedApps, uiState.permissions.hasUsageAccess) {
        if (uiState.permissions.hasUsageAccess) {
            todayUsageMinutesByPackage(context.getSystemService(UsageStatsManager::class.java))
        } else {
            emptyMap()
        }
    }
    val installedAppsByPackage = remember(uiState.installedApps) {
        uiState.installedApps.associateBy { it.packageName }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("제한", modifier = Modifier.padding(start = 4.dp), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            deleteMode = !deleteMode
                            if (!deleteMode) selectedDeleteId = null
                        },
                        enabled = uiState.lockedApps.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (deleteMode) "삭제 모드 종료" else "제한 앱 삭제",
                            tint = if (deleteMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.lockedApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AddRestrictionRow(
                        onClick = onAdd,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PandaSpeechBubble("아직 제한 앱이 없어요. 먼저 관리할 앱을 골라볼까요?", emotion = PandaEmotion.ENCOURAGING)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 140.dp),
                ) {
                    item {
                        AddRestrictionRow(onClick = onAdd)
                    }
                    item {
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
            if (deleteMode) {
                DeleteModeBottomBar(
                    selectedAppName = selectedDeleteApp?.appName,
                    onDelete = { if (selectedDeleteApp != null) showDeleteDialog = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
    ) {
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
    val infiniteTransition = rememberInfiniteTransition(label = "delete-row-shake")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -3.2f,
        targetValue = 3.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "delete-icon-rotation",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) AppSurfaceMuted else AppSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.graphicsLayer(rotationZ = if (deleteMode && selected) rotation else 0f)) {
            if (appInfo != null) {
                InstalledAppIcon(appInfo, size = 48.dp)
            } else {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .background(AppSurfaceMuted, RoundedCornerShape(8.dp)),
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
                Spacer(Modifier.width(8.dp))
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppSurfaceMuted),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .clip(RoundedCornerShape(8.dp))
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
        icon = Icons.Default.Delete,
        modifier = modifier,
    )
}

@Composable
private fun DeleteAppConfirmDialog(
    appName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = BorderStroke(1.dp, AppBorder),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
}

private fun formatRemainingTime(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val mins = safeMinutes % 60
    return "%02d시간 %02d분 00초 남음".format(hours, mins)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionEditScreen(
    uiState: AILockUiState,
    isEditing: Boolean,
    onBack: () -> Unit,
    onPickApp: () -> Unit,
    onDailyLimit: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingTimerPart by remember { mutableStateOf<TimerInputPart?>(null) }
    var timerInputValue by remember { mutableStateOf(TextFieldValue("")) }
    val draft = uiState.draft
    val selectedApp = uiState.installedApps.firstOrNull { it.packageName == draft.packageName }

    fun beginTimerEdit(part: TimerInputPart) {
        val text = part.valueFrom(draft.dailyLimitMinutes).toString()
        timerInputValue = TextFieldValue(text = text, selection = TextRange(0, text.length))
        editingTimerPart = part
    }

    fun updateTimerInput(part: TimerInputPart, value: TextFieldValue) {
        val cleaned = value.text.filter { it.isDigit() }.take(2)
        val parsed = cleaned.toIntOrNull()
        if (parsed == null || parsed in part.validRange) {
            timerInputValue = TextFieldValue(
                text = cleaned,
                selection = TextRange(cleaned.length),
            )
            if (parsed != null) {
                onDailyLimit(part.applyTo(draft.dailyLimitMinutes, parsed))
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditing) "앱 잠금 수정" else "앱 잠금하기", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .padding(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                SelectedAppSection(draft, selectedApp, onPickApp)
                if (draft.packageName.isNotBlank()) {
                    TimerSettingSection(
                        minutes = draft.dailyLimitMinutes,
                        editingPart = editingTimerPart,
                        inputValue = timerInputValue,
                        onInputChange = ::updateTimerInput,
                        onInputDone = { editingTimerPart = null },
                        onHoursClick = { beginTimerEdit(TimerInputPart.HOURS) },
                        onMinutesClick = { beginTimerEdit(TimerInputPart.MINUTES) },
                        onHoursStep = { delta ->
                            editingTimerPart = null
                            onDailyLimit(stepTimerHours(draft.dailyLimitMinutes, delta))
                        },
                        onMinutesStep = { delta ->
                            editingTimerPart = null
                            onDailyLimit(stepTimerMinutes(draft.dailyLimitMinutes, delta))
                        },
                    )
                }
            }
            FloatingBottomActionButton(
                text = if (isEditing) "저장" else "설정하기",
                onClick = onSave,
                enabled = draft.isValid,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(draft.appName, onDismiss = { showDeleteDialog = false }) {
            showDeleteDialog = false
            onDelete()
        }
    }
}

@Composable
private fun SelectedAppSection(draft: LockedAppDraft, selectedApp: InstalledAppInfo?, onPickApp: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPickApp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (draft.packageName.isBlank()) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("앱 선택하기", fontWeight = FontWeight.Bold, color = AppTextStrong)
                    Text("관리할 앱을 선택해요", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
                }
            } else {
                if (selectedApp != null) {
                    InstalledAppIcon(selectedApp, size = 48.dp)
                } else {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp)
                            .border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(draft.appName.take(1), fontWeight = FontWeight.Bold, color = AppTextSubtle)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(draft.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("앱 변경하기", style = MaterialTheme.typography.bodySmall, color = AppTextSubtle)
                }
            }
        }
    }
}

@Composable
private fun TimerSettingSection(
    minutes: Int,
    editingPart: TimerInputPart?,
    inputValue: TextFieldValue,
    onInputChange: (TimerInputPart, TextFieldValue) -> Unit,
    onInputDone: () -> Unit,
    onHoursClick: () -> Unit,
    onMinutesClick: () -> Unit,
    onHoursStep: (Int) -> Unit,
    onMinutesStep: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("타이머 설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = BorderStroke(1.dp, AppBorder),
        ) {
            TimerReadout(
                minutes = minutes,
                editingPart = editingPart,
                inputValue = inputValue,
                onInputChange = onInputChange,
                onInputDone = onInputDone,
                onHoursClick = onHoursClick,
                onMinutesClick = onMinutesClick,
                onHoursStep = onHoursStep,
                onMinutesStep = onMinutesStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 27.dp),
            )
        }
    }
}

@Composable
private fun TimerReadout(
    minutes: Int,
    editingPart: TimerInputPart?,
    inputValue: TextFieldValue,
    onInputChange: (TimerInputPart, TextFieldValue) -> Unit,
    onInputDone: () -> Unit,
    onHoursClick: () -> Unit,
    onMinutesClick: () -> Unit,
    onHoursStep: (Int) -> Unit,
    onMinutesStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = minutes / 60
    val mins = minutes % 60
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerNumber(
            part = TimerInputPart.HOURS,
            value = pad2(hours),
            previousValue = pad2(wrappedHours(hours - 1)),
            nextValue = pad2(wrappedHours(hours + 1)),
            editing = editingPart == TimerInputPart.HOURS,
            inputValue = inputValue,
            onInputChange = { value -> onInputChange(TimerInputPart.HOURS, value) },
            onInputDone = onInputDone,
            onClick = onHoursClick,
            onStep = onHoursStep,
        )
        Text(
            text = " : ",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = AppTextSubtle,
        )
        TimerNumber(
            part = TimerInputPart.MINUTES,
            value = pad2(mins),
            previousValue = pad2(wrappedMinutes(mins - 1)),
            nextValue = pad2(wrappedMinutes(mins + 1)),
            editing = editingPart == TimerInputPart.MINUTES,
            inputValue = inputValue,
            onInputChange = { value -> onInputChange(TimerInputPart.MINUTES, value) },
            onInputDone = onInputDone,
            onClick = onMinutesClick,
            onStep = onMinutesStep,
        )
    }
}

@Composable
private fun TimerNumber(
    part: TimerInputPart,
    value: String,
    previousValue: String,
    nextValue: String,
    editing: Boolean,
    inputValue: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onInputDone: () -> Unit,
    onClick: () -> Unit,
    onStep: (Int) -> Unit,
) {
    var dragAmount by remember { mutableFloatStateOf(0f) }
    val focusRequester = remember { FocusRequester() }
    val itemHeightPx = with(LocalDensity.current) { TimerWheelItemHeight.toPx() }
    val visualOffset by animateFloatAsState(
        targetValue = dragAmount.coerceIn(-itemHeightPx, itemHeightPx),
        animationSpec = tween(durationMillis = 90),
        label = "timer-wheel-offset",
    )
    val dragState = rememberDraggableState { delta ->
        dragAmount += delta
        val steps = (dragAmount / itemHeightPx).toInt()
        if (steps != 0) {
            dragAmount -= steps * itemHeightPx
            onStep(-steps)
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .width(96.dp)
            .height(TimerWheelItemHeight * 3)
            .clipToBounds()
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { dragAmount = 0f },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = visualOffset },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TimerSideValue(previousValue, modifier = Modifier.height(TimerWheelItemHeight))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TimerWheelItemHeight),
                contentAlignment = Alignment.Center,
            ) {
                if (editing) {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .width(84.dp)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            color = AppTextStrong,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onInputDone() }),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.Center) {
                                if (inputValue.text.isBlank()) {
                                    Text(
                                        text = part.placeholder,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTextSubtle.copy(alpha = 0.45f),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                } else {
                    Text(
                        text = value,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTextStrong,
                    )
                }
            }
            TimerSideValue(nextValue, modifier = Modifier.height(TimerWheelItemHeight))
        }
    }
}

@Composable
private fun TimerSideValue(value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = value,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = AppTextSubtle.copy(alpha = 0.28f),
        )
    }
}

private fun todayUsageMinutesByPackage(manager: UsageStatsManager): Map<String, Int> {
    val start = TimeUtils.todayStartMillis()
    val now = System.currentTimeMillis()
    return manager.queryAndAggregateUsageStats(start, now)
        .mapValues { (_, stats) -> (stats.totalTimeInForeground / 60_000L).toInt() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(uiState: AILockUiState, onBack: () -> Unit, onQuery: (String) -> Unit, onSelect: (String) -> Unit, onConfirm: () -> Unit) {
    val selectedPackage = uiState.draft.packageName
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("앱 선택", fontWeight = FontWeight.Bold) },
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 140.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.appQuery,
                        onValueChange = onQuery,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("앱 이름 검색") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = AILockShape.control,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PandaOrange,
                            unfocusedBorderColor = AppBorder,
                            focusedContainerColor = AppSurface,
                            unfocusedContainerColor = AppSurface,
                        ),
                        singleLine = true,
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        border = BorderStroke(1.dp, AppBorder),
                    ) {
                        Column {
                            uiState.installedApps.forEachIndexed { index, app ->
                                InstalledAppRow(
                                    app = app,
                                    selected = app.packageName == selectedPackage,
                                    onClick = { onSelect(app.packageName) },
                                )
                                if (index != uiState.installedApps.lastIndex) {
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
private fun InstalledAppRow(app: InstalledAppInfo, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) AppSurfaceMuted else AppSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstalledAppIcon(app)
        Column(modifier = Modifier.weight(1f)) {
            Text(app.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${app.category.label} · ${app.packageName}", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "선택됨", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DeleteConfirmDialog(appName: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    DeleteAppConfirmDialog(appName = appName, onDismiss = onDismiss, onDelete = onDelete)
}

private enum class TimerInputPart(
    val validRange: IntRange,
    val placeholder: String,
) {
    HOURS(validRange = 0..23, placeholder = "HH"),
    MINUTES(validRange = 0..59, placeholder = "MM");

    fun valueFrom(totalMinutes: Int): Int = when (this) {
        HOURS -> totalMinutes / 60
        MINUTES -> totalMinutes % 60
    }

    fun applyTo(totalMinutes: Int, value: Int): Int = when (this) {
        HOURS -> setTimerHours(totalMinutes, value)
        MINUTES -> setTimerMinutes(totalMinutes, value)
    }
}

private val TimerWheelItemHeight = 62.dp
private const val TIMER_MIN_MINUTES = 0
private const val TIMER_MAX_MINUTES = 23 * 60 + 59

private fun clampTimerMinutes(minutes: Int): Int = minutes.coerceIn(TIMER_MIN_MINUTES, TIMER_MAX_MINUTES)

private fun stepTimerHours(currentMinutes: Int, delta: Int): Int =
    setTimerHours(currentMinutes, (currentMinutes / 60) + delta)

private fun stepTimerMinutes(currentMinutes: Int, delta: Int): Int =
    setTimerMinutes(currentMinutes, (currentMinutes % 60) + delta)

private fun setTimerHours(currentMinutes: Int, hours: Int): Int {
    val safeHours = wrappedHours(hours)
    return clampTimerMinutes((safeHours * 60) + (currentMinutes % 60))
}

private fun setTimerMinutes(currentMinutes: Int, minutes: Int): Int {
    val safeMinutes = wrappedMinutes(minutes)
    return clampTimerMinutes(((currentMinutes / 60) * 60) + safeMinutes)
}

private fun wrappedHours(value: Int): Int = Math.floorMod(value, 24)

private fun wrappedMinutes(value: Int): Int = Math.floorMod(value, 60)

private fun pad2(value: Int): String = value.toString().padStart(2, '0')

