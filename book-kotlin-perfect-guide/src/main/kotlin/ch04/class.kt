class Person {
    var firstName: String = ""
    var familyName: String = ""
    var age: Int = 0

    fun fullName() = "$firstName $familyName"

    fun showMe() {
        println("${fullName()}: $age")
    }
}

fun main() {
    val person = Person()

    person.firstName = "John"
    person.familyName = "Doe"
    person.age = 25

    person.showMe()
}