class Dollar(
    var amount: Int
) {
    fun times(multiplier: Int): Dollar {
        return Dollar(amount * multiplier)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Dollar) return false
        return amount == other.amount
    }
}
