package com.example.ontrack.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Streak badge: flame + count (min 1) when today complete; ice + freeze count (1–3) when today not complete. */
@Composable
fun StreakBadge(
    streak: Int,
    isTodayComplete: Boolean,
    freezeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val displayNumber = if (isTodayComplete) maxOf(1, streak) else maxOf(1, freezeCount)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isTodayComplete) Icons.Filled.LocalFireDepartment else Icons.Filled.AcUnit,
            contentDescription = if (isTodayComplete) "Streak" else "Freeze",
            tint = when {
                isTodayComplete -> Color(0xFFFF6B35)
                else -> Color(0xFF58CCE8)
            },
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = "$displayNumber",
            style = MaterialTheme.typography.titleSmall,
            color = when {
                isTodayComplete -> Color(0xFFE63900)
                else -> Color(0xFF2E86AB)
            }
        )
    }
}
