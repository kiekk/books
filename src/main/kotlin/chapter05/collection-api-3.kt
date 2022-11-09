package chapter05

val canBeInClub27 = { p: Person -> p.age <= 27 }

fun main() {
    val people = listOf(Person("Alice", 27), Person("Bob", 31))
    println(people.all(canBeInClub27))
    // all : 주어진 조건에 모두 만족할 경우 true, 하나라도 만족하지 못하면 false

    val list = listOf(1, 2, 3)
    println(!list.all { it == 3 })
    println(list.any { it != 3 })
    // any: 주어진 조건에 하나라도 만족할 경우 true, 전부 만족하지 않으면 false

    println(people.find(canBeInClub27))
    // find: 가장 먼저 주어진 조건을 만족하는 요소를 반환, 만족하는 요소가 없을 경우 null 을 반환

    println(people.count(canBeInClub27))
    // count: 주어진 조건을 만족하는 요소의 개수를 반환
}
