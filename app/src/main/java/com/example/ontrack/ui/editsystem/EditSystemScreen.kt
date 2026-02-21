package com.example.ontrack.ui.editsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.ui.createsystem.AddHabitSheet
import com.example.ontrack.ui.createsystem.HabitItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSystemScreen(
    viewModel: EditSystemViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var step by remember { mutableStateOf(1) }
    var showAddHabitSheet by remember { mutableStateOf(false) }
    var editHabitIndex by remember { mutableStateOf<Int?>(null) }
    var showPauseCalendar by remember { mutableStateOf(false) }
    val habitSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pauseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                title = { Text("Edit Goal", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == 2) step = 1 else onNavigateBack()
                    }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        } else {
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
                            value = uiState.goal,
                            onValueChange = viewModel::updateGoal,
                            placeholder = { Text("e.g. Study for university", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.fillMaxWidth(),
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
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Pause goal (e.g. vacation)",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to select period (e.g. vacation). Paused days show in orange in Activity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            onClick = { showPauseCalendar = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = when {
                                    uiState.pausedFromDate.isNotBlank() && uiState.pausedToDate.isNotBlank() ->
                                        "${uiState.pausedFromDate} — ${uiState.pausedToDate}"
                                    uiState.pausedFromDate.isNotBlank() -> "From: ${uiState.pausedFromDate} (tap to set To)"
                                    else -> "Select pause period (From → To)"
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = if (uiState.pausedFromDate.isNotBlank() || uiState.pausedToDate.isNotBlank()) Color.Black else Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(80.dp))

                        Button(
                            onClick = { step = 2 },
                            enabled = uiState.goal.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (uiState.goal.isNotBlank()) {
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
                    val hasFiveOrMore = uiState.habits.size >= 5
                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Column(modifier = Modifier.weight(0.85f).fillMaxWidth()) {
                            if (hasFiveOrMore) {
                                Column(
                                    modifier = Modifier
                                        .height(360.dp)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "Habits",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        uiState.habits.forEachIndexed { index, habit ->
                                            HabitRow(
                                                habit = habit,
                                                onClick = { editHabitIndex = index; showAddHabitSheet = true },
                                                onRemove = { viewModel.removeHabit(index) }
                                            )
                                            if (index < uiState.habits.size - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 4.dp),
                                                    color = Color.White,
                                                    thickness = 1.dp
                                                )
                                            }
                                        }
                                    }
                                }
                                ModernAddButton(
                                    onClick = { editHabitIndex = null; showAddHabitSheet = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "Habits",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (uiState.habits.isEmpty()) {
                                        Text(
                                            text = "Add at least one habit (required).",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            uiState.habits.forEachIndexed { index, habit ->
                                                HabitRow(
                                                    habit = habit,
                                                    onClick = { editHabitIndex = index; showAddHabitSheet = true },
                                                    onRemove = { viewModel.removeHabit(index) }
                                                )
                                                if (index < uiState.habits.size - 1) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                        color = Color.White,
                                                        thickness = 1.dp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ModernAddButton(
                                        onClick = { editHabitIndex = null; showAddHabitSheet = true },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val canSave = !uiState.isSaving && uiState.goal.isNotBlank() && uiState.habits.isNotEmpty()
                            Button(
                                onClick = viewModel::save,
                                enabled = canSave,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = if (canSave) {
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
                                    Text("Save", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(0.15f))
                    }
                }
            }
        }
    }

    if (showAddHabitSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddHabitSheet = false; editHabitIndex = null },
            sheetState = habitSheetState
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

    if (showPauseCalendar) {
        ModalBottomSheet(
            onDismissRequest = { showPauseCalendar = false },
            sheetState = pauseSheetState
        ) {
            DateRangePickerSheet(
                initialFrom = uiState.pausedFromDate,
                initialTo = uiState.pausedToDate,
                onDismiss = { showPauseCalendar = false },
                onRangeSelected = { from, to ->
                    viewModel.setPauseRange(from, to)
                    showPauseCalendar = false
                },
                onCancel = { viewModel.setPauseRange("", "") }
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
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = when (habit.frequencyType) {
                        FrequencyType.DAILY -> "Daily"
                        FrequencyType.WEEKLY -> "Weekly"
                        FrequencyType.SPECIFIC_DAYS -> "${habit.targetCount}× per week"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove"
                )
            }
        }
    }
}
