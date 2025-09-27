open class Money(
    protected val amount: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Money) return false
        return amount == other.amount && this::class == other::class
    }
}
