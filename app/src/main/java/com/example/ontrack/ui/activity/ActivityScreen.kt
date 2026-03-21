package com.example.ontrack.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ontrack.ui.components.StreakBadge
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.HabitLogEntity
import com.example.ontrack.util.EffectiveDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val CalendarCols = 7
private val GreenDone = Color(0xFF58CC02)
private val BlueToday = Color(0xFF58CCE8)
private val OrangePaused = Color(0xFFFF9500)
private val GrayUpcoming = Color(0xFF3C3C3E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMonth by remember { mutableStateOf(YearMonth.from(EffectiveDate.today())) }
    var selectedDayEpoch by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        selectedMonth = YearMonth.from(EffectiveDate.today())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Activity") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                HeaderRow(
                    systemGoal = uiState.systemGoal,
                    totalDaysCompleted = uiState.totalDaysCompleted,
                    currentStreak = uiState.currentStreak,
                    lastStreakDateEpoch = uiState.lastStreakDateEpoch,
                    freezeCount = uiState.freezeCount,
                    isTodayComplete = uiState.isTodayComplete,
                    isVacationDay = uiState.isVacationDay
                )

                CalendarSwipeArea(
                    selectedMonth = selectedMonth,
                    onPrevMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                    onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
                    todayEpoch = uiState.todayEpoch,
                    firstCompletedEpoch = uiState.firstCompletedEpoch,
                    completedEpochDays = uiState.completedEpochDays,
                    pausedEpochDays = uiState.pausedEpochDays,
                    onDayClick = { epochDay -> selectedDayEpoch = epochDay }
                )
                Legend(modifier = Modifier.padding(top = 20.dp))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    selectedDayEpoch?.let { epochDay ->
        val date = LocalDate.ofEpochDay(epochDay)
        val dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"))
        ModalBottomSheet(
            onDismissRequest = { selectedDayEpoch = null },
            sheetState = sheetState
        ) {
            DayDetailSheet(
                dateLabel = dateLabel,
                habits = uiState.habits,
                epochDay = epochDay,
                allLogs = uiState.logs,
                onDismiss = { selectedDayEpoch = null }
            )
        }
    }
}

@Composable
private fun HeaderRow(
    systemGoal: String,
    totalDaysCompleted: Int,
    currentStreak: Int,
    lastStreakDateEpoch: Long,
    freezeCount: Int,
    isTodayComplete: Boolean,
    isVacationDay: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (systemGoal.isNotBlank()) {
                Text(
                    text = systemGoal,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$totalDaysCompleted days done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        StreakBadge(
            streak = currentStreak,
            isTodayComplete = isTodayComplete,
            lastStreakDateEpoch = lastStreakDateEpoch,
            freezeCount = freezeCount,
            snowflakeTint = Color(0xFFB8E6F4),
            fireTint = Color(0xFFFF6B35),
            isVacation = isVacationDay
        )
    }
}

@Composable
private fun CalendarSwipeArea(
    selectedMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    todayEpoch: Long,
    firstCompletedEpoch: Long?,
    completedEpochDays: Set<Long>,
    pausedEpochDays: Set<Long>,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 50.dp.toPx() }
    var accumulatedDrag by remember { mutableStateOf(0f) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(selectedMonth) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                    },
                    onDragEnd = {
                        when {
                            accumulatedDrag > swipeThresholdPx -> onPrevMonth()
                            accumulatedDrag < -swipeThresholdPx -> onNextMonth()
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f }
                )
            }
    ) {
        MonthNavigation(
            selectedMonth = selectedMonth,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )
        WeekDayHeaders(modifier = Modifier.fillMaxWidth())
        MonthCalendarGrid(
            selectedMonth = selectedMonth,
            todayEpoch = todayEpoch,
            firstCompletedEpoch = firstCompletedEpoch,
            completedEpochDays = completedEpochDays,
            pausedEpochDays = pausedEpochDays,
            onDayClick = onDayClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun MonthNavigation(
    selectedMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthYearText = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = monthYearText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row {
            IconButton(onClick = onPrevMonth) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Previous month"
                )
            }
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Next month"
                )
            }
        }
    }
}

