package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BATH_NOTE_TEXT
import com.example.data.model.DiaperEvent
import com.example.data.model.DiaperType
import com.example.data.model.FeedType
import com.example.data.model.FeedingEvent
import com.example.data.model.GrowthEvent
import com.example.data.model.HistoryDay
import com.example.data.model.NoteEvent
import com.example.data.model.PumpingEvent
import com.example.data.model.SleepEvent
import com.example.data.model.TimeFormat
import com.example.data.model.TrackedItem
import com.example.data.model.TummyTimeEvent
import com.example.data.model.formatGrowth
import com.example.data.model.formatMinutes
import com.example.ui.HistoryFilter
import com.example.ui.theme.ItemColors
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One row in the History list: any entry type, with what to show and what to open on tap. */
private data class HistoryEntry(
    val key: String,
    val timeMillis: Long,
    val time: String,
    val title: String,
    val detail: String,
    val subtitle: String,
    val dot: Color,
    val onClick: () -> Unit
)

/** "Alex · the note": who logged it, then the whole note (its lines run together). */
private fun whoAndNote(caregiver: String?, notes: String): String = listOfNotNull(
    caregiver,
    notes.lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() }
).joinToString(" · ")

/** "Dirty · Mustard · Seedy": the type, then the stool color and consistency when recorded. */
private fun diaperDetail(d: DiaperEvent): String {
    val stool = d.type == DiaperType.DIRTY || d.type == DiaperType.BOTH
    return listOfNotNull(
        d.type.name.lowercase().replaceFirstChar(Char::uppercase),
        d.color.takeIf { stool && it.isNotBlank() },
        d.consistency.substringBefore(" / ").takeIf { stool && it.isNotBlank() }
    ).joinToString(" · ")
}

/** Rash and cream, then who logged it and the note. */
private fun diaperSubtitle(d: DiaperEvent): String = listOfNotNull(
    "Rash".takeIf { d.rashPresent },
    d.skinBarrierApplied.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.substringBefore(" / ") },
    whoAndNote(d.caregiver, d.notes).takeIf { it.isNotBlank() }
).joinToString(" · ")

