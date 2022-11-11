package chapter06

data class Person3(
    val name: String,
    val age: Int? = null
) {

    fun isOlderThan(other: Person3): Boolean? {
        if (age == null || other.age == null)
            return null
        return age > other.age
    }
}

fun main() {
    println(Person3("Sam", 35).isOlderThan(Person3("Amy", 42)))
    println(Person3("Sam", 35).isOlderThan(Person3("Jane")))
}
