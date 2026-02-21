package com.example.ontrack.ui.createsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.ui.components.OnTrackSegmentButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitSheet(
    onDismiss: () -> Unit,
    onAdd: (HabitItem) -> Unit,
    initial: HabitItem? = null,
    onUpdate: ((HabitItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isEdit = initial != null && onUpdate != null
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var frequency by remember(initial) { mutableStateOf(initial?.frequencyType ?: FrequencyType.DAILY) }
    var timesPerWeek by remember(initial) { mutableStateOf(initial?.targetCount?.coerceIn(1, 7) ?: 3) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (isEdit) "Edit Habit" else "Add Habit",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = {
                Text(
                    text = "Habit title",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                    color = Color.DarkGray
                )
            },
            placeholder = {
                Text(
                    text = "e.g. Morning run",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = Color.Gray
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.DarkGray.copy(alpha = 0.7f),
                focusedLabelColor = Color.DarkGray,
                unfocusedLabelColor = Color.DarkGray,
                cursorColor = Color.Black,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedPlaceholderColor = Color.Gray,
                unfocusedPlaceholderColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Frequency",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrequencyType.entries.forEach { type ->
                val label = when (type) {
                    FrequencyType.DAILY -> "Daily"
                    FrequencyType.WEEKLY -> "Weekly"
                    FrequencyType.SPECIFIC_DAYS -> "X/week"
                }
                OnTrackSegmentButton(
                    text = label,
                    selected = frequency == type,
                    modifier = Modifier.weight(1f),
                    onClick = { frequency = type }
                )
            }
        }

        if (frequency == FrequencyType.SPECIFIC_DAYS) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$timesPerWeek times per week",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color.DarkGray
            )
            Slider(
                value = timesPerWeek.toFloat(),
                onValueChange = { timesPerWeek = it.toInt().coerceIn(1, 7) },
                valueRange = 1f..7f,
                steps = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text("Cancel", color = Color.Black)
            }
            Spacer(modifier = Modifier.padding(8.dp))
            OutlinedButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val item = HabitItem(
                            title = title.trim(),
                            frequencyType = frequency,
                            targetCount = if (frequency == FrequencyType.SPECIFIC_DAYS) timesPerWeek else 1
                        )
                        if (isEdit) onUpdate!!(item) else onAdd(item)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isEdit) "Save" else "Add", color = Color.Black)
            }
        }
    }
}
