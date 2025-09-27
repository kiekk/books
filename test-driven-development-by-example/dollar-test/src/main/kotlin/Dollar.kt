class Dollar(
    amount: Int
) : Money(amount) {
    fun times(multiplier: Int): Dollar {
        return Dollar(amount * multiplier)
    }
}
