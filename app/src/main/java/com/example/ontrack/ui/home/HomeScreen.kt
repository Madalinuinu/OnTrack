package com.example.ontrack.ui.home

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Button
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberDrawerState
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ontrack.data.local.entity.SystemEntity
import androidx.activity.ComponentActivity
import com.example.ontrack.util.EXTRA_SCROLL_TO_TODAY
import com.example.ontrack.util.playTimerFinishedSound
import com.example.ontrack.util.showTimerFinishedNotification
import com.example.ontrack.ui.components.StreakBadge
import com.example.ontrack.ui.components.streakBadgeDisplayNumber
import com.example.ontrack.ui.home.daysLeft
import com.example.ontrack.util.SleepReminderScheduler
import sh.calvin.reorderable.ReorderableItem
import java.util.Locale
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String,
    skipOnboardingEnabled: Boolean,
    onSkipOnboardingEnabledChange: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    vacationModeEnabled: Boolean,
    vacationModeFromEpochDay: Long,
    persistedVacationEpochDays: Set<Long>,
    onVacationModeEnabledChange: (Boolean) -> Unit,
    initialPage: Int,
    onConsumeInitialPage: () -> Unit,
    onCreateSystemClick: () -> Unit,
    onOpenSystemClick: (Long) -> Unit,
    onActivityClick: (Long) -> Unit,
    onEditSystemClick: (Long) -> Unit,
    onYourStatsClick: () -> Unit = {},
    sleepBedtimeMinutes: Int = -1,
    sleepWakeMinutes: Int = -1,
    onSetSleepTimes: (bedtimeMinutes: Int, wakeMinutes: Int) -> Unit = { _, _ -> },
    onStartTimerFromToday: (systemId: Long, habitId: Long, habitTitle: String, totalSeconds: Int) -> Unit,
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val systems by viewModel.systems.collectAsState(initial = emptyList())
    val expiredDialogSystemId by viewModel.expiredDialogSystemId.collectAsState(initial = null)
    val todayCompleteMap by viewModel.todayCompleteMap.collectAsState(initial = emptyMap())
    val freezeCountMap by viewModel.freezeCountMap.collectAsState(initial = emptyMap())
    val globalStreakDays by viewModel.globalStreakDays.collectAsState(initial = 0)
    val globalLastStreakDateEpoch by viewModel.globalLastStreakDateEpoch.collectAsState(initial = -1L)
    val allGoalsCompleteToday by viewModel.allGoalsCompleteToday.collectAsState(initial = false)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var drawerScreen by remember { mutableStateOf("menu") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, pagerState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val act = context as? ComponentActivity ?: return@LifecycleEventObserver
                if (act.intent.getBooleanExtra(EXTRA_SCROLL_TO_TODAY, false)) {
                    scope.launch { pagerState.animateScrollToPage(1) }
                    act.intent.removeExtra(EXTRA_SCROLL_TO_TODAY)
                    act.setIntent(act.intent)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val todayTimerFinished by viewModel.todayTimerFinished.collectAsState(initial = null)

    LaunchedEffect(todayTimerFinished, soundEnabled, notificationsEnabled) {
        if (todayTimerFinished != null) {
            val finished = todayTimerFinished!!
            if (notificationsEnabled) {
                if (soundEnabled) playTimerFinishedSound(context)
                showTimerFinishedNotification(
                    context = context,
                    habitTitle = finished.habitTitle,
                    soundEnabled = soundEnabled
                )
            }
            viewModel.clearTodayTimerFinished()
        }
    }

    LaunchedEffect(Unit) {
        onConsumeInitialPage()
        viewModel.refreshTodayComplete()
    }

    val expiredSystem = expiredDialogSystemId?.let { id -> systems.find { it.id == id } }
    if (expiredSystem != null) {
        Dialog(onDismissRequest = { }) {
            Card(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${expiredSystem.goal} time has passed",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "The time for this goal has passed. What would you like to do?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.onExpiredDialogContinue(expiredSystem.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Continue")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { viewModel.onExpiredDialogDelete(expiredSystem.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    }
                }
            }
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                windowInsets = WindowInsets(top = 0.dp)
            ) {
                val todayEpoch = com.example.ontrack.util.EffectiveDate.todayEpoch()
                val isVacationDay = vacationModeEnabled && vacationModeFromEpochDay >= 0 && todayEpoch >= vacationModeFromEpochDay
                if (drawerScreen == "menu") {
                    DrawerHeader(
                        globalStreakDays = globalStreakDays,
                        allGoalsCompleteToday = allGoalsCompleteToday,
                        globalLastStreakDateEpoch = globalLastStreakDateEpoch,
                        todayEpoch = todayEpoch,
                        isVacationDay = isVacationDay
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    drawerState.close()
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Goals",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    drawerState.close()
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    drawerState.close()
                                    onYourStatsClick()
                                }
                            }
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShowChart,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Your stats",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { drawerScreen = "sleep_reminder" }
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Sleep reminder",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { drawerScreen = "settings" }
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                } else if (drawerScreen == "sleep_reminder") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { drawerScreen = "menu" }
                            .padding(horizontal = 28.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Sleep reminder",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SleepReminderDrawerContent(
                        sleepBedtimeMinutes = sleepBedtimeMinutes,
                        sleepWakeMinutes = sleepWakeMinutes,
                        onSetSleepTimes = onSetSleepTimes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { drawerScreen = "menu" }
                            .padding(horizontal = 28.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skip onboarding screen",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                        Switch(
                            checked = skipOnboardingEnabled,
                            onCheckedChange = onSkipOnboardingEnabledChange
                        )
                    }
Text(
                            text = "Skip the onboarding screen to open the app faster",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                        )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vacation mode",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                        Switch(
                            checked = vacationModeEnabled,
                            onCheckedChange = onVacationModeEnabledChange
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onNotificationsEnabledChange
                        )
                    }
                    Text(
                        text = "Show notification when timer ends",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sound",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = onSoundEnabledChange,
                            enabled = notificationsEnabled
                        )
                    }
                    Text(
                        text = if (notificationsEnabled) {
                            "Play sound and vibration when timer ends"
                        } else {
                            "Enable notifications first to use sound and vibration"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                    )
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        drawerScreen = "menu"
                        scope.launch { drawerState.open() }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> GoalsPage(
                        systems = systems,
                        todayCompleteMap = todayCompleteMap,
                        freezeCountMap = freezeCountMap,
                        viewModel = viewModel,
                        isGoalsTabVisible = pagerState.currentPage == 0,
                        onCreateSystemClick = onCreateSystemClick,
                        onOpenGoalInToday = { systemId ->
                            viewModel.setTodayFilter(setOf(systemId))
                            viewModel.setSelectedDate(com.example.ontrack.util.EffectiveDate.today())
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onEditSystemClick = onEditSystemClick
                    )
                    1 -> TodayPage(
                        viewModel = viewModel,
                        onStartTimer = onStartTimerFromToday,
                        onPauseTimer = onPauseTimer,
                        onResumeTimer = onResumeTimer,
                        vacationModeEnabled = vacationModeEnabled,
                        vacationModeFromEpochDay = vacationModeFromEpochDay,
                        persistedVacationEpochDays = persistedVacationEpochDays
                    )
                }
            }
        }
    }
}

