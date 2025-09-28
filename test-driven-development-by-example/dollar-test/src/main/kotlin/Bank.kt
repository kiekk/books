import java.util.*

class Bank(
    val rates: Hashtable<Pair, Int> = Hashtable()
) {

    fun reduce(source: Expression, to: String): Money {
        return source.reduce(this, to)
    }

    fun rate(from: String, to: String): Int {
        if (from == to) return 1
        return rates.get(Pair(from, to))!!
    }

    fun addRate(from: String, to: String, rate: Int) {
        rates[Pair(from, to)] = rate
    }
}
