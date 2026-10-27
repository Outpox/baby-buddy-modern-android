package com.example

import com.example.data.ml.AutoMl
import com.example.data.ml.BoostedTrees
import com.example.data.ml.Candidate
import com.example.data.ml.Dataset
import com.example.data.ml.FixedValue
import com.example.data.ml.NeuralNet
import com.example.data.ml.Regressor
import com.example.data.ml.RhythmEvents
import com.example.data.ml.RhythmPrediction
import com.example.data.ml.RhythmTarget
import com.example.data.ml.RidgeRegression
import com.example.data.ml.SavedRhythmModel
import com.example.data.ml.TimeOfDayMedian
import com.example.data.ml.predictRhythms
import com.example.data.ml.toText
import com.example.data.ml.trainRhythmModels
import com.example.data.model.DiaperEvent
import com.example.data.model.DiaperType
import com.example.data.model.FeedType
import com.example.data.model.FeedingEvent
import com.example.data.model.SleepEvent
import com.example.data.repository.generateDemoHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlin.math.abs
import kotlin.random.Random

class RhythmModelsTest {
    private val random = Random(1)
    private val x = Array(300) { DoubleArray(3) { random.nextDouble() * 10 } }

    private fun mae(trainer: com.example.data.ml.Trainer, f: (DoubleArray) -> Double): Double {
        val y = DoubleArray(x.size) { f(x[it]) }
        val model = trainer.fit(x.copyOfRange(0, 250), y.copyOfRange(0, 250))
        return (250 until 300).sumOf { abs(model.predict(x[it]) - y[it]) } / 50
    }

    @Test
    fun `ridge recovers a linear function`() {
        assertTrue(mae(RidgeRegression(0.01)) { 3 * it[0] - 2 * it[1] + 5 } < 0.05)
    }

    @Test
    fun `boosted trees and the neural net learn a step the linear model can't`() {
        val step = { r: DoubleArray -> if (r[0] > 5) 100.0 else 20.0 }
        val linear = mae(RidgeRegression(0.1), step)
        assertTrue(mae(BoostedTrees(100, 2), step) < linear / 3)
        assertTrue(mae(NeuralNet(8, 1e-3), step) < linear)
    }

    @Test
    fun `automl picks a model that beats the typical value and reports every family`() {
        val y = DoubleArray(x.size) { if (x[it][0] > 5) 100.0 + x[it][1] else 20.0 }
        val result = AutoMl.fit(Dataset(x, y, listOf("a", "b", "c")))
        assertTrue(result.best.mae < result.baseline.mae / 3)
        assertEquals(5, result.leaderboard.size)
        assertEquals("a", result.importance.first().first)
    }