private fun localDayStart(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun sideLabel(feed: FeedingEvent): String {
    val sides = feed.sides
        ?.split("->")
        ?.map { it.trim().lowercase().replaceFirstChar(Char::uppercase) }
        ?.filter { it.isNotBlank() }
        ?.joinToString(" → ")
    return sides ?: "L ${feed.leftDurationMinutes}m · R ${feed.rightDurationMinutes}m"
}

private fun entriesOf(
    day: HistoryDay,
    growth: List<GrowthEvent>, // this day's measurements
    filter: HistoryFilter,
    onFeeding: (FeedingEvent) -> Unit,
    onDiaper: (DiaperEvent) -> Unit,
    onTummy: (TummyTimeEvent) -> Unit,
    onPumping: (PumpingEvent) -> Unit,
    onSleep: (SleepEvent) -> Unit,
    onBath: (NoteEvent) -> Unit = {},
    onNote: (NoteEvent) -> Unit = {},
    onGrowth: (GrowthEvent) -> Unit = {}
): List<HistoryEntry> {
    val out = mutableListOf<HistoryEntry>()
    if (filter == HistoryFilter.ALL || filter == HistoryFilter.FEEDS) {
        day.feedings.forEach { f ->
            val isBottle = f.amountMl != null || f.type == FeedType.BOTTLE
            out += HistoryEntry(
                key = "feed_${f.id}",
                timeMillis = f.timeMillis,
                time = f.timeFormatted,
                title = if (isBottle) "Bottle" else "Breastfeed",
                detail = if (isBottle) {
                    "${f.amountMl ?: 0} ml ${if (f.bottleFormula) "formula" else "breast milk"}"
                } else {
                    "${f.totalDurationMinutes} min · ${sideLabel(f)}"
                },
                subtitle = whoAndNote(f.caregiver, f.notes),
                dot = ItemColors.tint(if (isBottle) TrackedItem.BOTTLE else TrackedItem.BREASTFEEDING),
                onClick = { onFeeding(f) }
            )
        }
    }
    if (filter == HistoryFilter.ALL || filter == HistoryFilter.DIAPERS) {
        day.diapers.forEach { d ->
            out += HistoryEntry(
                key = "diaper_${d.id}",
                timeMillis = d.timeMillis,
                time = d.timeFormatted,
                title = "Diaper",
                detail = diaperDetail(d),
                subtitle = diaperSubtitle(d),
                dot = when (d.type) {
                    DiaperType.WET -> ItemColors.tint(TrackedItem.DIAPERS).copy(alpha = 0.4f)
                    DiaperType.DIRTY, DiaperType.BOTH -> ItemColors.tint(TrackedItem.DIAPERS)
                    DiaperType.DRY -> OutlineVariant
                },
                onClick = { onDiaper(d) }
            )
        }
    }
    if (filter == HistoryFilter.ALL || filter == HistoryFilter.PUMPING) {
        day.pumpings.forEach { p ->
            out += HistoryEntry(
                key = "pump_${p.id}",
                timeMillis = p.timeMillis,
                time = p.timeFormatted,
                title = "Pumping",
                detail = "${p.amountMl} ml" + if (p.durationMinutes > 0) " · ${p.durationMinutes} min" else "",
                subtitle = whoAndNote(p.caregiver, p.notes),
                dot = ItemColors.tint(TrackedItem.PUMPING),
                onClick = { onPumping(p) }
            )
        }
    }
    if (filter == HistoryFilter.ALL || filter == HistoryFilter.TUMMY) {
        day.tummyTimes.forEach { t ->
            out += HistoryEntry(
                key = "tummy_${t.id}",
                timeMillis = t.timeMillis,
                time = t.timeFormatted,
                title = "Tummy time",
                detail = "${t.durationMinutes} min",
                subtitle = whoAndNote(t.caregiver, t.milestone),
                dot = ItemColors.tint(TrackedItem.TUMMY_TIME),
                onClick = { onTummy(t) }
            )
        }
    }
    if (filter == HistoryFilter.ALL || filter == HistoryFilter.SLEEP) {
        day.sleeps.forEach { s ->
            out += HistoryEntry(
                key = "sleep_${s.id}",
                timeMillis = s.timeMillis,
                time = s.timeFormatted,
                title = if (s.isNap) "Nap" else "Sleep",
                detail = "${formatMinutes(s.durationMinutes)} · until ${TimeFormat.clock(s.endMillis)}",
                subtitle = whoAndNote(s.caregiver, s.notes),
                dot = ItemColors.tint(TrackedItem.SLEEP),
                onClick = { onSleep(s) }
            )
        }
    }
    if (filter == HistoryFilter.ALL || filter == HistoryFilter.BATH) {
        day.baths.forEach { b ->
            out += HistoryEntry(
                key = "bath_${b.id}",
                timeMillis = b.timeMillis,
                time = b.timeFormatted,
                title = "Bath",
                detail = "",
                subtitle = if (b.text == BATH_NOTE_TEXT) "" else whoAndNote(null, b.text),
                dot = ItemColors.tint(TrackedItem.BATH),
                onClick = { onBath(b) }
            )
        }
    }
    if (filter == HistoryFilter.ALL || filter == HistoryFilter.GROWTH) {
        growth.forEach { g ->
            out += HistoryEntry(
                key = "growth_${g.id}",
                timeMillis = g.dateMillis,
                time = "", // the server keeps only the date
                title = g.kind.label,
                detail = formatGrowth(g.kind, g.value),
                subtitle = whoAndNote(g.caregiver, g.notes),
                dot = ItemColors.tint(TrackedItem.GROWTH),
                onClick = { onGrowth(g) }
            )
        }
    }
    if (filter == HistoryFilter.ALL || filter == HistoryFilter.NOTES) {
        day.notes.forEach { n ->
            val lines = n.text.lines().filter { it.isNotBlank() }
            out += HistoryEntry(
                key = "note_${n.id}",
                timeMillis = n.timeMillis,
                time = n.timeFormatted,
                title = "Note",
                detail = lines.firstOrNull().orEmpty(),
                subtitle = listOfNotNull(
                    lines.drop(1).joinToString(" ").takeIf { it.isNotBlank() },
                    n.tags.takeIf { it.isNotEmpty() }?.joinToString(" ") { "#$it" }
                ).joinToString(" · "),
                dot = OutlineVariant,
                onClick = { onNote(n) }
            )
        }
    }
    return out.sortedByDescending { it.timeMillis }
}

/** The filters for what this phone tracks. */
internal fun historyFiltersFor(hidden: Set<TrackedItem>): List<HistoryFilter> = HistoryFilter.entries.filter { filter ->
    when (filter) {
        HistoryFilter.ALL -> true
        HistoryFilter.FEEDS -> TrackedItem.BREASTFEEDING !in hidden || TrackedItem.BOTTLE !in hidden
        HistoryFilter.DIAPERS -> TrackedItem.DIAPERS !in hidden
        HistoryFilter.PUMPING -> TrackedItem.PUMPING !in hidden
        HistoryFilter.TUMMY -> TrackedItem.TUMMY_TIME !in hidden
        HistoryFilter.SLEEP -> TrackedItem.SLEEP !in hidden
        HistoryFilter.BATH -> TrackedItem.BATH !in hidden
        HistoryFilter.GROWTH -> TrackedItem.GROWTH !in hidden
        HistoryFilter.NOTES -> true
    }
}

/** A day without the entry types this phone doesn't track. */
internal fun HistoryDay.withoutHidden(hidden: Set<TrackedItem>): HistoryDay = if (hidden.isEmpty()) {
    this
} else {
    copy(
        feedings = feedings.filter { f ->
            if (f.type == FeedType.NURSING) TrackedItem.BREASTFEEDING !in hidden else TrackedItem.BOTTLE !in hidden
        },
        diapers = if (TrackedItem.DIAPERS in hidden) emptyList() else diapers,
        pumpings = if (TrackedItem.PUMPING in hidden) emptyList() else pumpings,
        tummyTimes = if (TrackedItem.TUMMY_TIME in hidden) emptyList() else tummyTimes,
        sleeps = if (TrackedItem.SLEEP in hidden) emptyList() else sleeps,
        baths = if (TrackedItem.BATH in hidden) emptyList() else baths
    )
}

/** A short summary for a day's header, matching the filter. */
private fun daySummary(day: HistoryDay, growth: List<GrowthEvent>, filter: HistoryFilter): String = when (filter) {
    HistoryFilter.ALL -> listOf(
        "${day.feedings.size} feeds",
        "${day.diapers.size} diapers"
    ).joinToString(" · ")

    HistoryFilter.FEEDS -> {
        val nursing = day.feedings.filter { it.amountMl == null }.sumOf { it.totalDurationMinutes }
        val ml = day.feedings.sumOf { it.amountMl ?: 0 }
        listOfNotNull("${day.feedings.size} feeds", "$nursing min nursing", if (ml > 0) "$ml ml bottle" else null)
            .joinToString(" · ")
    }

    HistoryFilter.DIAPERS -> {
        val wet = day.diapers.count { it.type == DiaperType.WET || it.type == DiaperType.BOTH }
        val dirty = day.diapers.count { it.type == DiaperType.DIRTY || it.type == DiaperType.BOTH }
        "$wet wet · $dirty dirty"
    }

    HistoryFilter.PUMPING -> "${day.pumpings.sumOf { it.amountMl }} ml"

    HistoryFilter.TUMMY -> "${day.tummyTimes.sumOf { it.durationMinutes }} min"

    HistoryFilter.SLEEP -> "${formatMinutes(day.sleeps.sumOf { it.durationMinutes })} asleep"

    HistoryFilter.BATH -> if (day.baths.size == 1) "1 bath" else "${day.baths.size} baths"

    HistoryFilter.GROWTH -> if (growth.size == 1) "1 measurement" else "${growth.size} measurements"

    HistoryFilter.NOTES -> if (day.notes.size == 1) "1 note" else "${day.notes.size} notes"
}

private fun dayTitle(dayStartMillis: Long): String {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return when {
        dayStartMillis >= today -> "Today"
        dayStartMillis >= today - 24 * 60 * 60_000L -> "Yesterday"
        else -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(dayStartMillis))
    }
}

