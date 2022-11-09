package chapter05

fun main() {
    // 람다식 변수에 저장
    val sum = { x: Int, y: Int -> x + y }
    println(sum(1, 2))
}