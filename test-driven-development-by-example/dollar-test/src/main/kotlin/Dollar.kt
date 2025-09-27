class Dollar(
    amount: Int
) : Money(amount) {
    fun times(multiplier: Int): Money {
        return Dollar(amount * multiplier)
    }
}
