package chapter09

// inline, reified 키워드를 사용하면 value 의 타입을 실행 시점에 검사할 수 있다.
inline fun <reified T> isA(value: Any) = value is T

fun main() {
    println(isA<String>("abc"))
    println(isA<String>(123))
}