@Composable
internal fun HistoryFilterChips(
    selected: HistoryFilter,
    onSelect: (HistoryFilter) -> Unit,
    available: List<HistoryFilter> = HistoryFilter.entries
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        available.forEach { filter ->
            val isSelected = filter == selected
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) PrimaryColor else SurfaceContainerLow,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelect(filter) }
            ) {
                Text(
                    filter.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) OnPrimary else OnSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/** "Oct 4 – 10" with previous/next week arrows. */
@Composable
internal fun HistoryWeekNav(
    days: List<HistoryDay>,
    isCurrentWeek: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val first = days.firstOrNull()?.dayStartMillis ?: return
    val last = days.last().dayStartMillis
    val month = SimpleDateFormat("MMM d", Locale.getDefault())
    val sameMonth = Calendar.getInstance().apply { timeInMillis = first }.get(Calendar.MONTH) ==
        Calendar.getInstance().apply { timeInMillis = last }.get(Calendar.MONTH)
    val endLabel = if (sameMonth) {
        SimpleDateFormat("d", Locale.getDefault()).format(Date(last))
    } else {
        month.format(Date(last))
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousWeek) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", tint = OnSurfaceVariant)
        }
        Text(
            text = if (isCurrentWeek) "Last 7 days" else "${month.format(Date(first))} – $endLabel",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNextWeek, enabled = !isCurrentWeek) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next week",
                tint = if (isCurrentWeek) OutlineVariant else OnSurfaceVariant
            )
        }
    }
}