private val VacationOrange = Color(0xFFFF9500)

@Composable
private fun DrawerHeader(
    globalStreakDays: Int,
    allGoalsCompleteToday: Boolean,
    globalLastStreakDateEpoch: Long,
    todayEpoch: Long,
    isVacationDay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayCount = streakBadgeDisplayNumber(
        streak = globalStreakDays,
        isTodayComplete = allGoalsCompleteToday,
        lastStreakDateEpoch = globalLastStreakDateEpoch,
        todayEpoch = todayEpoch
    )
    val dayLabel = if (displayCount == 1) "Day" else "Days"
    val iconTint = when {
        isVacationDay -> VacationOrange
        allGoalsCompleteToday -> Color(0xFFFF6B35)
        else -> Color(0xFF58CCE8)
    }
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = scheme.primary,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$displayCount $dayLabel",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = scheme.onPrimary
                )
                Icon(
                    imageVector = if (allGoalsCompleteToday) Icons.Filled.LocalFireDepartment else Icons.Filled.AcUnit,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your current streak",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onPrimary.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun GoalsPage(
    systems: List<SystemEntity>,
    todayCompleteMap: Map<Long, Boolean>,
    freezeCountMap: Map<Long, Int>,
    viewModel: HomeViewModel,
    isGoalsTabVisible: Boolean,
    onCreateSystemClick: () -> Unit,
    onOpenGoalInToday: (Long) -> Unit,
    onEditSystemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var goalSelectionMode by remember { mutableStateOf(false) }
    var selectedGoalIds by remember { mutableStateOf(setOf<Long>()) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    fun clearGoalSelection() {
        goalSelectionMode = false
        selectedGoalIds = emptySet()
    }

    LaunchedEffect(isGoalsTabVisible) {
        if (!isGoalsTabVisible) clearGoalSelection()
    }

    LaunchedEffect(systems) {
        selectedGoalIds = selectedGoalIds.filter { id -> systems.any { it.id == id } }.toSet()
        if (selectedGoalIds.isEmpty()) goalSelectionMode = false
    }

    if (showBulkDeleteConfirm) {
        val deleteCount = selectedGoalIds.size
        val scheme = MaterialTheme.colorScheme
        Dialog(onDismissRequest = { showBulkDeleteConfirm = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = scheme.primary,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = if (deleteCount == 1) "Delete goal?" else "Delete goals?",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onPrimary
                        )
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = if (deleteCount == 1) {
                                "Are you sure you want to delete this goal? This cannot be undone."
                            } else {
                                "Are you sure you want to delete $deleteCount goals? This cannot be undone."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Streak and task history for these goals will be removed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.outline
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showBulkDeleteConfirm = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(2.dp, scheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = scheme.onSurface
                                )
                            ) {
                                Text("No", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    viewModel.deleteSystems(selectedGoalIds)
                                    showBulkDeleteConfirm = false
                                    clearGoalSelection()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = scheme.error,
                                    contentColor = scheme.onError
                                )
                            ) {
                                Text("Yes", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
            ) {
                if (goalSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            IconButton(onClick = { clearGoalSelection() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cancel selection",
                                    tint = Color.White
                                )
                            }
                            val n = selectedGoalIds.size
                            Text(
                                text = if (n == 1) "1 selected" else "$n selected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Button(
                            onClick = {
                                if (selectedGoalIds.isNotEmpty()) showBulkDeleteConfirm = true
                            },
                            enabled = selectedGoalIds.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp,
                                disabledElevation = 0.dp
                            )
                        ) {
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "GOALS",
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
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.3f))
                .then(
                    if (goalSelectionMode) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { clearGoalSelection() }
                    } else {
                        Modifier
                    }
                )
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (systems.isEmpty()) {
                GoalsEmptyState(
                    onAddGoalClick = onCreateSystemClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else {
                val haptic = LocalHapticFeedback.current
                var displaySystems by remember(systems) { mutableStateOf(systems) }
                LaunchedEffect(systems) {
                    displaySystems = systems
                }
                val lazyListState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    displaySystems = displaySystems.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    }
                    viewModel.reorderSystems(from.index, to.index)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    itemsIndexed(
                        displaySystems,
                        key = { _, system -> system.id }
                    ) { _, system ->
                        ReorderableItem(reorderableState, key = system.id) { isDragging ->
                            val handleInteractionSource = remember { MutableInteractionSource() }
                            val dragModifier = with(this) {
                                Modifier.longPressDraggableHandle(
                                    onDragStarted = {
                                        goalSelectionMode = true
                                        if (system.id !in selectedGoalIds) {
                                            selectedGoalIds = selectedGoalIds + system.id
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    interactionSource = handleInteractionSource
                                )
                            }
                            SystemCard(
                                modifier = Modifier.fillMaxWidth(),
                                reorderableDragModifier = dragModifier,
                                system = system,
                                isTodayComplete = todayCompleteMap[system.id] ?: false,
                                freezeCount = freezeCountMap[system.id] ?: 0,
                                isSelected = system.id in selectedGoalIds,
                                selectionMode = goalSelectionMode,
                                onOpenClick = {
                                    if (goalSelectionMode) {
                                        clearGoalSelection()
                                    }
                                    viewModel.selectSystem(null)
                                    onOpenGoalInToday(system.id)
                                },
                                onEditClick = {
                                    if (goalSelectionMode) {
                                        clearGoalSelection()
                                    }
                                    viewModel.selectSystem(null)
                                    onEditSystemClick(system.id)
                                },
                                onCardClick = {
                                    if (goalSelectionMode) {
                                        selectedGoalIds =
                                            if (system.id in selectedGoalIds) {
                                                selectedGoalIds - system.id
                                            } else {
                                                selectedGoalIds + system.id
                                            }
                                        if (selectedGoalIds.isEmpty()) goalSelectionMode = false
                                    }
                                },
                                onCardLongClick = {
                                    goalSelectionMode = true
                                    selectedGoalIds =
                                        if (system.id in selectedGoalIds) {
                                            selectedGoalIds
                                        } else {
                                            selectedGoalIds + system.id
                                        }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                isDragging = isDragging
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        AddGoalCard(
                            onClick = {
                                if (goalSelectionMode) clearGoalSelection()
                                onCreateSystemClick()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (goalSelectionMode) {
                        item(key = "selection_dismiss_area") {
                            val dismissInteraction = remember { MutableInteractionSource() }
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .clickable(
                                        interactionSource = dismissInteraction,
                                        indication = null
                                    ) { clearGoalSelection() }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsEmptyState(
    onAddGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        AddGoalCard(
            onClick = onAddGoalClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AddGoalCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Add goal",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
                Text(
                    text = "Create a new goal to track",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun SystemCard(
    system: SystemEntity,
    isTodayComplete: Boolean,
    freezeCount: Int = 0,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onOpenClick: () -> Unit,
    onEditClick: () -> Unit,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    /** Full-card long-press drag for reorder (works in normal and selection mode). */
    reorderableDragModifier: Modifier = Modifier
) {
    val daysLeft = system.daysLeft()
    val cardInteractionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .height(108.dp)
            .combinedClickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onCardClick,
                onLongClickLabel = "Select goal",
                onLongClick = onCardLongClick
            )
            .then(reorderableDragModifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 12.dp else 6.dp),
        border = if (isSelected) {
            BorderStroke(3.dp, Color(0xFF6C5CE7))
        } else {
            null
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = system.goal,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    StreakBadge(
                        streak = system.currentStreak,
                        isTodayComplete = isTodayComplete,
                        lastStreakDateEpoch = system.lastStreakDate,
                        freezeCount = freezeCount
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (!selectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onOpenClick) {
                            Text("Open")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onEditClick) {
                            Text("Edit")
                        }
                    }
                }
            }
            if (daysLeft != null) {
                Text(
                    text = "$daysLeft days left",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
        }
    }
}

private fun formatMinutesToTimeString(minutes: Int): String {
    if (minutes < 0) return "Not set"
    val hour = minutes / 60
    val minute = minutes % 60
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format(Locale.US, "%d:%02d %s", h, minute, amPm)
}

@Composable
private fun SleepReminderDrawerContent(
    sleepBedtimeMinutes: Int,
    sleepWakeMinutes: Int,
    onSetSleepTimes: (bedtimeMinutes: Int, wakeMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultBed = if (sleepBedtimeMinutes >= 0) sleepBedtimeMinutes else 23 * 60 // 11:00 PM
    val defaultWake = if (sleepWakeMinutes >= 0) sleepWakeMinutes else 7 * 60 + 30 // 7:30 AM
    Column(modifier = modifier) {
        Text(
            text = "Set your bedtime and wake-up time. You'll get notifications at these times (using device time).",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            val bedMin = hour * 60 + minute
                            val wake = if (sleepWakeMinutes >= 0) sleepWakeMinutes else defaultWake
                            onSetSleepTimes(bedMin, wake)
                            SleepReminderScheduler.schedule(context, bedMin, wake)
                        },
                        defaultBed / 60,
                        defaultBed % 60,
                        false
                    ).show()
                }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bedtime",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
            Text(
                text = formatMinutesToTimeString(if (sleepBedtimeMinutes >= 0) sleepBedtimeMinutes else defaultBed),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            val wakeMin = hour * 60 + minute
                            val bed = if (sleepBedtimeMinutes >= 0) sleepBedtimeMinutes else defaultBed
                            onSetSleepTimes(bed, wakeMin)
                            SleepReminderScheduler.schedule(context, bed, wakeMin)
                        },
                        defaultWake / 60,
                        defaultWake % 60,
                        false
                    ).show()
                }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wake up",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
            Text(
                text = formatMinutesToTimeString(if (sleepWakeMinutes >= 0) sleepWakeMinutes else defaultWake),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
