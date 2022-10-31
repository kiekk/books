class Person2(firstName: String, familyName: String) {
    var fullName = "$firstName $familyName"

    init {
        println("Created new Person instance: $fullName")
    }
}

fun main() {
    val person = Person2("John", "Doe")

    println(person.fullName)
}