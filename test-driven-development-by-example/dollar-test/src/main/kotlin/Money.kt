open class Money(
    protected val amount: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Money) return false
        return amount == other.amount && this::class == other::class
    }

    companion object {
        fun dollar(amount: Int): Dollar = Dollar(amount)
        fun franc(amount: Int): Franc = Franc(amount)
    }
}