    /** A baby who feeds every 2h by day and every 4h by night, with diapers and sleeps in between. */
    private fun syntheticEvents(days: Int): RhythmEvents {
        val start = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
        val end = start + days * 24 * 3_600_000L
        val feeds = mutableListOf<FeedingEvent>()
        val changes = mutableListOf<DiaperEvent>()
        val sleeps = mutableListOf<SleepEvent>()
        var t = start
        while (t < end) {
            val hour = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.HOUR_OF_DAY)
            val night = hour >= 20 || hour < 6
            val gapMinutes = (if (night) 240 else 120) + random.nextInt(-10, 11)
            feeds += FeedingEvent(id = t, timeMillis = t, type = FeedType.NURSING, leftDurationSec = 600, rightDurationSec = 300)
            changes += DiaperEvent(id = t, timeMillis = t + 20 * 60_000L, type = DiaperType.WET)
            val sleepStart = t + 30 * 60_000L
            sleeps += SleepEvent(id = t, timeMillis = sleepStart, endMillis = sleepStart + (gapMinutes - 45) * 60_000L, isNap = !night)
            t += gapMinutes * 60_000L
        }
        return RhythmEvents(feeds, changes, sleeps)
    }

    @Test
    fun `rhythm models learn the day-night feeding pattern and predict the next feed`() {
        val models = trainRhythmModels(syntheticEvents(30))
        assertEquals(RhythmTarget.entries.size, models.size)
        val feed = models.first { it.target == RhythmTarget.FEED_GAP }
        val result = requireNotNull(feed.result)
        // The gap is 120 or 240 by time of day (±10): a good model is off by far less than the typical gap's error
        assertTrue("best ${result.best} baseline ${result.baseline}", result.best.mae < 20)
        assertTrue(result.best.mae < result.baseline.mae / 2)
        assertNotNull(feed.predictedMinutes)
        assertNotNull(feed.anchorMillis)
    }

    @Test
    fun `too little history gives no model`() {
        val models = trainRhythmModels(syntheticEvents(1))
        assertNull(models.first { it.target == RhythmTarget.FEED_GAP }.result)
    }

    @Test
    fun `every model predicts the same after saving and loading`() {
        val y = DoubleArray(x.size) { 3 * x[it][0] + if (x[it][1] > 5) 40.0 else 0.0 }
        AutoMl.candidates(listOf("Time of day (sin)", "Time of day (cos)", "Age (days)")).forEach { c ->
            val model = c.trainer.fit(x, y)
            val loaded = Regressor.fromText(model.toText())
            x.take(20).forEach { row -> assertEquals(c.family, model.predict(row), loaded.predict(row), 1e-9) }
        }
    }

    @Test
    fun `the time-of-day median uses recent intervals from the same part of the day`() {
        // Columns: sin and cos of the time of day, age in days; night intervals (00–06) are long, the rest short
        val rows = (0 until 400).map { i ->
            val minute = (i * 97) % 1440
            val angle = 2 * Math.PI * minute / 1440
            doubleArrayOf(kotlin.math.sin(angle), kotlin.math.cos(angle), i / 10.0) to if (minute < 360) 240.0 else 150.0
        }
        // Older rows say something else: only the last week counts
        val x = rows.map { it.first }.toTypedArray()
        val y = DoubleArray(rows.size) { if (x[it][2] < 30) 60.0 else rows[it].second }
        val model = TimeOfDayMedian(6, 0, 7, 0, 1, 2).fit(x, y)
        val at = { minute: Int -> 2 * Math.PI * minute / 1440 }
        assertEquals(240.0, model.predict(doubleArrayOf(kotlin.math.sin(at(180)), kotlin.math.cos(at(180)), 40.0)), 1e-9)
        assertEquals(150.0, model.predict(doubleArrayOf(kotlin.math.sin(at(900)), kotlin.math.cos(at(900)), 40.0)), 1e-9)
    }

    @Test
    fun `an overdue prediction moves to the middle of the outcomes that ran longer`() {
        val minute = 60_000L
        val p = RhythmPrediction(anchorMillis = 0L, dueMillis = 180 * minute, residuals = listOf(-30.0, -10.0, 0.0, 10.0, 20.0, 40.0, 60.0))
        assertEquals(180 * minute, p.expectedMillis(100 * minute)) // not due yet: unchanged
        // At 205 min, outcomes still possible are +10 (no: 190 < 205), +20 → 200 (no), +40 → 220, +60 → 240
        assertEquals(230 * minute, p.expectedMillis(205 * minute))
        assertEquals(300 * minute, p.expectedMillis(300 * minute)) // later than ever before: now
        assertEquals(180 * minute, RhythmPrediction(0L, 180 * minute).expectedMillis(300 * minute)) // no errors saved
    }

    @Test
    fun `the default estimate is scored for comparison but never picked`() {
        val y = DoubleArray(x.size) { 100.0 + x[it][0] }
        // A fixed interval that happens to be nearly perfect still isn't a model to save
        val result = AutoMl.fit(Dataset(x, y, listOf("a", "b", "c")), default = Candidate("Default", "fixed", FixedValue(105.0)))
        val default = requireNotNull(result.default)
        assertTrue(default.reference)
        assertTrue(default in result.leaderboard)
        assertTrue(!result.best.reference)
    }

    @Test
    fun `a saved model predicts the next feed from recent entries`() {
        val events = syntheticEvents(30)
        val model = trainRhythmModels(events, mapOf(RhythmTarget.FEED_GAP to 180)).first { it.target == RhythmTarget.FEED_GAP }
        val result = requireNotNull(model.result)
        assertEquals("fixed 3h 00m from Settings", result.default?.settings)
        val saved = SavedRhythmModel(
            RhythmTarget.FEED_GAP, result.best.family, result.best.settings, result.best.mae, result.model, model.ageOriginMillis ?: 0L, 1L, model.inputs,
            result.best.residuals
        )
        val reloaded = requireNotNull(SavedRhythmModel.fromText(saved.toText()))
        assertEquals(saved.residuals, reloaded.residuals)
        assertTrue(reloaded.residuals.size in 1..49)
        // Only the last two days, as the app keeps for predicting
        val since = events.feeds.last().timeMillis - 2 * 24 * 3_600_000L
        val recent = RhythmEvents(events.feeds.filter { it.timeMillis >= since }, events.changes.filter { it.timeMillis >= since }, events.sleeps.filter { it.timeMillis >= since })
        val prediction = requireNotNull(predictRhythms(recent, listOf(reloaded))[RhythmTarget.FEED_GAP])
        assertEquals(events.feeds.last().timeMillis, prediction.anchorMillis)
        assertEquals(model.predictedMinutes!!, (prediction.dueMillis - prediction.anchorMillis) / 60_000.0, 20.0)
        // A model saved for other inputs (an app update changed them) gives no prediction
        val stale = SavedRhythmModel(RhythmTarget.FEED_GAP, "x", "y", 1.0, result.model, 0L, 1L, model.inputs + 1)
        assertNull(predictRhythms(recent, listOf(stale))[RhythmTarget.FEED_GAP])
    }

    @Test
    fun `the demo data trains every rhythm, and models beat the default estimate`() {
        val now = System.currentTimeMillis()
        val demo = generateDemoHistory(now = now, birthMillis = now - 270 * 86_400_000L) { "" }
        assertTrue(demo.growth.size >= 27) // monthly weight, length and head since birth
        val from = now - 60 * 86_400_000L
        val events = RhythmEvents(
            demo.feeds.filter { it.timeMillis >= from },
            demo.diapers.filter { it.timeMillis >= from },
            demo.sleeps.filter { it.timeMillis >= from },
            birthMillis = now - 270 * 86_400_000L
        )
        val defaults = mapOf(RhythmTarget.FEED_GAP to 180, RhythmTarget.DIAPER_GAP to 180, RhythmTarget.SLEEP_LENGTH to 150, RhythmTarget.AWAKE_WINDOW to 120)
        val models = trainRhythmModels(events, defaults)
        models.forEach { m ->
            val result = requireNotNull(m.result) { "${m.target}: ${m.samples} rows" }
            val default = requireNotNull(result.default)
            println("${m.target}: ${m.samples} rows, best ${result.best.family} ±${result.best.mae.toInt()}m, default ±${default.mae.toInt()}m")
            assertTrue("${m.target} best ${result.best} default $default", result.best.mae < default.mae)
        }
    }
}
