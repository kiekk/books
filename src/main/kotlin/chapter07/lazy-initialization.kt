package chapter07

class Email { /*...*/ }

fun loadEmails(person: Person4): List<Email> {
    println("Load emails for ${person.name}")
    return listOf(/*...*/)
}

class Person4(val name: String) {
    val emails by lazy { loadEmails(this) }
}

fun main() {
    val p = Person4("Alice")
    p.emails
    p.emails
}
