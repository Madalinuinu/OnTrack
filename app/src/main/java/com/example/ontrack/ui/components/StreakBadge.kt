package com.example.ontrack.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

/** Streak badge: flame + streak count when today complete; ice + (streak+1) when today not complete (e.g. 7 yesterday → show 8 with ice, then 8 with fire when done).
 * [snowflakeTint] when set, overrides the snowflake (AcUnit) icon color (e.g. on Activity screen).
 * [fireTint] when set, overrides the flame icon color (e.g. lighter orange on Activity screen).
 * [isVacation] when true, streak is frozen and icon is shown in vacation orange (same as Activity vacation days). */
@Composable
fun StreakBadge(
    streak: Int,
    isTodayComplete: Boolean,
    freezeCount: Int = 0,
    snowflakeTint: Color? = null,
    fireTint: Color? = null,
    isVacation: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayNumber = if (isTodayComplete) maxOf(1, streak) else maxOf(1, streak + 1)
    val vacationOrange = Color(0xFFFF9500)
    val iconTint = when {
        isVacation -> vacationOrange
        isTodayComplete -> (fireTint ?: Color(0xFFFF6B35))
        snowflakeTint != null -> snowflakeTint
        else -> Color(0xFF58CCE8)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isTodayComplete) Icons.Filled.LocalFireDepartment else Icons.Filled.AcUnit,
            contentDescription = if (isTodayComplete) "Streak" else "Freeze",
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$displayNumber",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black
        )
    }
}
