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

/**
 * Number shown next to fire/ice.
 * - Today complete → [streak] (e.g. fire + 6).
 * - Today incomplete, last streak day was **before** today → [streak + 1] (working toward next day; e.g. ice + 8 after 7-day streak).
 * - Today incomplete but DB still has [lastStreakDateEpoch] == today (user completed then toggled off; refreshStreak skips) → [streak],
 *   same as the fire state before undo (e.g. ice + 1, not ice + 2 on first day).
 */
fun streakBadgeDisplayNumber(
    streak: Int,
    isTodayComplete: Boolean,
    lastStreakDateEpoch: Long,
    todayEpoch: Long
): Int = when {
    isTodayComplete -> maxOf(1, streak)
    lastStreakDateEpoch == todayEpoch && !isTodayComplete -> maxOf(1, streak)
    else -> maxOf(1, streak + 1)
}

/** Streak badge: flame + streak count when today complete; ice uses [streakBadgeDisplayNumber].
 * [snowflakeTint] when set, overrides the snowflake (AcUnit) icon color (e.g. on Activity screen).
 * [fireTint] when set, overrides the flame icon color (e.g. lighter orange on Activity screen).
 * [isVacation] when true, streak is frozen and icon is shown in vacation orange (same as Activity vacation days). */
@Composable
fun StreakBadge(
    streak: Int,
    isTodayComplete: Boolean,
    lastStreakDateEpoch: Long = -1L,
    todayEpoch: Long = com.example.ontrack.util.EffectiveDate.todayEpoch(),
    freezeCount: Int = 0,
    snowflakeTint: Color? = null,
    fireTint: Color? = null,
    isVacation: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayNumber = streakBadgeDisplayNumber(
        streak = streak,
        isTodayComplete = isTodayComplete,
        lastStreakDateEpoch = lastStreakDateEpoch,
        todayEpoch = todayEpoch
    )
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
