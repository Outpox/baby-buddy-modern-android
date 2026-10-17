package com.example

import com.example.data.ml.AutoMl
import com.example.data.ml.BoostedTrees
import com.example.data.ml.Dataset
import com.example.data.ml.NeuralNet
import com.example.data.ml.RhythmEvents
import com.example.data.ml.RhythmTarget
import com.example.data.ml.RidgeRegression
import com.example.data.ml.trainRhythmModels
import com.example.data.model.DiaperEvent
import com.example.data.model.DiaperType
import com.example.data.model.FeedType
import com.example.data.model.FeedingEvent
import com.example.data.model.SleepEvent
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
}
