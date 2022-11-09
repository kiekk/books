package chapter05

fun main() {
    val people = listOf(Person("Alice", 29), Person("Bob", 31))
    val names = people.joinToString(separator = " ",
        // 인자로 람다식 전달
        transform = { p: Person -> p.name })
    println(names)
}