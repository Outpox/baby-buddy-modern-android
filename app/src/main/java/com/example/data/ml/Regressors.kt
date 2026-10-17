package com.example.data.ml

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A fitted model: one number out for a row of inputs. It can be saved as numbers ([encode]) and loaded back
 * ([decode]), so a model picked in Trends keeps predicting for reminders and widgets after a restart.
 */
sealed interface Regressor {
    fun predict(x: DoubleArray): Double

    fun encode(): DoubleArray

    companion object {
        fun decode(values: DoubleArray): Regressor = Reader(values).let { r ->
            when (r.int()) {
                0 -> ConstantModel(r.next())

                1 -> {
                    val p = r.int()
                    LinearModel(r.scaler(p), r.next(), r.array(p))
                }

                2 -> TreesModel(r.next(), r.next(), List(r.int()) { r.node() })

                3 -> {
                    val p = r.int()
                    val hidden = r.int()
                    NetModel(r.scaler(p), r.next(), r.next(), hidden, r.array(hidden * p + 2 * hidden + 1))
                }

                4 -> {
                    val p = r.int()
                    val k = r.int()
                    val n = r.int()
                    val scaler = r.scaler(p)
                    KnnModel(scaler, k, Array(n) { r.array(p) }, r.array(n))
                }

                else -> error("Unknown model")
            }
        }

        /** A saved model as text (numbers separated by commas). */
        fun fromText(text: String): Regressor = decode(text.split(',').map(String::toDouble).toDoubleArray())
    }
}

fun Regressor.toText(): String = encode().joinToString(",")

private class Reader(private val values: DoubleArray) {
    private var at = 0

    fun next() = values[at++]

    fun int() = next().toInt()

    fun array(n: Int) = DoubleArray(n) { next() }

    fun scaler(p: Int) = Scaler(array(p), array(p))

    fun node(): Node {
        val feature = int()
        return if (feature < 0) Leaf(next()) else Split(feature, next(), node(), node())
    }
}

internal class ConstantModel(private val value: Double) : Regressor {
    override fun predict(x: DoubleArray) = value

    override fun encode() = doubleArrayOf(0.0, value)
}

internal class LinearModel(private val scaler: Scaler, private val intercept: Double, private val w: DoubleArray) : Regressor {
    override fun predict(x: DoubleArray): Double {
        val s = scaler.apply(x)
        return intercept + s.indices.sumOf { w[it] * s[it] }
    }

    override fun encode() = doubleArrayOf(1.0, w.size.toDouble()) + scaler.encode() + intercept + w
}

internal sealed interface Node {
    fun eval(x: DoubleArray): Double

    fun encodeTo(out: MutableList<Double>)
}

internal class Leaf(val value: Double) : Node {
    override fun eval(x: DoubleArray) = value

    override fun encodeTo(out: MutableList<Double>) {
        out += -1.0
        out += value
    }
}

internal class Split(val feature: Int, val threshold: Double, val left: Node, val right: Node) : Node {
    override fun eval(x: DoubleArray) = if (x[feature] <= threshold) left.eval(x) else right.eval(x)

    override fun encodeTo(out: MutableList<Double>) {
        out += feature.toDouble()
        out += threshold
        left.encodeTo(out)
        right.encodeTo(out)
    }
}

internal class TreesModel(private val base: Double, private val learningRate: Double, private val forest: List<Node>) : Regressor {
    override fun predict(x: DoubleArray) = base + learningRate * forest.sumOf { it.eval(x) }

    override fun encode(): DoubleArray {
        val out = mutableListOf(2.0, base, learningRate, forest.size.toDouble())
        forest.forEach { it.encodeTo(out) }
        return out.toDoubleArray()
    }
}

internal class NetModel(
    private val scaler: Scaler,
    private val yMean: Double,
    private val yStd: Double,
    private val hidden: Int,
    private val theta: DoubleArray // w1 (hidden × p), b1 (hidden), w2 (hidden), b2
) : Regressor {
    override fun predict(x: DoubleArray): Double {
        val row = scaler.apply(x)
        val p = row.size
        val b1 = hidden * p
        val w2 = b1 + hidden
        var out = theta[w2 + hidden]
        for (u in 0 until hidden) {
            var a = theta[b1 + u]
            for (j in 0 until p) a += theta[u * p + j] * row[j]
            out += theta[w2 + u] * kotlin.math.tanh(a)
        }
        return yMean + yStd * out
    }

    override fun encode() = doubleArrayOf(3.0, scaler.size.toDouble(), hidden.toDouble()) + scaler.encode() + yMean + yStd + theta
}

