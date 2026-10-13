package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.TimeFormat
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.PrimaryFixed
import kotlinx.coroutines.launch

private val WHEEL_ROW_HEIGHT = 44.dp
private const val WHEEL_VISIBLE_ROWS = 5

/**
 * A vertical scroll wheel of [count] values that snaps to the middle row. [onSelected] gets the index
 * in the middle whenever it changes (including once at first show). Tapping a value scrolls to it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    count: Int,
    initialIndex: Int,
    label: (Int) -> String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceIn(0, (count - 1).coerceAtLeast(0)))
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val scope = rememberCoroutineScope()
    val rowPx = with(LocalDensity.current) { WHEEL_ROW_HEIGHT.toPx() }
    // With padding of two rows above and below, the first visible item is the one in the middle
    val centered by remember(count) {
        derivedStateOf {
            val offsetRows = if (state.firstVisibleItemScrollOffset > rowPx / 2) 1 else 0
            (state.firstVisibleItemIndex + offsetRows).coerceIn(0, (count - 1).coerceAtLeast(0))
        }
    }
    val latestOnSelected by rememberUpdatedState(onSelected)
    LaunchedEffect(centered) { latestOnSelected(centered) }

    Box(
        modifier = modifier
            .width(width)
            .height(WHEEL_ROW_HEIGHT * WHEEL_VISIBLE_ROWS),
        contentAlignment = Alignment.Center
    ) {
        // Selection band behind the middle row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_ROW_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryFixed)
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = WHEEL_ROW_HEIGHT * (WHEEL_VISIBLE_ROWS / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(count) { index ->
                val selected = index == centered
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WHEEL_ROW_HEIGHT)
                        .clickable { scope.launch { state.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label(index),
                        style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) PrimaryColor else OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelSeparator(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = OnSurface,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

/** Switches every time/duration picker between wheels and typing; the choice is remembered. */
@Composable
fun InputModeToggle(modifier: Modifier = Modifier) {
    TextButton(onClick = { TimeFormat.useWheel = !TimeFormat.useWheel }, modifier = modifier) {
        Icon(
            if (TimeFormat.useWheel) Icons.Default.Keyboard else Icons.Default.SwapVert,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            if (TimeFormat.useWheel) "Type instead" else "Use wheel",
            color = OnSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

/** A clock time (24h or 12h per Settings) as wheels or the keyboard field, with a switch between them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockTimeEntry(hour: Int, minute: Int, onChange: (hour: Int, minute: Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (TimeFormat.useWheel) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (TimeFormat.use24h) {
                    WheelPicker(count = 24, initialIndex = hour, label = { "%02d".format(it) }, onSelected = { onChange(it, minute) })
                } else {
                    // 12, 1 … 11 and AM/PM
                    WheelPicker(
                        count = 12,
                        initialIndex = hour % 12,
                        label = { if (it == 0) "12" else "$it" },
                        onSelected = { onChange(it + if (hour >= 12) 12 else 0, minute) }
                    )
                }
                WheelSeparator(":")
                WheelPicker(count = 60, initialIndex = minute, label = { "%02d".format(it) }, onSelected = { onChange(hour, it) })
                if (!TimeFormat.use24h) {
                    WheelPicker(
                        count = 2,
                        initialIndex = if (hour >= 12) 1 else 0,
                        label = { if (it == 0) "AM" else "PM" },
                        onSelected = { onChange(hour % 12 + it * 12, minute) },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        } else {
            val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = TimeFormat.use24h)
            LaunchedEffect(state.hour, state.minute) { onChange(state.hour, state.minute) }
            TimeInput(state = state)
        }
        InputModeToggle()
    }
}

/**
 * A duration as two wheels ([firstLabel] / [secondLabel], e.g. minutes : seconds or hours : minutes).
 * [firstMax] is the largest first value offered.
 */
@Composable
fun DurationWheels(
    first: Int,
    second: Int,
    firstMax: Int,
    firstUnit: String,
    secondUnit: String,
    onChange: (first: Int, second: Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        WheelPicker(
            count = maxOf(firstMax, first) + 1,
            initialIndex = first,
            label = { "$it" },
            onSelected = { onChange(it, second) }
        )
        WheelSeparator(firstUnit)
        WheelPicker(count = 60, initialIndex = second, label = { "%02d".format(it) }, onSelected = { onChange(first, it) })
        WheelSeparator(secondUnit)
    }
}
