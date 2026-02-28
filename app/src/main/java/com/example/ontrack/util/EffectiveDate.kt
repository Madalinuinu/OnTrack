package com.example.ontrack.util

import java.time.LocalDate

/**
 * When test mode is on, the app uses [epochDay] as "today" so you can simulate days.
 * When test mode is off, [epochDay] is null and real date is used.
 * Updated at app start from UserPreferences and when user taps "Next day" on Test screen.
 */
object EffectiveDate {

    @Volatile
    var epochDay: Long? = null
        private set

    /** Current "today" as epoch day (for test mode or real date). */
    fun todayEpoch(): Long = epochDay ?: LocalDate.now().toEpochDay()

    /** Current "today" as LocalDate. */
    fun today(): LocalDate = LocalDate.ofEpochDay(todayEpoch())

    /** Call when test mode or simulated date changes (from preferences or Test screen). */
    fun update(testModeEnabled: Boolean, testEpochDay: Long) {
        epochDay = if (testModeEnabled && testEpochDay >= 0) testEpochDay else null
    }
}
