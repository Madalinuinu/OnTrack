package com.example.ontrack.ui.editsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

private val minMonth: YearMonth get() = YearMonth.now()
private val minEpochDay: Long get() = LocalDate.now().toEpochDay()

@Composable
fun DateRangePickerSheet(
    initialFrom: String = "",
    initialTo: String = "",
    onDismiss: () -> Unit,
    onRangeSelected: (fromDate: String, toDate: String) -> Unit,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedMonth by remember {
        val start = when {
            initialFrom.isNotBlank() -> try {
                YearMonth.from(LocalDate.parse(initialFrom, formatter))
            } catch (_: Exception) { minMonth }
            else -> minMonth
        }
        mutableStateOf(maxOf(start, minMonth))
    }
    var fromEpoch by remember {
        val v = initialFrom.let { s ->
            if (s.isBlank()) null else try {
                LocalDate.parse(s, formatter).toEpochDay()
            } catch (_: Exception) { null }
        }
        mutableStateOf(v)
    }
    var toEpoch by remember {
        val v = initialTo.let { s ->
            if (s.isBlank()) null else try {
                LocalDate.parse(s, formatter).toEpochDay()
            } catch (_: Exception) { null }
        }
        mutableStateOf(v)
    }

    val canGoPrev = selectedMonth > minMonth

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Select period",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Tap first date (From), then second date (To). From this month onward.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (fromEpoch != null) {
            Text(
                text = "From: ${LocalDate.ofEpochDay(fromEpoch!!).format(formatter)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (toEpoch != null) {
            Text(
                text = "To: ${LocalDate.ofEpochDay(toEpoch!!).format(formatter)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (canGoPrev) selectedMonth = selectedMonth.minusMonths(1) },
                enabled = canGoPrev
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        CalendarGrid(
            month = selectedMonth,
            minEpochDay = minEpochDay,
            fromEpoch = fromEpoch,
            toEpoch = toEpoch,
            onDayClick = { epochDay ->
                when {
                    fromEpoch == null -> fromEpoch = epochDay
                    toEpoch == null -> {
                        val from = fromEpoch!!
                        toEpoch = if (epochDay < from) {
                            fromEpoch = epochDay
                            from
                        } else epochDay
                    }
                    else -> {
                        fromEpoch = epochDay
                        toEpoch = null
                    }
                }
            }
        )

        if (fromEpoch != null && toEpoch != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    onCancel()
                    onDismiss()
                }) {
                    Text("Cancel")
                }
                Button(
                    modifier = Modifier.padding(start = 8.dp),
                    onClick = {
                        val (f, t) = minOf(fromEpoch!!, toEpoch!!) to maxOf(fromEpoch!!, toEpoch!!)
                        onRangeSelected(
                            LocalDate.ofEpochDay(f).format(formatter),
                            LocalDate.ofEpochDay(t).format(formatter)
                        )
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    minEpochDay: Long,
    fromEpoch: Long?,
    toEpoch: Long?,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDay = month.atDay(1)
    val lastDayNum = month.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value - 1
    val totalCells = startOffset + lastDayNum
    val rows = (totalCells + 6) / 7

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = if (cellIndex >= startOffset) (cellIndex - startOffset + 1) else null
                    val valid = dayNum != null && dayNum <= lastDayNum
                    val date = if (valid) month.atDay(dayNum!!) else null
                    val epochDay = date?.toEpochDay() ?: -1L
                    val isPast = valid && epochDay < minEpochDay
                    val clickable = valid && !isPast

                    val isFrom = epochDay == fromEpoch
                    val isTo = epochDay == toEpoch
                    val inRange = if (fromEpoch != null && toEpoch != null && epochDay >= 0)
                        epochDay in fromEpoch..toEpoch
                    else false

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isFrom || isTo -> MaterialTheme.colorScheme.primary
                                    inRange -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (clickable) Modifier.clickable { onDayClick(epochDay) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (valid) {
                            Text(
                                text = "$dayNum",
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isFrom || isTo -> MaterialTheme.colorScheme.onPrimary
                                    inRange -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
