class Person2(fullName: String) {
    val firstName: String
    val familyName: String

    init {
        // init 블록 안에서 프로퍼티 초기화 가능
        var names = fullName.split(" ")
        if (names.size != 2) {
            throw IllegalArgumentException("Invalid name: $fullName")
        }
        firstName = names[0]
        familyName = names[1]
    }
}

fun main() {
    val person = Person2("John Doe")

    println(person.firstName)
    println(person.familyName)
}