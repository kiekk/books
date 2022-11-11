package chapter08

fun lookForAlice(people: List<Person2>) {
    for (person in people) {
        if (person.name == "Alice") {
            println("Found!")
            return // 함수로부터 반환
        }
    }
    println("Alice is not found")
}

fun main() {
    lookForAlice(listOf(Person2("Alice", 29), Person2("Bob", 31)))
}
