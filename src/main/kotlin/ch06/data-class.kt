data class Person4(val firstName: String, val familyName: String, val age: Int)

fun main() {
    val person1 = Person4("John", "Doe", 25)
    val person2 = Person4("John", "Doe", 25)
    val person3 = person1

    println(person1 == person2) // true
    println(person2 == person3) // true
    println(person1 == person3) // true
}