package chapter09

val <T> List<T>.penultimate: T
    get() = this[size - 2]

fun main() {
    println(listOf(1, 2, 3, 4).penultimate)
}
