package chapter05

fun main() {
    val people = listOf(Person("Alice", 29), Person("Bob", 31))
    val names = people.joinToString(separator = " ",
        // 인자로 람다식 전달
        transform = { p: Person -> p.name })
    println(names)

    // 람다식 괄호 밖으로 빼기
    val names2 = people.joinToString(" ") { p: Person -> p.name }
    println(names2)

    // 타입 생략 가능
    val names3 = people.joinToString(" ") { p -> p.name }
    println(names3)

    // 디폴트 파라미터 이름 it 사용
    val names4 = people.joinToString(" ") { it.name }
    println(names4)
}