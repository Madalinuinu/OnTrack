package com.example.ontrack.ui.tracker

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.util.playTimerFinishedSound
import com.example.ontrack.util.showTimerFinishedNotification
import com.example.ontrack.ui.components.StreakBadge
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel,
    notificationsEnabled: Boolean,
    soundEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onActivityClick: () -> Unit,
    onStartTimer: (systemId: Long, habitId: Long, habitTitle: String, totalSeconds: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val system by viewModel.system.collectAsState(initial = null)
    val trackerItems by viewModel.trackerItems.collectAsState(initial = emptyList())
    val daysLeft by viewModel.daysLeft.collectAsState(initial = null)
    val currentStreak by viewModel.currentStreak.collectAsState(initial = 0)
    val isVacationDay by viewModel.isVacationDay.collectAsState(initial = false)
    val isTodayComplete by viewModel.isTodayComplete.collectAsState(initial = false)
    val freezeCount by viewModel.freezeCount.collectAsState(initial = 0)
    val activeTimer by viewModel.activeTimer.collectAsState(initial = null)
    val timerFinished by viewModel.timerFinished.collectAsState(initial = null)

    var showDurationSheet by remember { mutableStateOf(false) }
    var habitForDuration by remember { mutableStateOf<TrackerItem?>(null) }
    val context = LocalContext.current

    LaunchedEffect(timerFinished, soundEnabled, notificationsEnabled) {
        if (timerFinished != null) {
            val finished = timerFinished!!
            if (notificationsEnabled) {
                if (soundEnabled) playTimerFinishedSound(context)
                showTimerFinishedNotification(
                    context = context,
                    habitTitle = finished.habitTitle,
                    soundEnabled = soundEnabled
                )
            }
            viewModel.clearTimerFinished()
        }
    }

    LaunchedEffect(Unit) {
        val pending = com.example.ontrack.PendingTimer.consume()
        if (pending != null && pending.systemId == viewModel.systemId) {
            onStartTimer(viewModel.systemId, pending.habitId, pending.habitTitle, pending.totalSeconds)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(system?.name ?: "Tracking") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier.clickable(onClick = onActivityClick)
                    ) {
                        StreakBadge(
                            streak = currentStreak,
                            isTodayComplete = isTodayComplete,
                            lastStreakDateEpoch = system?.lastStreakDate ?: -1L,
                            freezeCount = freezeCount,
                            isVacation = isVacationDay
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (system == null) {
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
                    .padding(horizontal = 24.dp)
            ) {
                val todayFormatted = com.example.ontrack.util.EffectiveDate.today().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
                Text(
                    text = todayFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (trackerItems.isEmpty()) {
                    Text(
                        text = "Nothing to track today for this system.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(trackerItems, key = { it.habit.id }) { item ->
                            TrackerRow(
                                item = item,
                                daysLeft = daysLeft,
                                activeTimer = null,
                                onClick = {
                                    if (item.isCompletedToday) return@TrackerRow
                                    viewModel.toggleHabit(item.habit.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Track time via timers is disabled in Tracker; tapping a row just toggles completion.
}

private fun frequencyLabel(item: TrackerItem): String = when (item.habit.frequencyType) {
    FrequencyType.DAILY -> "Daily"
    FrequencyType.WEEKLY -> "Weekly"
    FrequencyType.SPECIFIC_DAYS -> "${item.habit.targetCount}× per week"
}

@Composable
private fun TrackerRow(
    item: TrackerItem,
    daysLeft: Int?,
    activeTimer: ActiveTimer?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = item.isCompletedToday
    val isRunning = activeTimer != null
    val backgroundColor = when {
        isCompleted -> androidx.compose.ui.graphics.Color(0xFF58CC02)
        isRunning -> androidx.compose.ui.graphics.Color(0xFF58CCE8)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val contentColor = if (isCompleted || isRunning) {
        androidx.compose.ui.graphics.Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            Text(
                text = item.habit.title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier.fillMaxWidth()
            )
            if (activeTimer != null) {
                Text(
                    text = "⏱ ${activeTimer.formattedTime()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = frequencyLabel(item),
                style = MaterialTheme.typography.labelMedium,
                color = if (isCompleted || isRunning) contentColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            item.progressSubtitle()?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompleted || isRunning) contentColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (daysLeft != null) {
                Text(
                    text = "$daysLeft days left",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCompleted || isRunning) contentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

