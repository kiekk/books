fun main() {
    val person = Person4("John", "Doe", 25)

    val (_, familyName, age) = person
    // 사용하지 않는 파라미터의 위치에 _ 를 작성

    println("person.firstName: ${person.firstName}")
    println("person.familyName: ${person.familyName}")
    println("person.age: ${person.age}")

//    println("firstName: ${firstName}")
    println("familyName: ${familyName}") // Doe
    println("age: ${age}") // 25
}