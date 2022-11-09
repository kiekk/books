package chapter05

fun main() {
    // 생성자 참조
    val createPerson = ::Person
    val p = createPerson("Alice", 29)
    println(p)

    // 확장 함수도 멤버 참조 방식 사용 가능
    var predicate = Person::isAdult
}

fun Person.isAdult() = age >= 21