class Franc(
    amount: Int
) : Money(amount) {
    fun times(multiplier: Int): Money {
        return Franc(amount * multiplier)
    }

    override fun currency(): String {
        return "CHF"
    }
}
