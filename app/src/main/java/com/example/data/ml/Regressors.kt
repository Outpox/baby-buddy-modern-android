package com.example.data.ml

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

/** A fitted model: one number out for a row of inputs. */
fun interface Regressor {
    fun predict(x: DoubleArray): Double
}

/** Fits a [Regressor] to rows [x] and targets [y]. Deterministic (seeded), so AutoML picks are repeatable. */
fun interface Trainer {
    fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor
}

/** Column means and standard deviations, to put inputs on one scale (a constant column gets std 1). */
internal class Scaler(x: Array<DoubleArray>) {
    private val mean = DoubleArray(x[0].size) { j -> x.sumOf { it[j] } / x.size }
    private val std = DoubleArray(x[0].size) { j ->
        val v = x.sumOf { (it[j] - mean[j]).let { d -> d * d } } / x.size
        sqrt(v).takeIf { it > 1e-9 } ?: 1.0
    }

    fun apply(row: DoubleArray) = DoubleArray(row.size) { (row[it] - mean[it]) / std[it] }
}

internal fun median(values: DoubleArray): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
}

/** Always the training median: the "typical gap" every model has to beat. */
object MedianBaseline : Trainer {
    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor {
        val m = median(y)
        return Regressor { m }
    }
}

/** Linear regression with an L2 penalty [lambda] on standardized inputs (closed form). */
class RidgeRegression(private val lambda: Double) : Trainer {
    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor {
        val scaler = Scaler(x)
        val z = x.map(scaler::apply)
        val p = z[0].size
        val yMean = y.average()
        // (ZᵀZ + λI) w = Zᵀ(y - ȳ); the intercept is ȳ since Z is centered
        val a = Array(p) { i -> DoubleArray(p) { j -> z.sumOf { it[i] * it[j] } + if (i == j) lambda else 0.0 } }
        val b = DoubleArray(p) { i -> z.indices.sumOf { r -> z[r][i] * (y[r] - yMean) } }
        val w = solve(a, b)
        return Regressor { row ->
            val s = scaler.apply(row)
            yMean + s.indices.sumOf { w[it] * s[it] }
        }
    }

    /** Gaussian elimination with partial pivoting; a singular system gives zeros for the free weights. */
    private fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
        val n = b.size
        for (col in 0 until n) {
            val pivot = (col until n).maxByOrNull { abs(a[it][col]) }!!
            if (abs(a[pivot][col]) < 1e-12) continue
            a[col] = a[pivot].also { a[pivot] = a[col] }
            b[col] = b[pivot].also { b[pivot] = b[col] }
            for (r in col + 1 until n) {
                val f = a[r][col] / a[col][col]
                if (f == 0.0) continue
                for (c in col until n) a[r][c] -= f * a[col][c]
                b[r] -= f * b[col]
            }
        }
        val w = DoubleArray(n)
        for (r in n - 1 downTo 0) {
            if (abs(a[r][r]) < 1e-12) continue
            w[r] = (b[r] - (r + 1 until n).sumOf { a[r][it] * w[it] }) / a[r][r]
        }
        return w
    }
}

/**
 * Gradient-boosted regression trees (squared loss): [trees] trees of depth [depth], each fitted to what the
 * ones before got wrong, scaled by [learningRate]; every tree sees a random [subsample] of the rows.
 */
class BoostedTrees(
    private val trees: Int,
    private val depth: Int,
    private val learningRate: Double = 0.1,
    private val minLeaf: Int = 5,
    private val subsample: Double = 0.8,
    private val seed: Int = 7
) : Trainer {
    private sealed interface Node {
        fun eval(x: DoubleArray): Double
    }

    private class Leaf(val value: Double) : Node {
        override fun eval(x: DoubleArray) = value
    }

    private class Split(val feature: Int, val threshold: Double, val left: Node, val right: Node) : Node {
        override fun eval(x: DoubleArray) = if (x[feature] <= threshold) left.eval(x) else right.eval(x)
    }

    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor {
        val random = Random(seed)
        val base = y.average()
        val f = DoubleArray(y.size) { base }
        val forest = ArrayList<Node>(trees)
        repeat(trees) {
            val residual = DoubleArray(y.size) { y[it] - f[it] }
            val rows = y.indices.filter { random.nextDouble() < subsample }.ifEmpty { y.indices.toList() }
            val tree = grow(x, residual, rows, depth)
            forest += tree
            for (i in y.indices) f[i] += learningRate * tree.eval(x[i])
        }
        return Regressor { row -> base + learningRate * forest.sumOf { it.eval(row) } }
    }

    private fun grow(x: Array<DoubleArray>, r: DoubleArray, rows: List<Int>, levels: Int): Node {
        val mean = rows.sumOf { r[it] } / rows.size
        if (levels == 0 || rows.size < 2 * minLeaf) return Leaf(mean)
        val total = rows.sumOf { r[it] }
        var bestGain = 1e-9
        var bestFeature = -1
        var bestThreshold = 0.0
        for (j in x[0].indices) {
            val sorted = rows.sortedBy { x[it][j] }
            var leftSum = 0.0
            // Maximizing sum²/n on both sides is minimizing the squared error
            for (k in 0 until sorted.size - 1) {
                leftSum += r[sorted[k]]
                val nLeft = k + 1
                val nRight = sorted.size - nLeft
                if (nLeft < minLeaf || nRight < minLeaf) continue
                val here = x[sorted[k]][j]
                val next = x[sorted[k + 1]][j]
                if (here == next) continue
                val rightSum = total - leftSum
                val gain = leftSum * leftSum / nLeft + rightSum * rightSum / nRight - total * total / sorted.size
                if (gain > bestGain) {
                    bestGain = gain
                    bestFeature = j
                    bestThreshold = (here + next) / 2
                }
            }
        }
        if (bestFeature < 0) return Leaf(mean)
        val (left, right) = rows.partition { x[it][bestFeature] <= bestThreshold }
        return Split(bestFeature, bestThreshold, grow(x, r, left, levels - 1), grow(x, r, right, levels - 1))
    }
}

