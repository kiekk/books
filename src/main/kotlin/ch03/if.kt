fun main() {
    println(max(5, 2))
    println(max2(5, 2))
}

fun max(a: Int, b: Int): Int {
    return if (a > b) a else b
}

fun max2(a: Int, b: Int): Int = if (a > b) a else b

