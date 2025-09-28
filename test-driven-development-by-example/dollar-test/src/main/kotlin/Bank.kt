import java.util.*

class Bank(
    val rates: Hashtable<Pair, Int> = Hashtable()
) {

    fun reduce(source: Expression, to: Currency): Money {
        return source.reduce(this, to)
    }

    fun rate(from: Currency, to: Currency): Int {
        if (from == to) return 1
        return rates[Pair(from, to)]!!
    }

    fun addRate(from: Currency, to: Currency, rate: Int) {
        rates[Pair(from, to)] = rate
    }
}
