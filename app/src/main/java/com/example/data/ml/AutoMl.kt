package com.example.data.ml

import kotlin.math.abs
import kotlin.random.Random

/** Rows in time order (oldest first), a target per row, and what each input column is. */
class Dataset(val x: Array<DoubleArray>, val y: DoubleArray, val features: List<String>) {
    val size: Int get() = y.size
}

/**
 * One model family with one choice of settings, e.g. boosted trees with 150 trees of depth 3. A [reference]
 * (the app's default estimate) is scored like the others for comparison but never picked.
 */
data class Candidate(val family: String, val settings: String, val trainer: Trainer, val reference: Boolean = false)

/**
 * How a candidate did on the held-out folds: mean absolute error, in the target's unit (minutes), and each
 * error (actual − predicted), which tells how long an overdue event may still take.
 */
data class Score(val family: String, val settings: String, val mae: Double, val reference: Boolean = false, val residuals: List<Double> = emptyList())

data class AutoMlResult(
    val best: Score,
    val baseline: Score,
    /** The app's default estimate on the same folds, when one was given. */
    val default: Score?,
    /** Best settings of each family, best first (the baseline and the default included). */
    val leaderboard: List<Score>,
    /** The winner refitted on every row. */
    val model: Regressor,
    /** How much worse the winner gets when one input is shuffled (on the last fold), biggest first. */
    val importance: List<Pair<String, Double>>,
    /** Actual and predicted targets on the last held-out fold, oldest first. */
    val lastFold: List<Pair<Double, Double>>,
    /** A leaderboard entry's model fitted on every row (the winner's is [model]). CPU-bound. */
    val refit: (Score) -> Regressor
)

object AutoMl {
    const val MIN_ROWS = 20

    /** Every model family and the settings tried for it; [features] (the input columns) enable the time-of-day medians. */
    fun candidates(features: List<String> = emptyList()): List<Candidate> = buildList {
        add(Candidate("Typical (median)", "baseline", MedianBaseline))
        listOf(0.1, 1.0, 10.0, 100.0).forEach { add(Candidate("Linear regression", "ridge λ=$it", RidgeRegression(it))) }
        for (depth in listOf(2, 3)) {
            for (trees in listOf(50, 150)) add(Candidate("Boosted trees", "$trees trees, depth $depth", BoostedTrees(trees, depth)))
        }
        for (hidden in listOf(4, 12)) {
            for (decay in listOf(1e-3, 1e-2)) add(Candidate("Neural net", "$hidden hidden, decay $decay", NeuralNet(hidden, decay)))
        }
        listOf(5, 15).forEach { add(Candidate("Nearest neighbours", "k=$it", NearestNeighbours(it))) }
        val sin = features.indexOf("Time of day (sin)")
        val cos = features.indexOf("Time of day (cos)")
        val age = features.indexOf("Age (days)")
        if (sin >= 0 && cos >= 0 && age >= 0) {
            listOf(3, 7).forEach { add(Candidate("Recent median", "last $it days", TimeOfDayMedian(24, 0, it, sin, cos, age))) }
            for ((width, start) in listOf(4 to 2, 6 to 0, 12 to 6)) {
                for (days in listOf(7, 14)) {
                    add(Candidate("Time of day", "${width}h windows from %02d:00, last $days days".format(start), TimeOfDayMedian(width, start, days, sin, cos, age)))
                }
            }
        }
    }

    /**
     * Rolling-origin validation: the last [folds] blocks of rows are each predicted by models trained only on
     * the rows before them (as a model would be used: on what came before). Every candidate is scored by its
     * mean absolute error over those blocks; the best one is refitted on all rows.
     */
    fun fit(data: Dataset, folds: Int = 3, candidates: List<Candidate> = candidates(data.features), default: Candidate? = null, onProgress: (Float) -> Unit = {}): AutoMlResult {
        val all = candidates + listOfNotNull(default?.copy(reference = true))
        require(data.size >= MIN_ROWS) { "Need at least $MIN_ROWS rows" }
        val n = data.size
        val k = if (n < 40) 1 else folds
        val block = maxOf(n / 5 / k, 3)
        val splits = (k downTo 1).map { i -> (n - i * block) to (n - (i - 1) * block) } // (train end = test start, test end)
        var done = 0
        val scored = all.map { c ->
            val residuals = ArrayList<Double>()
            for ((testStart, testEnd) in splits) {
                val model = c.trainer.fit(data.x.copyOfRange(0, testStart), data.y.copyOfRange(0, testStart))
                for (i in testStart until testEnd) residuals += data.y[i] - model.predict(data.x[i])
            }
            onProgress(++done / (all.size + 2f))
            c to Score(c.family, c.settings, residuals.sumOf { abs(it) } / residuals.size, c.reference, residuals)
        }
        val (bestCandidate, best) = scored.filter { !it.first.reference }.minBy { it.second.mae }
        val baseline = scored.first { it.first.trainer === MedianBaseline }.second
        val defaultScore = scored.firstOrNull { it.first.reference }?.second
        val leaderboard = scored.map { it.second }.groupBy { it.family }.map { (_, s) -> s.minBy { it.mae } }.sortedBy { it.mae }

        // Importance and the chart come from the winner on the last fold, which it hadn't seen
        val (testStart, testEnd) = splits.last()
        val foldModel = bestCandidate.trainer.fit(data.x.copyOfRange(0, testStart), data.y.copyOfRange(0, testStart))
        val test = (testStart until testEnd).toList()
        val lastFold = test.map { data.y[it] to foldModel.predict(data.x[it]) }
        val foldMae = lastFold.sumOf { abs(it.first - it.second) } / test.size
        val random = Random(3)
        val importance = data.features.indices.map { j ->
            val column = test.map { data.x[it][j] }.shuffled(random)
            val shuffledMae = test.indices.sumOf { t ->
                val row = data.x[test[t]].copyOf().also { it[j] = column[t] }
                abs(foldModel.predict(row) - data.y[test[t]])
            } / test.size
            data.features[j] to (shuffledMae - foldMae).coerceAtLeast(0.0)
        }.sortedByDescending { it.second }
        onProgress((all.size + 1f) / (all.size + 2f))

        val model = bestCandidate.trainer.fit(data.x, data.y)
        onProgress(1f)
        val trainers = scored.associate { it.second to it.first.trainer }
        val refit = { score: Score -> if (score == best) model else trainers.getValue(score).fit(data.x, data.y) }
        return AutoMlResult(best, baseline, defaultScore, leaderboard, model, importance, lastFold, refit)
    }
}
