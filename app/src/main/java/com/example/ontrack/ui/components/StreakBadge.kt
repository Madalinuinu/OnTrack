package com.example.ontrack.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Duolingo-style streak badge: orange/red flame + count. */
@Composable
fun StreakBadge(
    streak: Int,
    modifier: Modifier = Modifier
) {
    if (streak <= 0) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = "Streak",
            tint = Color(0xFFFF6B35),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = "$streak",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFE63900)
        )
    }
}
