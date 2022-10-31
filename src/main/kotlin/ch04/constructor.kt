class Person2(firstName: String, familyName: String) {
    var fullName = "$firstName $familyName"

    init {
        // init 블록 안에서는 return 키워드 사용 불가능
//        if(firstName.isEmpty()&& familyName.isEmpty()) return
        println("Created new Person instance: $fullName")
    }
}

fun main() {
    val person = Person2("John", "Doe")

    println(person.fullName)
}