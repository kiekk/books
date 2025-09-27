class Franc(
    amount: Int,
    currency: String,
) : Money(amount, currency) {
    fun times(multiplier: Int): Money {
        return franc(amount * multiplier)
    }
}
