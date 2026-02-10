package com.example.ontrack.ui.activity

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ontrack.ui.components.StreakBadge
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.HabitLogEntity

private val CalendarCols = 7
private val GreenDone = Color(0xFF58CC02)
private val BlueToday = Color(0xFF58CCE8)
private val OrangePaused = Color(0xFFFF9500)
private val GrayUpcoming = Color(0xFF3C3C3E)
private val GrayPast = Color(0xFF2C2C2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDayEpoch by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            ) {
                HeaderRow(
                    systemGoal = uiState.systemGoal,
                    totalDaysCompleted = uiState.totalDaysCompleted,
                    currentStreak = uiState.currentStreak,
                    freezeCount = uiState.freezeCount,
                    isTodayComplete = uiState.isTodayComplete
                )

                MonthNavigation(
                    selectedMonth = selectedMonth,
                    onPrevMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                    onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                )

                WeekDayHeaders(modifier = Modifier.fillMaxWidth())
                MonthCalendarGrid(
                    selectedMonth = selectedMonth,
                    todayEpoch = uiState.todayEpoch,
                    completedEpochDays = uiState.completedEpochDays,
                    pausedEpochDays = uiState.pausedEpochDays,
                    onDayClick = { epochDay -> selectedDayEpoch = epochDay },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Legend(modifier = Modifier.padding(top = 20.dp))
                if (uiState.habits.isNotEmpty()) {
                    ProgressSection(
                        habits = uiState.habits,
                        totalMinutesByHabitId = uiState.totalMinutesByHabitId,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }

    selectedDayEpoch?.let { epochDay ->
        val date = LocalDate.ofEpochDay(epochDay)
        val dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"))
        val logsForDay = uiState.logs.filter { it.date == epochDay }
        ModalBottomSheet(
            onDismissRequest = { selectedDayEpoch = null },
            sheetState = sheetState
        ) {
            DayDetailSheet(
                dateLabel = dateLabel,
                habits = uiState.habits,
                logsForDay = logsForDay,
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
    freezeCount: Int,
    isTodayComplete: Boolean,
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
            freezeCount = freezeCount
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

                    DayCell(
                        dayLabel = if (validDay) "$dayNum" else "",
                        isCompleted = isCompleted,
                        isToday = isToday,
                        isUpcoming = isUpcoming,
                        isPaused = isPaused,
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
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCompleted -> GreenDone
        isPaused -> OrangePaused
        isToday -> BlueToday
        isUpcoming -> GrayUpcoming
        else -> GrayPast
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
        LegendItem(color = BlueToday, label = "Today")
        LegendItem(color = OrangePaused, label = "Paused")
        LegendItem(color = GrayUpcoming, label = "Upcoming")
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
    logsForDay: List<HabitLogEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logByHabit = logsForDay.associateBy { it.habitId }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Habituri în această zi",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        habits.forEach { habit ->
            val log = logByHabit[habit.id]
            val done = log?.isCompleted == true
            val minutes = log?.durationMinutes
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (done) "Realizat" else "Încă nu a fost realizat",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (done) GreenDone else BlueToday
                )
                if (minutes != null && minutes > 0) {
                    Text(
                        text = "${minutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        Text(
            text = "Raport – timp per habit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
        habits.forEach { habit ->
            val log = logByHabit[habit.id]
            val minutes = log?.durationMinutes ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (minutes > 0) "${minutes} min" else "–",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.TextButton(onClick = onDismiss) {
            Text("Închide")
        }
    }
}

@Composable
private fun ProgressSection(
    habits: List<HabitEntity>,
    totalMinutesByHabitId: Map<Long, Int>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Progres sistem – total timp",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        habits.forEach { habit ->
            val totalMin = totalMinutesByHabitId[habit.id] ?: 0
            val hours = totalMin / 60.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "%.1f h".format(hours),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
