class Person2(firstName: String, familyName: String) {
    var fullName = "$firstName $familyName"
}

fun main() {
    val person = Person2("John", "Doe")

    println(person.fullName)
}