/**
 * A small neural network: one hidden layer of [hidden] tanh units and a linear output, trained with Adam on
 * standardized inputs and targets for [epochs] passes in mini-batches, with L2 [weightDecay].
 */
class NeuralNet(
    private val hidden: Int,
    private val weightDecay: Double,
    private val epochs: Int = 150,
    private val learningRate: Double = 0.01,
    private val batch: Int = 32,
    private val seed: Int = 11
) : Trainer {
    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor {
        val random = Random(seed)
        val scaler = Scaler(x)
        val z = x.map(scaler::apply)
        val p = z[0].size
        val yMean = y.average()
        val yStd = sqrt(y.sumOf { (it - yMean) * (it - yMean) } / y.size).takeIf { it > 1e-9 } ?: 1.0
        val t = DoubleArray(y.size) { (y[it] - yMean) / yStd }

        // Parameters in one array: w1 (hidden × p), b1 (hidden), w2 (hidden), b2
        val w1 = 0
        val b1 = hidden * p
        val w2 = b1 + hidden
        val b2 = w2 + hidden
        val size = b2 + 1
        val scale = sqrt(1.0 / p)
        val theta = DoubleArray(size) { i ->
            if (i < b1) {
                (random.nextDouble() * 2 - 1) * scale
            } else if (i in w2 until b2) {
                (random.nextDouble() * 2 - 1) * sqrt(1.0 / hidden)
            } else {
                0.0
            }
        }
        val m = DoubleArray(size)
        val v = DoubleArray(size)
        val grad = DoubleArray(size)
        val h = DoubleArray(hidden)
        var step = 0
        val order = z.indices.toMutableList()
        repeat(epochs) {
            order.shuffle(random)
            for (start in order.indices step batch) {
                grad.fill(0.0)
                val end = minOf(start + batch, order.size)
                for (k in start until end) {
                    val row = z[order[k]]
                    for (u in 0 until hidden) {
                        var a = theta[b1 + u]
                        for (j in 0 until p) a += theta[w1 + u * p + j] * row[j]
                        h[u] = kotlin.math.tanh(a)
                    }
                    var out = theta[b2]
                    for (u in 0 until hidden) out += theta[w2 + u] * h[u]
                    val err = out - t[order[k]] // d(½err²)/d(out)
                    grad[b2] += err
                    for (u in 0 until hidden) {
                        grad[w2 + u] += err * h[u]
                        val back = err * theta[w2 + u] * (1 - h[u] * h[u])
                        grad[b1 + u] += back
                        for (j in 0 until p) grad[w1 + u * p + j] += back * row[j]
                    }
                }
                val n = (end - start).toDouble()
                step++
                for (i in 0 until size) {
                    val g = grad[i] / n + if (i < b1 || i in w2 until b2) weightDecay * theta[i] else 0.0
                    m[i] = 0.9 * m[i] + 0.1 * g
                    v[i] = 0.999 * v[i] + 0.001 * g * g
                    val mHat = m[i] / (1 - Math.pow(0.9, step.toDouble()))
                    val vHat = v[i] / (1 - Math.pow(0.999, step.toDouble()))
                    theta[i] -= learningRate * mHat / (sqrt(vHat) + 1e-8)
                }
            }
        }
        return Regressor { raw ->
            val row = scaler.apply(raw)
            var out = theta[b2]
            for (u in 0 until hidden) {
                var a = theta[b1 + u]
                for (j in 0 until p) a += theta[w1 + u * p + j] * row[j]
                out += theta[w2 + u] * kotlin.math.tanh(a)
            }
            yMean + yStd * out
        }
    }
}

/** k nearest neighbours (standardized inputs), averaging their targets weighted by closeness. */
class NearestNeighbours(private val k: Int) : Trainer {
    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor {
        val scaler = Scaler(x)
        val z = x.map(scaler::apply)
        return Regressor { raw ->
            val row = scaler.apply(raw)
            val nearest = z.indices.map { i -> i to z[i].indices.sumOf { (z[i][it] - row[it]).let { d -> d * d } } }
                .sortedBy { it.second }
                .take(k)
            val weights = nearest.map { exp(-sqrt(it.second) / 2) + 1e-9 }
            nearest.indices.sumOf { weights[it] * y[nearest[it].first] } / weights.sum()
        }
    }
}
