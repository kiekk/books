package chapter08

data class Person2(val name: String, val age: Int)

val people = listOf(Person2("Alice", 29), Person2("Bob", 31))

fun main() {
    println(people.filter { it.age < 30 })
}