internal class KnnModel(private val scaler: Scaler, private val k: Int, private val z: Array<DoubleArray>, private val y: DoubleArray) : Regressor {
    override fun predict(x: DoubleArray): Double {
        val row = scaler.apply(x)
        val nearest = z.indices.map { i -> i to z[i].indices.sumOf { (z[i][it] - row[it]).let { d -> d * d } } }
            .sortedBy { it.second }
            .take(k)
        val weights = nearest.map { exp(-sqrt(it.second) / 2) + 1e-9 }
        return nearest.indices.sumOf { weights[it] * y[nearest[it].first] } / weights.sum()
    }

    override fun encode(): DoubleArray {
        val out = mutableListOf(4.0, scaler.size.toDouble(), k.toDouble(), y.size.toDouble())
        out += scaler.encode().toList()
        z.forEach { out += it.toList() }
        out += y.toList()
        return out.toDoubleArray()
    }
}

/** Fits a [Regressor] to rows [x] and targets [y]. Deterministic (seeded), so AutoML picks are repeatable. */
fun interface Trainer {
    fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor
}

/** Column means and standard deviations, to put inputs on one scale (a constant column gets std 1). */
internal class Scaler(private val mean: DoubleArray, private val std: DoubleArray) {
    val size: Int get() = mean.size

    fun apply(row: DoubleArray) = DoubleArray(row.size) { (row[it] - mean[it]) / std[it] }

    fun encode() = mean + std

    companion object {
        fun fit(x: Array<DoubleArray>): Scaler {
            val mean = DoubleArray(x[0].size) { j -> x.sumOf { it[j] } / x.size }
            val std = DoubleArray(x[0].size) { j ->
                val v = x.sumOf { (it[j] - mean[j]).let { d -> d * d } } / x.size
                sqrt(v).takeIf { it > 1e-9 } ?: 1.0
            }
            return Scaler(mean, std)
        }
    }
}

internal fun median(values: DoubleArray): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
}

/** Always the training median: the "typical gap" every model has to beat. */
object MedianBaseline : Trainer {
    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor = ConstantModel(median(y))
}

/** Always [value]: the app's default estimate when it's a fixed interval from Settings. */
class FixedValue(private val value: Double) : Trainer {
    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor = ConstantModel(value)
}

/** Always the training average: the app's default estimate (average interval) when no fixed one is set. */
object MeanBaseline : Trainer {
    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor = ConstantModel(y.average())
}

/** Linear regression with an L2 penalty [lambda] on standardized inputs (closed form). */
class RidgeRegression(private val lambda: Double) : Trainer {
    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor {
        val scaler = Scaler.fit(x)
        val z = x.map(scaler::apply)
        val p = z[0].size
        val yMean = y.average()
        // (ZᵀZ + λI) w = Zᵀ(y - ȳ); the intercept is ȳ since Z is centered
        val a = Array(p) { i -> DoubleArray(p) { j -> z.sumOf { it[i] * it[j] } + if (i == j) lambda else 0.0 } }
        val b = DoubleArray(p) { i -> z.indices.sumOf { r -> z[r][i] * (y[r] - yMean) } }
        val w = solve(a, b)
        return LinearModel(scaler, yMean, w)
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
        return TreesModel(base, learningRate, forest)
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
        val scaler = Scaler.fit(x)
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
        return NetModel(scaler, yMean, yStd, hidden, theta)
    }
}

/** k nearest neighbours (standardized inputs), averaging their targets weighted by closeness. */
class NearestNeighbours(private val k: Int) : Trainer {
    override fun fit(x: Array<DoubleArray>, y: DoubleArray): Regressor {
        val scaler = Scaler.fit(x)
        val z = x.map(scaler::apply)
        return KnnModel(scaler, k, z.toTypedArray(), y.copyOf())
    }
}
