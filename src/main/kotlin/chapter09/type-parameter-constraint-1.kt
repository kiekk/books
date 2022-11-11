package chapter09

// Number 를 타입 파라미터 상한(upper) 로 정함.
// T는 Number 의 하위 클래스 여야 한다.
fun <T : Number> oneHalf(value: T): Double {
    return value.toDouble() / 2.0
}

fun main() {
    println(oneHalf(3))
}