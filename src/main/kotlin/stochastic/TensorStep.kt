//package stochastic
//
//import edu.unh.cs980.features.featSDMWithEntityQueryExpansion
//import edu.unh.cs980.normalize
//import edu.unh.cs980.sharedRand
//import koma.matrix.Matrix
//import org.apache.commons.math3.distribution.NormalDistribution
//import org.apache.commons.math3.geometry.Vector
//import smile.math.matrix.Matrix
//import smile.math.*
//import smile.math.Math.*
//import kotlin.math.abs
//import kotlin.math.log2
//import kotlin.math.max
//import smile.math.matrix.*
//import smile.regression.LASSO
//import smile.wavelet.*
//import smile.regression.OLS
//import smile.regression.RidgeRegression
//import kotlin.math.sign
//import kotlin.system.exitProcess
//
//
//
///**
// * Func: uniformSmoothing
// * Desc: The values of the target function (our paragraph to be embedded) are averaged out.
// *       It is almost always very close to the uniform distribution.
// */
//fun uniformSmoothing(values: List<Double>): DoubleArray {
//    val total = values.sum()
//    val normal = total / values.size.toDouble()
//    return (0 until values.size).map { normal }.toDoubleArray()
//}
//
//
///**
// * Class: GradientDescenter
// * Desc: Performs gradients descent on KLD. Find the linear combination of distributions (topics) that
// *       minimizes KLD to the uniform distribution.
// */
//class GradientDescenter(val origin: Matrix<Double>, val features: List<Matrix<Double>>) {
//
//    // Warning, lots of hacky shit below. I need to switch to using a real convex optimizer next time instead
//    // of my poor approximation of gradient descent.
//
//    val weightMatrices =
//        (0 until features.size).map { 1.0 }.toDoubleArray()
//
//
//    fun changeWeight(index: Int, weight: Double) =
//        weightMatrices.set(index, weight)
//
//
////    fun doKld() =
////        weightMatrices.zip(topicMatrices)
////            .map { (w,t) -> w.transpose().mul(t) }
////            .reduce { acc, denseMatrix -> acc.add(denseMatrix)  }
////            .run { div(sum()) }
////            .transpose().array()
////            .run { KullbackLeiblerDivergence(originArray, this.first())}
//
//
//
//
//
//    fun getDerivative(baseline: Double, index: Int, weights: List<Double>): Pair<Double, Double> {
//        if (weights[index] <= 0.0) { return 0.0 to 0.0 }
//        val curWeight = weights[index]
//
//        val lower = weights.toMutableList().apply { set(index, curWeight - 0.001) }
//        val upper = weights.toMutableList().apply { set(index, curWeight + 0.001) }
//        val lowerDiff = (kld(lower) - baseline)
//        val upperDiff = (kld(upper) - baseline)
//
//        return if (lowerDiff < upperDiff) -1.0 to abs(lowerDiff) else 1.0 to abs(upperDiff)
//    }
//
//    fun getDerivative2(baseline: Double, index: Int, curWeight: Double): Pair<Double, Double> {
//        if (curWeight <= 0.0) return 0.0 to 0.0
//
//        changeWeight(index, curWeight - 0.001)
//        val lowerDiff = (doKld() - baseline)
//
//        changeWeight(index, curWeight + 0.001)
//        val upperDiff = (doKld() - baseline)
//        changeWeight(index, curWeight)
//
//        return if (lowerDiff < upperDiff) -1.0 to abs(lowerDiff) else 1.0 to abs(upperDiff)
//    }
//
//
//    fun startDescent(nTimes: Int): Pair<List<Double>, Double> {
//        var weights = (0 until topics.size).map { 0.1 }.toList()
//
//        (0 until nTimes).forEach {
//            weights.mapIndexed { index, weight -> changeWeight(index, weight) }
//            val baseline = doKld()
//            val gradient = (0 until topics.size).map { index -> getDerivative2(baseline, index, weights[index])}
//            val total = gradient.sumByDouble { (_, delta) -> delta }
//
//            if (total > 0.00000000000001) {
//                weights = weights.mapIndexed { index, weight ->
//                    val delta = gradient[index]
//                    if (weight <= 0.0) 0.0 else
//                        max(0.0, weight + delta.first * (delta.second / total) * 0.01)
//                }
//            }
//
//        }
//        val weightSum = weights.sum()
//        val finalWeights = weights.map { value -> value / weightSum }
//        return finalWeights to doKld()
//    }
//
//
//    private fun mix(weights: List<Double>): List<Double> {
//        val weightsTotal = weights.sum()
//        return topics.foldIndexed(mempty) { index, acc, list ->
//            acc.zip(list).map { (v1, v2) -> v1 + v2 * weights[index] } }
//            .map { mixValue -> mixValue / weightsTotal }
//    }
//
//}
