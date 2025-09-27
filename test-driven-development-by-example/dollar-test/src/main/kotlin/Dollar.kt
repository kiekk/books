class Dollar(
    amount: Int,
    currency: String,
) : Money(amount, currency) {
    fun times(multiplier: Int): Money {
        return dollar(amount * multiplier)
    }
}
