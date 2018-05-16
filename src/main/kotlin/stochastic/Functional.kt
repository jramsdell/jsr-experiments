package stochastic

import kotlin.math.pow

interface Functional<A> {

    fun evaluate(input: A): Double
    fun evaluate(input: Domain<A>): List<Double> = input.interval().map(this::evaluate)
}

class Polynomial(val coefs: List<Double>): Functional<Double> {

    override fun evaluate(input: Double): Double =
        coefs.mapIndexed { index, c -> c * input.pow((index.toDouble() + 1.0)) }
            .sum()
}




