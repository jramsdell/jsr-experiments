package stochastic

import koma.create
import koma.eye
import koma.extensions.*
import koma.matrix.Matrix
import koma.ndarray.NumericalNDArray
import koma.ndarray.NumericalNDArrayFactory
import koma.ndarray.common.NumericalNDArrayFacBase
import koma.ndarray.default.DefaultDoubleNDArray
import koma.rand
import koma.randn
import org.apache.commons.math3.distribution.NormalDistribution
import utils.normalize
import kotlin.math.abs


object StochasticTools {

    fun perturbSequence(seq: DoubleArray, nCopies: Int = 50, normalize: Boolean = true, noiseMult: Double = 0.01): Matrix<Double> {
        val arr = (0 until nCopies).map { seq.clone() }.toTypedArray()
        val mat = create(arr)
        val noise = randn(mat.numRows(), mat.numCols()) * noiseMult
//        println(mat + noise)
        var result = mat + noise

        if (normalize) {
            result = result.mapRows { row -> row / row.elementSum() }
        }

        return result
    }

    fun subTwo(m1: Matrix<Double>, m2: Matrix<Double>) {
        println((m1 - m2).normF())

    }

    fun measureToPerturbed(perturbed: Matrix<Double>, feature: DoubleArray): Matrix<Double> {
        val vec = create(feature)
        val result = perturbed.mapRowsToList { row -> (row - vec).normF() }
            .normalize()
            .toDoubleArray()
        println(perturbed.normF())

        return create(result)
    }

}

fun main(args: Array<String>) {
//    val stuff = doubleArrayOf(1.0, 5.0, 5.0, 2.0)
//    val mat = StochasticTools.perturbSequence(stuff, 10, false)
//    val mat2 = StochasticTools.perturbSequence(stuff, 10, false, noiseMult = 0.0)
////    val res = StochasticTools.measureToPerturbed(mat, stuff)
//    StochasticTools.subTwo(mat, mat2)
//    println(NormalDistribution(0.0, 0.1).probability(0.0, 1.0))
    val norm = NormalDistribution(0.0, 0.001)
    val target = create(doubleArrayOf(0.1, 0.3, 0.2, 0.2, 0.2))
    val feat = create(doubleArrayOf(0.1, 0.3, 0.2, 0.2, 0.2))
//    val res = (14 .. 25).map { i ->
//        val p = norm.inverseCumulativeProbability(i.toDouble() * 0.025)
//        println("${i * 0.025}: $p")
//        p * target - feat
//    }.average()
//    println(res)

//    val res2 = norm.sample(1000).map { r ->  abs(r + target - feat)}.average()
    val res2 = norm.sample(1000).asIterable().chunked(5)
        .map { c ->
            val noise = create(c.toDoubleArray())
            var n = target + noise
            n /= n.elementSum()
            (n - feat).map(::abs).elementSum()
//            (n - feat).normF()
        }.average()
    println(res2)
}


