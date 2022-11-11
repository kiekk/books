package chapter08

fun lookForAlice(people: List<Person2>) {
    people.forEach label@{
        if (it.name == "Alice") return@label // local-return, 해당 label 로부터 반환
    }
    println("Alice might be somewhere")
}

fun main() {
    lookForAlice(listOf(Person2("Alice", 29), Person2("Bob", 31)))
}
