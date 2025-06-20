package chapter05

fun main() {
    val list = listOf(1, 2, 3, 4)
    println(list.filter { it % 2 == 0 })
    // 이름 있는 인자로도 전달 가능
    println(list.filter(predicate = { it % 2 == 0 }))

    val people = listOf(Person("Alice", 29), Person("Bob", 31))
    println(people.filter { it.age > 30 })
}
