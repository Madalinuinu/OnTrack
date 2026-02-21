package com.example.ontrack.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.ui.components.StreakBadge
import com.example.ontrack.ui.home.daysLeft

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String,
    trackTimeEnabled: Boolean,
    onTrackTimeEnabledChange: (Boolean) -> Unit,
    skipOnboardingEnabled: Boolean,
    onSkipOnboardingEnabledChange: (Boolean) -> Unit,
    initialPage: Int,
    onConsumeInitialPage: () -> Unit,
    onCreateSystemClick: () -> Unit,
    onOpenSystemClick: (Long) -> Unit,
    onActivityClick: (Long) -> Unit,
    onEditSystemClick: (Long) -> Unit,
    onStartTimerFromToday: (systemId: Long, habitId: Long, habitTitle: String, totalSeconds: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val systems by viewModel.systems.collectAsState(initial = emptyList())
    val selectedSystemId by viewModel.selectedSystemId.collectAsState(initial = null)
    val expiredDialogSystemId by viewModel.expiredDialogSystemId.collectAsState(initial = null)
    val todayCompleteMap by viewModel.todayCompleteMap.collectAsState(initial = emptyMap())
    val freezeCountMap by viewModel.freezeCountMap.collectAsState(initial = emptyMap())
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        onConsumeInitialPage()
        viewModel.refreshTodayComplete()
    }

    val selectedSystem = remember(selectedSystemId, systems) {
        selectedSystemId?.let { id -> systems.find { it.id == id } }
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
    if (showDeleteConfirm && selectedSystem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete goal?") },
            text = {
                Text(
                    "Are you sure you want to delete this goal? Your streak is ${selectedSystem.currentStreak} days."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSystem(selectedSystem.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
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
                        text = "Track time",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                    Switch(
                        checked = trackTimeEnabled,
                        onCheckedChange = onTrackTimeEnabledChange
                    )
                }
                Text(
                    text = if (trackTimeEnabled) "When you tap a task, the timer appears." else "When you tap a task, it completes directly.",
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
                    onClick = { scope.launch { drawerState.open() } }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
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
                    0 -> ChallengesPage(
                        systems = systems,
                        selectedSystemId = selectedSystemId,
                        todayCompleteMap = todayCompleteMap,
                        freezeCountMap = freezeCountMap,
                        viewModel = viewModel,
                        showDeleteConfirm = { showDeleteConfirm = true },
                        onCreateSystemClick = onCreateSystemClick,
                        onOpenSystemClick = onOpenSystemClick,
                        onActivityClick = onActivityClick,
                        onEditSystemClick = onEditSystemClick
                    )
                    1 -> TodayPage(
                        viewModel = viewModel,
                        trackTimeEnabled = trackTimeEnabled,
                        onStartTimer = onStartTimerFromToday,
                        onCompleteHabit = { systemId, habitId -> viewModel.completeHabitToday(systemId, habitId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengesPage(
    systems: List<SystemEntity>,
    selectedSystemId: Long?,
    todayCompleteMap: Map<Long, Boolean>,
    freezeCountMap: Map<Long, Int>,
    viewModel: HomeViewModel,
    showDeleteConfirm: () -> Unit,
    onCreateSystemClick: () -> Unit,
    onOpenSystemClick: (Long) -> Unit,
    onActivityClick: (Long) -> Unit,
    onEditSystemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CHALLENGES",
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
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (systems.isEmpty()) {
            GoalsEmptyState(
                onAddGoalClick = onCreateSystemClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    systems.mapIndexed { index, system -> index to system },
                    key = { it.second.id }
                ) { (index, system) ->
                    SystemCard(
                        system = system,
                        index = index,
                        listSize = systems.size,
                        isSelected = selectedSystemId == system.id,
                        isTodayComplete = todayCompleteMap[system.id] ?: false,
                        freezeCount = freezeCountMap[system.id] ?: 0,
                        onCardClick = {
                            viewModel.selectSystem(null)
                            onEditSystemClick(system.id)
                        },
                        onLongClick = { viewModel.selectSystem(system.id) },
                        onOpenClick = { viewModel.selectSystem(null); onEditSystemClick(system.id) },
                        onActivityClick = { viewModel.selectSystem(null); onActivityClick(system.id) },
                        onDeleteClick = showDeleteConfirm,
                        onMoveUp = { if (index > 0) viewModel.reorderSystems(index, index - 1) },
                        onMoveDown = { if (index < systems.size - 1) viewModel.reorderSystems(index, index + 1) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AddGoalCard(
                        onClick = onCreateSystemClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SystemCard(
    system: SystemEntity,
    index: Int,
    listSize: Int,
    isSelected: Boolean,
    isTodayComplete: Boolean,
    freezeCount: Int = 0,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpenClick: () -> Unit,
    onActivityClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysLeft = system.daysLeft()
    val cardColor = Color.White
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCardClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = system.goal,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onActivityClick
                        )
                    ) {
                        StreakBadge(
                            streak = system.currentStreak,
                            isTodayComplete = isTodayComplete,
                            freezeCount = freezeCount
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        IconButton(onClick = onMoveUp, enabled = index > 0) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(onClick = onMoveDown, enabled = index < listSize - 1) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete goal")
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Button(onClick = onActivityClick) {
                        Text("Activity")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onOpenClick) {
                        Text("Edit")
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
                        .padding(12.dp)
                )
            }
        }
    }
}
