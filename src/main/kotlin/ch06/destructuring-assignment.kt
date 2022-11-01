fun main() {
    val person = Person4("John", "Doe", 25)

    val (familyName, age) = person
    // 파라미터 생략시 순서대로 매핑되기 때문에 의도한 결과대로 출력되지 않는다.

    println("person.firstName: ${person.firstName}")
    println("person.familyName: ${person.familyName}")
    println("person.age: ${person.age}")

//    println("firstName: ${firstName}")
    println("familyName: ${familyName}") // Doe 가 출력되어야 하지만 John 이 출력
    println("age: ${age}") // 25 가 출력되어야 하지만 Doe 가 출력
}