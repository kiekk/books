fun main() {
    val person = Person4("John", "Doe", 25)

    val (firstName, familyName, age) = person

    println("person.firstName: ${person.firstName}")
    println("person.familyName: ${person.familyName}")
    println("person.age: ${person.age}")

    println("firstName: ${firstName}")
    println("familyName: ${familyName}")
    println("age: ${age}")
}