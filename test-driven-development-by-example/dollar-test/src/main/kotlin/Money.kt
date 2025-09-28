open class Money(
    val amount: Int,
    val currency: String,
) : Expression {
    fun currency(): String {
        return currency
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Money) return false
        return amount == other.amount && currency == other.currency
    }

    override fun toString(): String {
        return "$amount $currency"
    }

    fun times(multiplier: Int): Money {
        return Money(amount * multiplier, currency)
    }

    fun plus(addend: Money): Expression {
        return Sum(this, addend)
    }

    override fun reduce(bank: Bank, to: String): Money {
        val rate = bank.rate(currency, to)
        return Money(amount / rate, to)
    }

    companion object {
        fun dollar(amount: Int): Money = Money(amount, "USD")
        fun franc(amount: Int): Money = Money(amount, "CHF")
    }
}
