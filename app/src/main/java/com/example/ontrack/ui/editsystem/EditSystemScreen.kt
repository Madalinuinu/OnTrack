package com.example.ontrack.ui.editsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
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
                title = { Text("Edit System") },
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.goal,
                    onValueChange = viewModel::updateGoal,
                    label = { Text("Goal") },
                    placeholder = { Text("e.g. Become more productive") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.duration,
                    onValueChange = viewModel::updateDuration,
                    label = { Text("Duration (optional)") },
                    placeholder = { Text("e.g. 30 or 90 days") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Pause system (e.g. vacation)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to select period (e.g. vacation). Paused days show in orange in Activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showPauseCalendar = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            uiState.pausedFromDate.isNotBlank() && uiState.pausedToDate.isNotBlank() ->
                                "${uiState.pausedFromDate} — ${uiState.pausedToDate}"
                            uiState.pausedFromDate.isNotBlank() -> "From: ${uiState.pausedFromDate} (tap to set To)"
                            else -> "Select pause period (From → To)"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Habits",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.habits.isEmpty()) {
                    Text(
                        text = "Add at least one habit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.habits.forEachIndexed { index, habit ->
                            HabitRow(
                                habit = habit,
                                onClick = { editHabitIndex = index; showAddHabitSheet = true },
                                onRemove = { viewModel.removeHabit(index) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { editHabitIndex = null; showAddHabitSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Habit")
                }
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving && uiState.goal.isNotBlank() && uiState.habits.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
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
