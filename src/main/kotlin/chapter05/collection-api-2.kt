package chapter05

fun main() {
    val list = listOf(1, 2, 3, 4)
    println(list.map { it * it })
    println(list.map(transform = { it * it }))

    val people = listOf(Person("Alice", 29), Person("Bob", 31))
    println(people.map { it.name })

    // 멤버 참조도 가능
    println(people.map(Person::name))

    val numbers = mapOf(0 to "zero", 1 to "one")
    println(numbers.mapValues { it.value.uppercase() })
}
/*
    filterKeys: key 를 걸러냄
    filterValues: value 를 걸러냄
    mapKeys: key 를 변환
    mapValues: value 를 변환
 */