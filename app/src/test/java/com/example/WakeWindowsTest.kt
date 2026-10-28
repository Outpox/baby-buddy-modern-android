package com.example

import com.example.data.model.SleepEvent
import com.example.data.model.WakeWindow
import com.example.data.model.completedWakeWindows
import org.junit.Assert.assertEquals
import org.junit.Test

/** Today's wake windows are the gaps between consecutive sleeps. */
class WakeWindowsTest {
    private val hour = 60 * 60_000L
    private val midnight = 100 * 24 * hour

    private fun sleep(fromHour: Double, toHour: Double) = SleepEvent(
        timeMillis = midnight + (fromHour * hour).toLong(),
        endMillis = midnight + (toHour * hour).toLong()
    )

    @Test
    fun gapsBetweenSleepsInAnyOrder() {
        val sleeps = listOf(sleep(12.0, 13.5), sleep(-4.0, 6.0), sleep(8.0, 9.0))
        assertEquals(
            listOf(
                WakeWindow(midnight + 6 * hour, midnight + 8 * hour),
                WakeWindow(midnight + 9 * hour, midnight + 12 * hour)
            ),
            completedWakeWindows(sleeps, midnight)
        )
        assertEquals(listOf(120, 180), completedWakeWindows(sleeps, midnight).map { it.minutes })
    }

    @Test
    fun leavesOutOverlapsYesterdayAndMissedSleeps() {
        val overlapping = listOf(sleep(8.0, 10.0), sleep(9.5, 11.0))
        assertEquals(emptyList<WakeWindow>(), completedWakeWindows(overlapping, midnight))
        // A window that ended before midnight belongs to yesterday
        assertEquals(emptyList<WakeWindow>(), completedWakeWindows(listOf(sleep(-6.0, -5.0), sleep(-3.0, 1.0)), midnight))
        // Over 12 hours awake means a sleep wasn't logged
        assertEquals(emptyList<WakeWindow>(), completedWakeWindows(listOf(sleep(-4.0, 1.0), sleep(14.0, 15.0)), midnight))
    }
}