@Composable
private fun WeekDayHeaders(modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(top = 8.dp)) {
        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    selectedMonth: YearMonth,
    todayEpoch: Long,
    firstCompletedEpoch: Long?,
    completedEpochDays: Set<Long>,
    pausedEpochDays: Set<Long>,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDay = selectedMonth.atDay(1)
    val lastDayNum = selectedMonth.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.getValue() % 7
    val totalCells = startOffset + lastDayNum
    val calendarRows = (totalCells + CalendarCols - 1) / CalendarCols

    Column(modifier = modifier) {
        for (row in 0 until calendarRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until CalendarCols) {
                    val cellIndex = row * CalendarCols + col
                    val dayNum = if (cellIndex >= startOffset) (cellIndex - startOffset + 1) else null
                    val validDay = dayNum != null && dayNum <= lastDayNum
                    val date = if (validDay) selectedMonth.atDay(dayNum!!) else null
                    val epochDay = date?.toEpochDay() ?: -1L
                    val isToday = epochDay == todayEpoch
                    val isUpcoming = validDay && epochDay > todayEpoch
                    val isCompleted = validDay && epochDay in completedEpochDays
                    val isPaused = validDay && epochDay in pausedEpochDays
                    val isBeforeFirstGreen = validDay && (
                        (firstCompletedEpoch == null && epochDay < todayEpoch) ||
                        (firstCompletedEpoch != null && epochDay < firstCompletedEpoch)
                    )

                    DayCell(
                        dayLabel = if (validDay) "$dayNum" else "",
                        isCompleted = isCompleted,
                        isToday = isToday,
                        isUpcoming = isUpcoming,
                        isPaused = isPaused,
                        isBeforeFirstGreen = isBeforeFirstGreen,
                        onClick = if (validDay) ({ onDayClick(epochDay) }) else null,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayLabel: String,
    isCompleted: Boolean,
    isToday: Boolean,
    isUpcoming: Boolean,
    isPaused: Boolean = false,
    isBeforeFirstGreen: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isEmpty = dayLabel.isBlank()
    val backgroundColor = when {
        isEmpty -> GrayUpcoming
        isCompleted -> GreenDone
        isPaused -> OrangePaused
        isToday -> BlueToday
        isUpcoming -> GrayUpcoming
        isBeforeFirstGreen -> GrayUpcoming
        else -> BlueToday
    }
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (dayLabel.isNotBlank()) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = GreenDone, label = "Done")
        LegendItem(color = BlueToday, label = "Skipped")
        LegendItem(color = GrayUpcoming, label = "Upcoming")
        LegendItem(color = OrangePaused, label = "Vacation")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 6.dp)
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayDetailSheet(
    dateLabel: String,
    habits: List<HabitEntity>,
    epochDay: Long,
    allLogs: List<HabitLogEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val date = LocalDate.ofEpochDay(epochDay)
    val dayOfWeek = date.dayOfWeek.value
    val weekStartEpoch = epochDay - (dayOfWeek - 1)
    val weekEndEpoch = weekStartEpoch + 6
    val logsForDay = allLogs.filter { it.date == epochDay }
    val logByHabit = logsForDay.associateBy { it.habitId }
    val weekLogsByHabit = allLogs
        .filter { it.date in weekStartEpoch..weekEndEpoch && it.isCompleted }
        .groupBy { it.habitId }
        .mapValues { (_, list) -> list.distinctBy { it.date }.size }
    val textPrimary = Color.Black
    val textSecondary = Color(0xFF424242)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleLarge,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Habits on this day",
            style = MaterialTheme.typography.titleMedium,
            color = textPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )
        habits.forEach { habit ->
            val log = logByHabit[habit.id]
            val done = when (habit.frequencyType) {
                FrequencyType.DAILY -> log?.isCompleted == true
                FrequencyType.WEEKLY -> (weekLogsByHabit[habit.id] ?: 0) >= 1
                FrequencyType.SPECIFIC_DAYS -> (weekLogsByHabit[habit.id] ?: 0) >= habit.targetCount
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (done) "Done" else "Not done yet",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (done) GreenDone else BlueToday
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.TextButton(onClick = onDismiss) {
            Text("Close", color = textPrimary)
        }
    }
}

@Composable
private fun HabitTimeRow(
    habitName: String,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val timeStr = if (totalMinutes < 60)
        "%.2f h".format(totalMinutes / 60.0)
    else
        "%.1f h".format(totalMinutes / 60.0)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habitName,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = timeStr,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = Color.Black
            )
        }
    }
}