/** The week's entries as a list, newest first, grouped by day. Tapping a row opens its edit dialog. */
@Composable
internal fun HistoryList(
    days: List<HistoryDay>,
    filter: HistoryFilter,
    onFeedingClick: (FeedingEvent) -> Unit,
    onDiaperClick: (DiaperEvent) -> Unit,
    onTummyClick: (TummyTimeEvent) -> Unit,
    onPumpingClick: (PumpingEvent) -> Unit,
    onSleepClick: (SleepEvent) -> Unit = {},
    onBathClick: (NoteEvent) -> Unit = {},
    onNoteClick: (NoteEvent) -> Unit = {},
    growth: List<GrowthEvent> = emptyList(),
    onGrowthClick: (GrowthEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Measurements are rare (monthly), so the Growth filter lists all of them rather than the week's
    val growthByDay = growth.groupBy { localDayStart(it.dateMillis) }
    val shownDays = if (filter == HistoryFilter.GROWTH) growthByDay.keys.map { HistoryDay(dayStartMillis = it) } else days
    val grouped = shownDays.sortedByDescending { it.dayStartMillis }.map { day ->
        val dayGrowth = growthByDay[day.dayStartMillis].orEmpty()
        Triple(
            day,
            dayGrowth,
            entriesOf(day, dayGrowth, filter, onFeedingClick, onDiaperClick, onTummyClick, onPumpingClick, onSleepClick, onBathClick, onNoteClick, onGrowthClick)
        )
    }
    val anything = grouped.any { it.third.isNotEmpty() }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!anything) {
            item {
                Text(
                    text = when (filter) {
                        HistoryFilter.ALL -> "Nothing logged these 7 days."
                        HistoryFilter.GROWTH -> "No growth measurements yet."
                        else -> "No ${filter.label.lowercase()} logged these 7 days."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp)
                )
            }
        }
        grouped.forEach { (day, dayGrowth, entries) ->
            if (entries.isEmpty()) return@forEach
            item(key = "day_${day.dayStartMillis}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        dayTitle(day.dayStartMillis),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(daySummary(day, dayGrowth, filter), style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                }
            }
            items(entries, key = { it.key }) { entry -> HistoryRow(entry) }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            entry.time,
            style = MaterialTheme.typography.labelLarge,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(end = 2.dp)
        )
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(entry.dot))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (entry.detail.isBlank()) entry.title else "${entry.title} · ${entry.detail}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (entry.subtitle.isNotBlank()) {
                Text(
                    entry.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
