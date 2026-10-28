package com.example.data.model

/** The parts of the Wake windows card; which show is chosen in Customize Today. */
enum class WakeWindowPart(val label: String) {
    AWAKE_FOR("Awake for"),
    NAP_DUE("Nap due"),
    TODAY_STATS("Today's average"),
    WINDOW_BARS("Each window")
}

/** Time awake between two sleeps (the Wake windows card on Today). */
data class WakeWindow(val startMillis: Long, val endMillis: Long) {
    val minutes: Int get() = ((endMillis - startMillis) / 60_000L).toInt()
}

/**
 * The wake windows that ended today: the gaps between consecutive [sleeps] (today's, which include last night's sleep
 * when it ended after midnight). Overlapping sleeps and gaps over 12 hours (a missed sleep entry) are left out.
 */
fun completedWakeWindows(sleeps: List<SleepEvent>, dayStartMillis: Long): List<WakeWindow> = sleeps.sortedBy { it.timeMillis }
    .zipWithNext { a, b -> WakeWindow(a.endMillis, b.timeMillis) }
    .filter { it.endMillis >= dayStartMillis && it.endMillis > it.startMillis && it.endMillis - it.startMillis <= MAX_WAKE_WINDOW_MS }

fun List<WakeWindow>.averageMinutes(): Int = if (isEmpty()) 0 else sumOf { it.minutes } / size

private const val MAX_WAKE_WINDOW_MS = 12 * 60 * 60_000L
