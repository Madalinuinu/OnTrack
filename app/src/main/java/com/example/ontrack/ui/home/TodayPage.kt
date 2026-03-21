package com.example.ontrack.ui.home

import android.content.ClipData
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.PlayCircle
import com.example.ontrack.data.local.entity.FrequencyType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayPage(
    viewModel: HomeViewModel,
    onStartTimer: (systemId: Long, habitId: Long, habitTitle: String, totalSeconds: Int) -> Unit,
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    vacationModeEnabled: Boolean = false,
    vacationModeFromEpochDay: Long = -1L,
    persistedVacationEpochDays: Set<Long> = emptySet(),
    modifier: Modifier = Modifier
) {
    val systems by viewModel.systems.collectAsState(initial = emptyList())
    val todayTasksFiltered by viewModel.todayTasksFiltered.collectAsState(initial = emptyList())
    val filterGoalIds by viewModel.todayFilterGoalIds.collectAsState(initial = null)
    val todayActiveTimer by viewModel.todayActiveTimer.collectAsState(initial = null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        val selectedDate by viewModel.selectedDate.collectAsState(initial = com.example.ontrack.util.EffectiveDate.today())
        val today = com.example.ontrack.util.EffectiveDate.today()
        val headerTitle = if (selectedDate == today) {
            "TODAY"
        } else {
            selectedDate.format(DateTimeFormatter.ofPattern("EEEE"))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.05.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        val weekStart = com.example.ontrack.util.EffectiveDate.today().with(DayOfWeek.MONDAY)
        val daysToShow = (0..6).map { weekStart.plusDays(it.toLong()) }
        val dayFormatter = DateTimeFormatter.ofPattern("EEE")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            daysToShow.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                DayChip(
                    date = date,
                    dayFormatter = dayFormatter,
                    isSelected = isSelected,
                    isToday = isToday,
                    onClick = { viewModel.setSelectedDate(date) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.height(24.dp))

        val chipScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                label = "All",
                selected = filterGoalIds == null,
                onClick = { viewModel.setTodayFilter(null) }
            )
            systems.forEachIndexed { index, system ->
                DraggableGoalChip(
                    label = system.goal,
                    systemId = system.id,
                    index = index,
                    selected = filterGoalIds?.contains(system.id) == true,
                    onClick = { viewModel.setTodayFilter(setOf(system.id)) },
                    onReorder = { from, to -> viewModel.reorderSystems(from, to) }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        val todayEpoch = com.example.ontrack.util.EffectiveDate.todayEpoch()
        val selectedEpoch = selectedDate.toEpochDay()
        val vacationDays = if (vacationModeEnabled && vacationModeFromEpochDay >= 0) {
            (vacationModeFromEpochDay..todayEpoch).toSet() + persistedVacationEpochDays
        } else {
            persistedVacationEpochDays
        }
        val isVacationDay = selectedEpoch in vacationDays

        if (isVacationDay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enjoy your vacation!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
        val isToday = selectedDate == com.example.ontrack.util.EffectiveDate.today()
        var pendingStartConfirm by remember { mutableStateOf<TodayTaskItem?>(null) }
        pendingStartConfirm?.let { pending ->
            StartTaskConfirmDialog(
                pending = pending,
                onDismiss = { pendingStartConfirm = null },
                onConfirm = {
                    viewModel.confirmStartOngoingTask(
                        pending.habit.systemId,
                        pending.habit.id,
                        pending.habit.title
                    )
                    pendingStartConfirm = null
                }
            )
        }
        if (todayTasksFiltered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val emptyMessage = when {
                    !isToday && selectedDate.isBefore(com.example.ontrack.util.EffectiveDate.today()) -> "No completions on this day."
                    !isToday && selectedDate.isAfter(com.example.ontrack.util.EffectiveDate.today()) -> "Nothing due on this day."
                    filterGoalIds != null -> "No habits in selected goals."
                    else -> "No habits yet. Add goals and habits to see them here."
                }
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        } else {
            var displayTasks by remember(todayTasksFiltered) { mutableStateOf(todayTasksFiltered) }
            LaunchedEffect(todayTasksFiltered) {
                displayTasks = todayTasksFiltered
            }
            val haptic = LocalHapticFeedback.current
            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                displayTasks = displayTasks.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
                viewModel.reorderTodayTasks(displayTasks.map { it.habit.id })
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    displayTasks,
                    key = { _, item -> item.habit.id }
                ) { index, item ->
                    ReorderableItem(reorderableState, key = item.habit.id) { isDragging ->
                        val interactionSource = remember { MutableInteractionSource() }
                        TodayTaskCard(
                            modifier = with(this) {
                                Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .longPressDraggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        interactionSource = interactionSource
                                    )
                            },
                            task = item,
                            trackTimeEnabled = false,
                            activeTimer = null,
                            isInteractiveDay = isToday,
                            taskTapEnabled = !item.isCompletedToday,
                            interactionSource = interactionSource,
                            isDragging = isDragging,
                            onCardClick = {
                                when {
                                    item.isCompletedToday -> { }
                                    item.isOngoingToday ->
                                        viewModel.onTodayTaskClick(
                                            item.habit.systemId,
                                            item.habit.id
                                        )
                                    else -> pendingStartConfirm = item
                                }
                            },
                            onStartClick = {},
                            onPauseClick = {},
                            onResumeClick = {}
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
        }
    }

    // Track time via timers is disabled; tasks complete directly on tap.
}

@Composable
private fun DayChip(
    date: LocalDate,
    dayFormatter: DateTimeFormatter,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weekdayLabel = date.format(dayFormatter)
    val dayOfMonth = date.dayOfMonth.toString()
    val chipColor = when {
        isToday && isSelected -> Color.White.copy(alpha = 0.5f)
        isToday && !isSelected -> Color.White.copy(alpha = 0.2f)
        isSelected -> Color.White.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = chipColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weekdayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = if (isSelected) 1f else 0.9f),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = dayOfMonth,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color.White else MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggableGoalChip(
    label: String,
    systemId: Long,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val reorderDropTarget = remember(index) {
        object : androidx.compose.ui.draganddrop.DragAndDropTarget {
            private fun getSourceIndex(event: androidx.compose.ui.draganddrop.DragAndDropEvent): Int? {
                val clipData = event.toAndroidDragEvent().clipData ?: return null
                if (clipData.itemCount == 0) return null
                val text = clipData.getItemAt(0).text?.toString() ?: return null
                val parts = text.split(",")
                if (parts.size != 2) return null
                return parts[1].toIntOrNull()
            }
            override fun onEntered(event: androidx.compose.ui.draganddrop.DragAndDropEvent) {
                val fromIdx = getSourceIndex(event) ?: return
                if (fromIdx != index) onReorder(fromIdx, index)
            }
            override fun onDrop(event: androidx.compose.ui.draganddrop.DragAndDropEvent): Boolean {
                val fromIdx = getSourceIndex(event) ?: return false
                if (fromIdx != index) onReorder(fromIdx, index)
                return true
            }
        }
    }
    Card(
        modifier = modifier
            .dragAndDropSource(
                block = {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            startTransfer(
                                DragAndDropTransferData(
                                    clipData = ClipData.newPlainText("goal", "$systemId,$index"),
                                    flags = View.DRAG_FLAG_OPAQUE
                                )
                            )
                        },
                        onDrag = { _: androidx.compose.ui.input.pointer.PointerInputChange, _: androidx.compose.ui.geometry.Offset -> }
                    )
                }
            )
            .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = reorderDropTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color.White else MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StartTaskConfirmDialog(
    pending: TodayTaskItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 300.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    scheme.primary,
                                    scheme.primary.copy(alpha = 0.85f),
                                    Color(0xFF58CCE8)
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Start task",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.02.sp
                            ),
                            color = Color.White
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = pending.habit.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pending.goalName,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, scheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Text("Start", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayTaskCard(
    task: TodayTaskItem,
    trackTimeEnabled: Boolean,
    activeTimer: TodayActiveTimer?,
    isInteractiveDay: Boolean,
    /** When false, tap does not run task actions (e.g. Done cannot be toggled off). */
    taskTapEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    isDragging: Boolean = false,
    onCardClick: () -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit
) {
    val isCompleted = task.isCompletedToday
    val isOngoing = task.isOngoingToday && !isCompleted
    val allowTaskTap = isInteractiveDay && !trackTimeEnabled && taskTapEnabled
    Card(
        modifier = modifier.then(
            if (allowTaskTap && interactionSource == null) Modifier.clickable(onClick = onCardClick)
            else Modifier
        ),
        onClick = {
            if (interactionSource != null && allowTaskTap && !trackTimeEnabled) onCardClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 12.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = task.habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.goalName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (task.habit.frequencyType) {
                            FrequencyType.DAILY -> "Daily"
                            FrequencyType.WEEKLY -> "Weekly"
                            FrequencyType.SPECIFIC_DAYS -> "${task.habit.targetCount}× per week"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    if (task.habit.frequencyType == FrequencyType.WEEKLY || task.habit.frequencyType == FrequencyType.SPECIFIC_DAYS) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (task.habit.frequencyType) {
                                FrequencyType.WEEKLY -> "${task.weekCompletionCount}/1"
                                FrequencyType.SPECIFIC_DAYS -> "${task.weekCompletionCount}/${task.habit.targetCount}"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.isWeeklyTargetReached()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
            when {
                isCompleted -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        val minutes = task.durationMinutes
                        if (minutes != null && minutes > 0) {
                            Text(
                                text = "$minutes min",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                isOngoing -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Ongoing",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Outlined.HourglassEmpty,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
