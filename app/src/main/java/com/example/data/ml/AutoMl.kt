package com.example.data.ml

import kotlin.math.abs
import kotlin.random.Random

/** Rows in time order (oldest first), a target per row, and what each input column is. */
class Dataset(val x: Array<DoubleArray>, val y: DoubleArray, val features: List<String>) {
    val size: Int get() = y.size
}

/** One model family with one choice of settings, e.g. boosted trees with 150 trees of depth 3. */
data class Candidate(val family: String, val settings: String, val trainer: Trainer)

/** How a candidate did on the held-out folds: mean absolute error, in the target's unit (minutes). */
data class Score(val family: String, val settings: String, val mae: Double)

data class AutoMlResult(
    val best: Score,
    val baseline: Score,
    /** Best settings of each family, best first (the baseline included). */
    val leaderboard: List<Score>,
    /** The winner refitted on every row. */
    val model: Regressor,
    /** How much worse the winner gets when one input is shuffled (on the last fold), biggest first. */
    val importance: List<Pair<String, Double>>,
    /** Actual and predicted targets on the last held-out fold, oldest first. */
    val lastFold: List<Pair<Double, Double>>
)

object AutoMl {
    const val MIN_ROWS = 20

    /** Every model family and the settings tried for it. */
    fun candidates(): List<Candidate> = buildList {
        add(Candidate("Typical (median)", "baseline", MedianBaseline))
        listOf(0.1, 1.0, 10.0, 100.0).forEach { add(Candidate("Linear regression", "ridge λ=$it", RidgeRegression(it))) }
        for (depth in listOf(2, 3)) {
            for (trees in listOf(50, 150)) add(Candidate("Boosted trees", "$trees trees, depth $depth", BoostedTrees(trees, depth)))
        }
        for (hidden in listOf(4, 12)) {
            for (decay in listOf(1e-3, 1e-2)) add(Candidate("Neural net", "$hidden hidden, decay $decay", NeuralNet(hidden, decay)))
        }
        listOf(5, 15).forEach { add(Candidate("Nearest neighbours", "k=$it", NearestNeighbours(it))) }
    }

    /**
     * Rolling-origin validation: the last [folds] blocks of rows are each predicted by models trained only on
     * the rows before them (as a model would be used: on what came before). Every candidate is scored by its
     * mean absolute error over those blocks; the best one is refitted on all rows.
     */
    fun fit(data: Dataset, folds: Int = 3, candidates: List<Candidate> = candidates(), onProgress: (Float) -> Unit = {}): AutoMlResult {
        require(data.size >= MIN_ROWS) { "Need at least $MIN_ROWS rows" }
        val n = data.size
        val k = if (n < 40) 1 else folds
        val block = maxOf(n / 5 / k, 3)
        val splits = (k downTo 1).map { i -> (n - i * block) to (n - (i - 1) * block) } // (train end = test start, test end)
        var done = 0
        val scored = candidates.map { c ->
            var errorSum = 0.0
            var count = 0
            for ((testStart, testEnd) in splits) {
                val model = c.trainer.fit(data.x.copyOfRange(0, testStart), data.y.copyOfRange(0, testStart))
                for (i in testStart until testEnd) {
                    errorSum += abs(model.predict(data.x[i]) - data.y[i])
                    count++
                }
            }
            onProgress(++done / (candidates.size + 2f))
            c to Score(c.family, c.settings, errorSum / count)
        }
        val (bestCandidate, best) = scored.minBy { it.second.mae }
        val baseline = scored.first { it.first.trainer === MedianBaseline }.second
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
        onProgress((candidates.size + 1f) / (candidates.size + 2f))

        val model = bestCandidate.trainer.fit(data.x, data.y)
        onProgress(1f)
        return AutoMlResult(best, baseline, leaderboard, model, importance, lastFold)
    }
}
