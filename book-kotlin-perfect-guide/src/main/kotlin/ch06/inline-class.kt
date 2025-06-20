@JvmInline
value class Dollar(val amount: Int) {
    fun add(d: Dollar) = Dollar(amount + d.amount)
    val isDebt get() = amount < 0
}

fun main() {
    println(Dollar(15).add(Dollar(20)).amount) // 35
    println(Dollar(-100).isDebt) // true
}