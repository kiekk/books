fun main() {
    val a = IntArray(10) { it * it }
    var sum = 0

    for (x in a) {
        sum += x
    }

    println("Sum: $sum")
}