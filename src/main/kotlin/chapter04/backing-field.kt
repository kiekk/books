class User12(val name: String) {
    var address: String = "unspecified"
        set(value) {
            println(
                """
                Address was changed for $name:
                "$field" -> "$value".""".trimIndent()
            )
            field = value
        }
}

fun main() {
    val user = User12("Alice")
    user.address = "Elsenheimerstrasse 47, 80687 Muenchen"
}