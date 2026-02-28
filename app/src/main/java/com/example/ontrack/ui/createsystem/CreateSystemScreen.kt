package com.example.ontrack.ui.createsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ontrack.data.local.entity.FrequencyType
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSystemScreen(
    viewModel: CreateSystemViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddHabitSheet by remember { mutableStateOf(false) }
    var editHabitIndex by remember { mutableStateOf<Int?>(null) }
    var step by remember { mutableStateOf(1) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            viewModel.setNavigateBackHandled()
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (step == 2) "Back" else "New Goal",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (step == 2) step = 1 else onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (step == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                Text(
                    text = "Name",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.systemGoal,
                    onValueChange = viewModel::updateGoal,
                    placeholder = { Text("e.g. Study for university", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        cursorColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Duration (optional)",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.duration,
                    onValueChange = viewModel::updateDuration,
                    placeholder = { Text("e.g. 30 or 90", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    suffix = {
                        if (uiState.duration.isNotBlank()) {
                            Text(
                                " days",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = Color.Gray
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        cursorColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(80.dp))

                Button(
                    onClick = { step = 2 },
                    enabled = uiState.systemGoal.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = if (uiState.systemGoal.isNotBlank()) {
                        ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text("Next", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                var displayHabits by remember { mutableStateOf(uiState.habits) }
                LaunchedEffect(uiState.habits) { displayHabits = uiState.habits }
                val haptic = LocalHapticFeedback.current
                val lazyListState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    displayHabits = displayHabits.toMutableList().apply { add(to.index, removeAt(from.index)) }
                    viewModel.reorderHabits(from.index, to.index)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        text = "Habits",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (displayHabits.isEmpty()) {
                        Text(
                            text = "Add at least one habit (required).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = lazyListState,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(displayHabits, key = { _, h -> h.id }) { index, habit ->
                                    ReorderableItem(reorderableState, key = habit.id) { isDragging ->
                                        val interactionSource = remember { MutableInteractionSource() }
                                        HabitRow(
                                            habit = habit,
                                            index = index,
                                            onClick = { editHabitIndex = index; showAddHabitSheet = true },
                                            onRemove = { viewModel.removeHabit(index) },
                                            modifier = with(this) {
                                                Modifier
                                                    .fillMaxWidth()
                                                    .animateItem()
                                                    .longPressDraggableHandle(
                                                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                                        interactionSource = interactionSource
                                                    )
                                            },
                                            interactionSource = interactionSource,
                                            isDragging = isDragging
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    ModernAddButton(
                        onClick = { editHabitIndex = null; showAddHabitSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val canFinish = !uiState.isSaving && uiState.systemGoal.isNotBlank() && uiState.habits.isNotEmpty()
                        Button(
                            onClick = viewModel::createSystem,
                            enabled = canFinish,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (canFinish) {
                                ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary)
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text("Finish", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddHabitSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddHabitSheet = false; editHabitIndex = null },
            sheetState = sheetState
        ) {
            AddHabitSheet(
                onDismiss = { showAddHabitSheet = false; editHabitIndex = null },
                onAdd = { viewModel.addHabit(it); showAddHabitSheet = false; editHabitIndex = null },
                initial = editHabitIndex?.let { uiState.habits.getOrNull(it) },
                onUpdate = editHabitIndex?.let { idx ->
                    { item -> viewModel.updateHabit(idx, item); showAddHabitSheet = false; editHabitIndex = null }
                }
            )
        }
    }
}

@Composable
private fun ModernAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Habit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
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
                    text = "Add Habit",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Create a new habit to track",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun HabitRow(
    habit: HabitItem,
    index: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    isDragging: Boolean = false
) {
    Card(
        modifier = modifier.then(
            if (interactionSource == null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        onClick = { if (interactionSource != null) onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 12.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = Color.Black
                )
                Text(
                    text = when (habit.frequencyType) {
                        FrequencyType.DAILY -> "Daily"
                        FrequencyType.WEEKLY -> "Weekly"
                        FrequencyType.SPECIFIC_DAYS -> "${habit.targetCount}× per week"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = Color.Gray
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.Gray
                )
            }
        }
    }
}
