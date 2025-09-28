open class Money(
    val amount: Int,
    val currency: Currency,
) : Expression {
    fun currency(): Currency {
        return currency
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Money) return false
        return amount == other.amount && currency == other.currency
    }

    override fun toString(): String {
        return "$amount $currency"
    }

    override fun times(multiplier: Int): Expression {
        return Money(amount * multiplier, currency)
    }

    override fun plus(addend: Expression): Expression {
        return Sum(this, addend)
    }

    override fun reduce(bank: Bank, to: Currency): Money {
        val rate = bank.rate(currency, to)
        return Money(amount / rate, to)
    }

    companion object {
        fun dollar(amount: Int): Money = Money(amount, Currency.USD)
        fun franc(amount: Int): Money = Money(amount, Currency.CHF)
    }
}
