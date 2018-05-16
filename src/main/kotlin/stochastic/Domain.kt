package stochastic

interface Domain<A> {

    fun interval(): List<A>
}

class RealInterval(val start: Double, val stop: Double, val step: Int): Domain<Double> {
    private val diff = stop - start
    private val interior = (0 until step).map { start + (diff / step) * it  }
    override fun interval(): List<Double> = interior
}