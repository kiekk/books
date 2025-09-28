open class Money(
    protected val amount: Int,
    protected val currency: String,
) {
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

    fun plus(addend: Money): Money {
        return Money(amount + addend.amount, currency)
    }

    companion object {
        fun dollar(amount: Int): Money = Money(amount, "USD")
        fun franc(amount: Int): Money = Money(amount, "CHF")
    }
}
