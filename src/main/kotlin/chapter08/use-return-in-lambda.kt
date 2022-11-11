package chapter08

fun lookForAlice(people: List<Person2>) {
    people.forEach {
        if (it.name == "Alice") return@forEach // 함수 이름을 return label 로 사용
    }
    println("Alice might be somewhere")
}

fun main() {
    lookForAlice(listOf(Person2("Alice", 29), Person2("Bob", 31)))
}
