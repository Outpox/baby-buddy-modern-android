package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import com.example.data.model.TimeFormat
import com.example.ui.components.ClockTimeEntry
import com.example.ui.components.DurationWheels
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WheelPickerTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun useWheels() {
        TimeFormat.useWheel = true
        TimeFormat.use24h = true
    }

    @After
    fun reset() {
        TimeFormat.useWheel = false
    }

    @Test
    fun `minutes going back past 00 wrap to 59 and carry the hour and the day`() {
        var hour = 0
        var minute = 0
        var days = 0
        compose.setContent {
            var h by remember { mutableIntStateOf(0) }
            var m by remember { mutableIntStateOf(0) }
            ClockTimeEntry(hour = h, minute = m, onChange = { newH, newM, shift ->
                h = newH
                m = newM
                hour = newH
                minute = newM
                days += shift
            })
        }
        // The row just above 00 on the minute wheel (the hour wheel shows 22, 23, 00, 01, 02)
        compose.onAllNodesWithText("59").onFirst().performClick()
        compose.waitForIdle()
        assertEquals(23, hour)
        assertEquals(59, minute)
        assertEquals(-1, days)
        // The hour wheel followed to 23 without counting a second wrap
        compose.onAllNodesWithText("23").onFirst().performClick()
        compose.waitForIdle()
        assertEquals(23, hour)
        assertEquals(-1, days)
    }

    @Test
    fun `duration seconds going back past 00 lower the minutes but not below zero`() {
        var value by mutableStateOf(60)
        compose.setContent {
            DurationWheels(first = value / 60, second = value % 60, firstMax = 180, firstUnit = "m", secondUnit = "s", onChange = { m, s ->
                value = m * 60 + s
            })
        }
        compose.onAllNodesWithText("59").onFirst().performClick()
        compose.waitForIdle()
        assertEquals(59, value)
    }
}
