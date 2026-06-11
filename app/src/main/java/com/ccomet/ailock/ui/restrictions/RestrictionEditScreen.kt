package com.ccomet.ailock.ui.restrictions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccomet.ailock.R
import com.ccomet.ailock.data.model.InstalledAppInfo
import com.ccomet.ailock.ui.AILockUiState
import com.ccomet.ailock.ui.LockedAppDraft
import com.ccomet.ailock.ui.components.AilockCard
import com.ccomet.ailock.ui.components.FloatingBottomActionButton
import com.ccomet.ailock.ui.components.InstalledAppIcon
import com.ccomet.ailock.ui.theme.AppBorder
import com.ccomet.ailock.ui.theme.AppTextStrong
import com.ccomet.ailock.ui.theme.AppTextSubtle
import com.ccomet.ailock.ui.theme.AILockLayout
import com.ccomet.ailock.ui.theme.AILockShape
import com.ccomet.ailock.ui.theme.AILockSpacing


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionEditScreen(
    uiState: AILockUiState,
    isEditing: Boolean,
    onBack: () -> Unit,
    onPickApp: () -> Unit,
    onDailyLimit: (Int) -> Unit,
    onLockTimer: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingTimerPart by remember { mutableStateOf<TimerInputPart?>(null) }
    var timerInputValue by remember { mutableStateOf(TextFieldValue("")) }
    val draft = uiState.draft
    val selectedApp = uiState.installedApps.firstOrNull { it.packageName == draft.packageName }
    val editableTimerMinutes = if (isEditing) draft.lockTimerMinutes else draft.dailyLimitMinutes

    fun beginTimerEdit(part: TimerInputPart) {
        val text = part.valueFrom(editableTimerMinutes).toString()
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
                val next = part.applyTo(editableTimerMinutes, parsed)
                if (isEditing) onLockTimer(next) else onDailyLimit(next)
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
                            Icon(
                                painter = painterResource(id = R.drawable.ic_action_delete_outlined),
                                contentDescription = "삭제",
                            )
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
                    .padding(horizontal = AILockSpacing.screenHorizontal)
                    .padding(top = AILockSpacing.sectionGap)
                    .padding(bottom = AILockLayout.scrollContentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(AILockSpacing.sectionGap),
            ) {
                SelectedAppSection(draft, selectedApp, onPickApp)
                if (draft.packageName.isNotBlank()) {
                    if (isEditing) {
                        TimerSettingSection(
                            title = "설정된 하루 최대 시간",
                            minutes = draft.dailyLimitMinutes,
                            readOnly = true,
                            editingPart = null,
                            inputValue = timerInputValue,
                            onInputChange = ::updateTimerInput,
                            onInputDone = { editingTimerPart = null },
                            onHoursClick = {},
                            onMinutesClick = {},
                            onHoursStep = {},
                            onMinutesStep = {},
                        )
                    }
                    TimerSettingSection(
                        title = if (isEditing) "잠금 타이머 설정" else "하루 최대 시간 설정",
                        minutes = editableTimerMinutes,
                        readOnly = false,
                        editingPart = editingTimerPart,
                        inputValue = timerInputValue,
                        onInputChange = ::updateTimerInput,
                        onInputDone = { editingTimerPart = null },
                        onHoursClick = { beginTimerEdit(TimerInputPart.HOURS) },
                        onMinutesClick = { beginTimerEdit(TimerInputPart.MINUTES) },
                        onHoursStep = { delta ->
                            editingTimerPart = null
                            val next = stepTimerHours(editableTimerMinutes, delta)
                            if (isEditing) onLockTimer(next) else onDailyLimit(next)
                        },
                        onMinutesStep = { delta ->
                            editingTimerPart = null
                            val next = stepTimerMinutes(editableTimerMinutes, delta)
                            if (isEditing) onLockTimer(next) else onDailyLimit(next)
                        },
                    )
                }
            }
            FloatingBottomActionButton(
                text = if (isEditing) "잠금 시작" else "설정하기",
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
    AilockCard(
        modifier = Modifier
            .clickable(onClick = onPickApp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AILockSpacing.itemPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AILockSpacing.iconTextGap),
        ) {
            if (draft.packageName.isBlank()) {
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
                            .border(1.dp, AppBorder, AILockShape.control),
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
    title: String,
    minutes: Int,
    readOnly: Boolean,
    editingPart: TimerInputPart?,
    inputValue: TextFieldValue,
    onInputChange: (TimerInputPart, TextFieldValue) -> Unit,
    onInputDone: () -> Unit,
    onHoursClick: () -> Unit,
    onMinutesClick: () -> Unit,
    onHoursStep: (Int) -> Unit,
    onMinutesStep: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AILockSpacing.compactGap)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AilockCard(contentPadding = PaddingValues(0.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (readOnly) {
                    StaticTimerText(
                        minutes = minutes,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TimerUnitLabel("시간")
                        Box(modifier = Modifier.width(38.dp))
                        TimerUnitLabel("분")
                    }
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerStaticReadout(
    minutes: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pad2(minutes / 60),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = AppTextStrong,
        )
        Text(
            text = " : ",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = AppTextSubtle,
        )
        Text(
            text = pad2(minutes % 60),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = AppTextStrong,
        )
    }
}

@Composable
private fun StaticTimerText(
    minutes: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "%02d시간 %02d분".format(minutes / 60, minutes % 60),
        modifier = modifier,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = AppTextStrong,
    )
}

@Composable
private fun TimerUnitLabel(text: String) {
    Box(modifier = Modifier.width(96.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = AppTextSubtle,
        )
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
            .height(TimerWheelItemHeight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
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
private const val TIMER_MIN_MINUTES = 10
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
