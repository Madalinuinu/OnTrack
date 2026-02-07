package com.example.ontrack.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.ui.components.StreakBadge

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onCreateSystemClick: () -> Unit,
    onOpenSystemClick: (Long) -> Unit,
    onActivityClick: (Long) -> Unit,
    onEditSystemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val systems by viewModel.systems.collectAsState(initial = emptyList())
    val selectedSystemId by viewModel.selectedSystemId.collectAsState(initial = null)
    val todayCompleteMap by viewModel.todayCompleteMap.collectAsState(initial = emptyMap())
    val freezeCountMap by viewModel.freezeCountMap.collectAsState(initial = emptyMap())
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshTodayComplete()
    }

    val selectedSystem = remember(selectedSystemId, systems) {
        selectedSystemId?.let { id -> systems.find { it.id == id } }
    }
    if (showDeleteConfirm && selectedSystem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete system?") },
            text = {
                Text(
                    "Are you sure you want to delete this system? Your streak is ${selectedSystem.currentStreak} days."
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome back, ${userName.ifBlank { "there" }}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    imageVector = if (darkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = if (darkMode) "Light mode" else "Dark mode"
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateSystemClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create New System")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (systems.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        onOpenClick = { viewModel.selectSystem(null); onOpenSystemClick(system.id) },
                        onActivityClick = { viewModel.selectSystem(null); onActivityClick(system.id) },
                        onDeleteClick = { showDeleteConfirm = true },
                        onMoveUp = { if (index > 0) viewModel.reorderSystems(index, index - 1) },
                        onMoveDown = { if (index < systems.size - 1) viewModel.reorderSystems(index, index + 1) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .alpha(0.6f),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No systems yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Create your first system to start tracking habits and building streaks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isTodayComplete -> Color(0xFFB8E0B8)
        else -> Color(0xFFB4C8E4)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCardClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
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
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
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
                            Text("Delete system")
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(onClick = onActivityClick) {
                    Text("Activity")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onOpenClick) {
                    Text("Open")
                }
            }
        }
    }
}
