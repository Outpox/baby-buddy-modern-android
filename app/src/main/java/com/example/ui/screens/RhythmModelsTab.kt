package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ml.AutoMl
import com.example.data.ml.RhythmModel
import com.example.data.ml.RhythmTarget
import com.example.data.model.TimeFormat
import com.example.data.model.TrackedItem
import com.example.data.model.formatMinutes
import com.example.ui.MainViewModel.ModelsState
import com.example.ui.theme.ItemColors
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.SecondaryColor
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(20.dp)

/** The tracked item whose colour a rhythm uses, and whether it's tracked at all on this phone. */
private fun RhythmTarget.item(): TrackedItem = when (this) {
    RhythmTarget.FEED_GAP -> TrackedItem.BREASTFEEDING
    RhythmTarget.DIAPER_GAP -> TrackedItem.DIAPERS
    RhythmTarget.SLEEP_LENGTH, RhythmTarget.AWAKE_WINDOW -> TrackedItem.SLEEP
}

private fun RhythmTarget.hidden(hidden: Set<TrackedItem>) = when (this) {
    RhythmTarget.FEED_GAP -> TrackedItem.BREASTFEEDING in hidden && TrackedItem.BOTTLE in hidden
    else -> item() in hidden
}

/**
 * Trends → Models: for each rhythm (feed gaps, diaper gaps, sleep length, awake windows) AutoML fits linear
 * regression, boosted trees, a small neural net and nearest neighbours on the phone, keeps the one that best
 * predicted the most recent entries, and uses it for "next feed around 15:40".
 */
@Composable
fun RhythmModelsTab(state: ModelsState, onTrain: (days: Int) -> Unit, hiddenItems: Set<TrackedItem>) {
    var days by remember { mutableStateOf((state as? ModelsState.Ready)?.days ?: 60) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Each rhythm gets its own model, chosen automatically: linear regression, boosted trees, a small " +
                "neural net and nearest neighbours are each tried with several settings, scored on how well they " +
                "predicted your latest entries, and the best one is kept. Everything runs on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(30, 60, 90).forEach { option ->
                val selected = option == days
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) PrimaryColor else SurfaceContainerLow)
                        .clickable { days = option }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("$option days", style = MaterialTheme.typography.labelLarge, color = if (selected) OnPrimary else OnSurfaceVariant)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onTrain(days) },
                enabled = state !is ModelsState.Training,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = OnPrimary)
            ) {
                Text(if (state is ModelsState.Ready) "Retrain" else "Train")
            }
        }
        when (state) {
            ModelsState.Idle -> Text(
                "Pick how much history to learn from and tap Train. It takes a few seconds.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )

            is ModelsState.Training -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.step, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth(), color = PrimaryColor)
            }

            is ModelsState.Failed -> Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)

            is ModelsState.Ready -> {
                Text(
                    "Trained on the last ${state.days} days at ${TimeFormat.clock(state.trainedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                state.models.filterNot { it.target.hidden(hiddenItems) }.forEach { ModelCard(it) }
            }
        }
    }
}

