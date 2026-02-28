package com.example.ontrack.ui.tracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val textPrimary = Color(0xFF1A1A1A)
private val textSecondary = Color(0xFF424242)
private val dialogBg = Color(0xFFFFFFFF)
private val dialogText = Color(0xFF1A1A1A)
private val dialogBorder = Color(0xFF1A1A1A)
private val slotRowHeight = 56.dp
private val labelAreaHeight = 32.dp

private const val MIN_DURATION_SECONDS = 120 // 2 minutes minimum

@Composable
fun DurationPickerSheet(
    habitTitle: String,
    selectedHours: Int,
    selectedMinutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSeconds = selectedHours * 3600 + selectedMinutes * 60
    val canStart = totalSeconds >= MIN_DURATION_SECONDS

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Select activity duration",
                style = MaterialTheme.typography.titleLarge,
                color = textPrimary
            )
            Text(
                text = habitTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeColumn(
                    label = "Hours",
                    value = selectedHours,
                    range = 0..99,
                    onValueChange = onHoursChange,
                    rowHeight = slotRowHeight
                )
                ColonSeparator(rowHeight = slotRowHeight)
                TimeColumn(
                    label = "Minutes",
                    value = selectedMinutes,
                    range = 0..59,
                    onValueChange = onMinutesChange,
                    rowHeight = slotRowHeight
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = onStart,
                    enabled = canStart
                ) {
                    Text(
                        text = "Start",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ColonSeparator(
    rowHeight: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.height(labelAreaHeight), contentAlignment = Alignment.Center) {}
        Box(modifier = Modifier.height(rowHeight), contentAlignment = Alignment.Center) {
            Text(
                text = " : ",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = textPrimary
            )
        }
    }
}

@Composable
private fun TimeColumn(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    rowHeight: Dp,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.clipToBounds(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textSecondary
        )
        Box(
            modifier = Modifier
                .height(rowHeight)
                .padding(horizontal = 12.dp)
                .clickable {
                    inputText = if (value == 0) "" else value.toString()
                    showDialog = true
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%02d".format(value),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = textPrimary
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = dialogBg,
            iconContentColor = dialogText,
            titleContentColor = dialogText,
            textContentColor = dialogText,
            title = { Text(text = label, color = dialogText) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = { Text("${range.first} – ${range.last}", color = dialogText.copy(alpha = 0.7f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = dialogText,
                        unfocusedTextColor = dialogText,
                        cursorColor = dialogText,
                        focusedBorderColor = dialogBorder,
                        unfocusedBorderColor = dialogBorder.copy(alpha = 0.5f),
                        focusedContainerColor = dialogBg,
                        unfocusedContainerColor = dialogBg
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val n = inputText.toIntOrNull()?.coerceIn(range.first, range.last)
                            ?: range.first
                        onValueChange(n)
                        showDialog = false
                    }
                ) {
                    Text("OK", color = dialogText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = dialogText)
                }
            }
        )
    }
}
