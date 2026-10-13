package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BabySex
import com.example.data.model.GrowthEvent
import com.example.data.model.GrowthKind
import com.example.data.model.NoteEvent
import com.example.data.model.TemperatureEvent
import com.example.data.model.formatGrowth
import com.example.data.model.growthPercentile
import com.example.data.model.ordinalPercentile
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import java.util.Locale

/** Shared calm card shell: rounded white card, title row with a small remove button. */
@Composable
private fun WidgetCardShell(
    title: String,
    onRemoveWidget: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemoveWidget, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove card",
                        tint = OutlineColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(end = 8.dp)) {
                content()
            }
        }
    }
}

private fun isPlausibleTemperature(value: Double): Boolean = (value in 30.0..45.0) || (value in 86.0..113.0)

private fun parseTemperature(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

@Composable
fun TummyTimeGoalWidgetCard(
    goalMinutes: Int,
    todayMinutes: Int,
    onAddMinutes: (Int) -> Unit,
    onRemoveWidget: () -> Unit
) {
    val progress = if (goalMinutes > 0) {
        (todayMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    WidgetCardShell(title = "Tummy time", onRemoveWidget = onRemoveWidget) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$todayMinutes of $goalMinutes min",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = PrimaryContainer,
            trackColor = SurfaceContainerLow
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onAddMinutes(5) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("+5 min", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { onAddMinutes(10) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("+10 min", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun TemperatureWidgetCard(
    temperatures: List<TemperatureEvent>,
    onLogTemperature: (value: Double, notes: String) -> Unit,
    onRemoveWidget: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val parsed = parseTemperature(input)
    val canLog = parsed != null && isPlausibleTemperature(parsed)
    val latest = temperatures.maxByOrNull { it.timeMillis }

    WidgetCardShell(title = "Temperature", onRemoveWidget = onRemoveWidget) {
        Spacer(modifier = Modifier.height(4.dp))
        if (latest != null) {
            Text(
                text = String.format(Locale.US, "%.1f°", latest.temperature),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryColor
            )
            val caregiver = latest.caregiver?.takeIf { it.isNotBlank() }
            val meta = listOfNotNull(latest.timeFormatted.takeIf { it.isNotBlank() }, caregiver)
                .joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "No readings today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("36.8") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val value = parseTemperature(input)
                    if (value != null && isPlausibleTemperature(value)) {
                        onLogTemperature(value, "")
                        input = ""
                    }
                },
                enabled = canLog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    contentColor = OnPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Log", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Latest weight, length and head circumference, each with its WHO percentile, and a button to log new ones. */
@Composable
fun GrowthWidgetCard(
    growth: List<GrowthEvent>,
    birthDate: String?,
    sex: BabySex?,
    onLogGrowth: (values: Map<GrowthKind, Double>, dateMillis: Long, notes: String) -> Unit,
    onRemoveWidget: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val latest = latestGrowth(growth)

    WidgetCardShell(title = "Growth", onRemoveWidget = onRemoveWidget) {
        Spacer(modifier = Modifier.height(4.dp))
        if (latest.isEmpty()) {
            Text(
                text = "No measurements yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        GrowthKind.entries.forEach { kind ->
            val event = latest[kind] ?: return@forEach
            val percentile = growthPercentile(event, birthDate, sex)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(kind.shortLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatGrowth(kind, event.value),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (percentile != null) {
                        Text(
                            "${ordinalPercentile(percentile)} percentile",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        shortDate(event.dateMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (latest.isNotEmpty() && sex == null) {
            Text(
                text = "Choose boy or girl in Settings → You to see WHO percentiles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = OnPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log growth", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showDialog) {
        GrowthDialog(
            latest = latest,
            onDismiss = { showDialog = false },
            onSave = { values, date, notes ->
                onLogGrowth(values, date, notes)
                showDialog = false
            }
        )
    }
}

@Composable
fun NotesWidgetCard(
    notes: List<NoteEvent>,
    onAddNote: (String) -> Unit,
    onRemoveWidget: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val latestNotes = notes.sortedByDescending { it.timeMillis }.take(3)

    WidgetCardShell(title = "Notes", onRemoveWidget = onRemoveWidget) {
        Spacer(modifier = Modifier.height(4.dp))
        if (latestNotes.isEmpty()) {
            Text(
                text = "No notes yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                latestNotes.forEach { note ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (note.timeFormatted.isNotBlank()) {
                            Text(
                                text = note.timeFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = note.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Add a note") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        onAddNote(text)
                        input = ""
                    }
                },
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    contentColor = OnPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Add", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
