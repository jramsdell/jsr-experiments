package stochastic

import org.apache.commons.math3.distribution.NormalDistribution
import utils.normalize
import utils.sharedRand

class Perturber<A>(val target: Functional<A>, val domain: Domain<A>) {
    val features = ArrayList<Functional<A>>()


    fun perturb(nSamples: Int = 50): List<List<Double>> {
        val targetVals = target.evaluate(domain).normalize()
        return perturbSequence(targetVals, nSamples)
    }

    companion object {
        fun perturbSequence(seq: List<Double>, nSamples: Int, normalize: Boolean = true): List<List<Double>> {
            val norm = NormalDistribution(sharedRand, 1.0, 0.001)
            val results = (0 until nSamples).map {
                norm.sample(seq.size).zip(seq)
                    .map { (x, y) -> x + y }
                    .let { if (normalize) it.normalize() else it }
            }

            return results
        }

    }
}

fun main(args: Array<String>) {
    val myseq = listOf(0.2, 0.2, 0.5, 0.5, 0.2, 0.2, 0.5, 0.1)
    val mypol = Polynomial(listOf(1.0, 1.0))
    val mydom = RealInterval(1.0, 3.0, 2)
    val p = Perturber(mypol, mydom)
    println(p.perturb(50))
//    println(Perturber.perturbSequence(myseq, 50, true))
}