@Composable
private fun ModelCard(model: RhythmModel) {
    val tint = ItemColors.tint(model.target.item())
    val soft = ItemColors.soft(model.target.item())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.width(8.dp))
            Text(model.target.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = OnSurface)
            Spacer(Modifier.weight(1f))
            Text("${model.samples} examples", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
        }
        val result = model.result
        if (result == null) {
            Text(
                "Not enough entries yet: ${model.samples} of the ${AutoMl.MIN_ROWS} needed. Try a longer range.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
            return@Column
        }
        Prediction(model)
        Text(
            "Best: ${result.best.family} (${result.best.settings}), typically off by ${formatMinutes(result.best.mae.roundToInt())}" +
                if (result.best.mae < result.baseline.mae) {
                    ", ${((1 - result.best.mae / result.baseline.mae) * 100).roundToInt()}% better than the typical interval"
                } else {
                    "; nothing beat the typical interval yet"
                },
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
        // Leaderboard: each family's best settings
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Models tried", style = MaterialTheme.typography.labelLarge, color = OnSurface)
            val worst = result.leaderboard.maxOf { it.mae }.coerceAtLeast(1.0)
            result.leaderboard.forEach { score ->
                val isBest = score == result.best
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            score.family,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                            color = OnSurface
                        )
                        Text(score.settings, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    }
                    Bar(fraction = (score.mae / worst).toFloat(), color = if (isBest) tint else soft, modifier = Modifier.width(96.dp))
                    Text(
                        "±${formatMinutes(score.mae.roundToInt())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.width(64.dp).padding(start = 8.dp)
                    )
                }
            }
        }
        // What the winner relies on
        val important = result.importance.filter { it.second > 0.05 }.take(5)
        if (important.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("What matters most", style = MaterialTheme.typography.labelLarge, color = OnSurface)
                val top = important.first().second
                important.forEach { (name, gain) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = MaterialTheme.typography.bodySmall, color = OnSurface, modifier = Modifier.weight(1f))
                        Bar(fraction = (gain / top).toFloat(), color = tint, modifier = Modifier.width(96.dp))
                        Text(
                            "+${formatMinutes(gain.roundToInt().coerceAtLeast(1))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.width(64.dp).padding(start = 8.dp)
                        )
                    }
                }
                Text(
                    "How much worse the predictions get when that input is scrambled.",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
        if (result.lastFold.size >= 3) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Latest entries: actual vs predicted", style = MaterialTheme.typography.labelLarge, color = OnSurface)
                ActualVsPredicted(result.lastFold, actualColor = soft, predictedColor = tint)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendSwatch(soft, "Actual")
                    LegendSwatch(tint, "Predicted")
                }
            }
        }
    }
}

@Composable
private fun Prediction(model: RhythmModel) {
    val minutes = model.predictedMinutes ?: return
    val anchor = model.anchorMillis ?: return
    val at = anchor + (minutes * 60_000).toLong()
    val untilMinutes = ((at - System.currentTimeMillis()) / 60_000L).toInt()
    val headline = when (model.target) {
        RhythmTarget.FEED_GAP -> "Next feed around ${TimeFormat.clock(at)}"
        RhythmTarget.DIAPER_GAP -> "Next change around ${TimeFormat.clock(at)}"
        RhythmTarget.SLEEP_LENGTH -> "Wake-up around ${TimeFormat.clock(at)}"
        RhythmTarget.AWAKE_WINDOW -> "Ready to sleep around ${TimeFormat.clock(at)}"
    }
    Column {
        Text(headline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurface)
        Text(
            if (untilMinutes >= 0) "In ${formatMinutes(untilMinutes)}" else "${formatMinutes(-untilMinutes)} ago",
            style = MaterialTheme.typography.bodyMedium,
            color = if (untilMinutes >= 0) SecondaryColor else OnSurfaceVariant
        )
    }
}

@Composable
private fun Bar(fraction: Float, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(modifier.height(8.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerLow)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(8.dp).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
private fun LegendSwatch(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
    }
}

/** Actual (dots) and predicted (line) intervals for the held-out entries, oldest left. */
@Composable
private fun ActualVsPredicted(points: List<Pair<Double, Double>>, actualColor: androidx.compose.ui.graphics.Color, predictedColor: androidx.compose.ui.graphics.Color) {
    val max = points.maxOf { maxOf(it.first, it.second) }.coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val step = if (points.size > 1) size.width / (points.size - 1) else 0f
        fun y(v: Double) = size.height - (v / max).toFloat() * size.height
        points.forEachIndexed { i, (actual, _) -> drawCircle(actualColor, radius = 3.dp.toPx(), center = Offset(i * step, y(actual))) }
        val path = Path()
        points.forEachIndexed { i, (_, predicted) ->
            if (i == 0) path.moveTo(0f, y(predicted)) else path.lineTo(i * step, y(predicted))
        }
        drawPath(path, predictedColor, style = Stroke(width = 2.dp.toPx()))
    }
}
