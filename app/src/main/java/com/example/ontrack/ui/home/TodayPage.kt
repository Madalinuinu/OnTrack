package com.example.ontrack.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ontrack.data.local.entity.FrequencyType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.ontrack.ui.tracker.DurationPickerSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayPage(
    viewModel: HomeViewModel,
    trackTimeEnabled: Boolean,
    onStartTimer: (systemId: Long, habitId: Long, habitTitle: String, totalSeconds: Int) -> Unit,
    onCompleteHabit: (systemId: Long, habitId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val systems by viewModel.systems.collectAsState(initial = emptyList())
    val todayTasksFiltered by viewModel.todayTasksFiltered.collectAsState(initial = emptyList())
    val filterGoalIds by viewModel.todayFilterGoalIds.collectAsState(initial = null)
    val todayActiveTimer by viewModel.todayActiveTimer.collectAsState(initial = null)
    var taskForTimer by remember { mutableStateOf<TodayTaskItem?>(null) }
    var selectedHours by remember { mutableIntStateOf(0) }
    var selectedMinutes by remember { mutableIntStateOf(3) }
    var selectedSeconds by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        val selectedDate by viewModel.selectedDate.collectAsState(initial = LocalDate.now())
        val today = LocalDate.now()
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
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
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
            systems.forEach { system ->
                FilterChip(
                    label = system.goal,
                    selected = filterGoalIds?.contains(system.id) == true,
                    onClick = { viewModel.toggleTodayFilterGoal(system.id) }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        val isToday = selectedDate == LocalDate.now()
        if (todayTasksFiltered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val emptyMessage = when {
                    !isToday && selectedDate.isBefore(LocalDate.now()) -> "No completions on this day."
                    !isToday && selectedDate.isAfter(LocalDate.now()) -> "Nothing due on this day."
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(todayTasksFiltered, key = { it.habit.id }) { item ->
                    val timerForThis = todayActiveTimer?.takeIf { it.habitId == item.habit.id }
                    TodayTaskCard(
                        task = item,
                        trackTimeEnabled = trackTimeEnabled,
                        activeTimer = timerForThis,
                        isInteractiveDay = isToday,
                        onCardClick = {
                            if (!trackTimeEnabled) onCompleteHabit(item.habit.systemId, item.habit.id)
                        },
                        onStartClick = {
                            taskForTimer = item
                            selectedHours = 0
                            selectedMinutes = 3
                            selectedSeconds = 0
                        },
                        onPauseClick = { viewModel.pauseTodayTimer() },
                        onResumeClick = { viewModel.resumeTodayTimer() }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (taskForTimer != null) {
        ModalBottomSheet(
            onDismissRequest = { taskForTimer = null },
            sheetState = sheetState
        ) {
            DurationPickerSheet(
                habitTitle = taskForTimer!!.habit.title,
                selectedHours = selectedHours,
                selectedMinutes = selectedMinutes,
                selectedSeconds = selectedSeconds,
                onHoursChange = { selectedHours = it },
                onMinutesChange = { selectedMinutes = it },
                onSecondsChange = { selectedSeconds = it },
                onStart = {
                    val totalSeconds = selectedHours * 3600 + selectedMinutes * 60 + selectedSeconds
                    if (totalSeconds > 0) {
                        onStartTimer(
                            taskForTimer!!.habit.systemId,
                            taskForTimer!!.habit.id,
                            taskForTimer!!.habit.title,
                            totalSeconds.coerceAtLeast(1)
                        )
                        taskForTimer = null
                    }
                },
                onCancel = { taskForTimer = null }
            )
        }
    }
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
            .clickable(onClick = onClick),
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else Color.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TodayTaskCard(
    task: TodayTaskItem,
    trackTimeEnabled: Boolean,
    activeTimer: com.example.ontrack.ui.home.TodayActiveTimer?,
    isInteractiveDay: Boolean,
    onCardClick: () -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = task.isCompletedToday
    val isTimerRunning = activeTimer != null && !activeTimer.isPaused
    val isTimerPaused = activeTimer != null && activeTimer.isPaused

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isInteractiveDay && !trackTimeEnabled) Modifier.clickable(onClick = onCardClick)
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
            if (trackTimeEnabled || isCompleted || task.isWeeklyTargetReached()) {
                when {
                    isCompleted -> {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = task.durationMinutes?.let { "${it} min" } ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    task.isWeeklyTargetReached() && !isCompleted -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tap to add more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            if (isInteractiveDay && trackTimeEnabled) {
                                Button(
                                    onClick = onStartClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Start")
                                }
                            }
                        }
                    }
                    isInteractiveDay && (isTimerRunning || isTimerPaused) -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = if (isTimerPaused) onResumeClick else onPauseClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (isTimerPaused) "Resume" else "Pause")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeTimer?.formattedTime() ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    isInteractiveDay -> {
                        Button(
                            onClick = onStartClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Start")
                        }
                    }
                    else -> { }
                }
            }
        }
    }
}
