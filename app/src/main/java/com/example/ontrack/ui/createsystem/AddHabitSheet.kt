package com.example.ontrack.ui.createsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ontrack.data.local.entity.FrequencyType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitSheet(
    onDismiss: () -> Unit,
    onAdd: (HabitItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(FrequencyType.DAILY) }
    var timesPerWeek by remember { mutableStateOf(3) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Add Habit",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Habit title") },
            placeholder = { Text("e.g. Morning run") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Frequency",
            style = MaterialTheme.typography.labelMedium
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
                val selected = frequency == type
                if (selected) {
                    Button(
                        onClick = { frequency = type },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = { frequency = type },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (frequency == FrequencyType.SPECIFIC_DAYS) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$timesPerWeek times per week",
                style = MaterialTheme.typography.bodyMedium
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
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            HabitItem(
                                title = title.trim(),
                                frequencyType = frequency,
                                targetCount = if (frequency == FrequencyType.SPECIFIC_DAYS) timesPerWeek else 1
                            )
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Add")
            }
        }
    }
}
