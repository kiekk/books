package chapter05

fun salute() = println("Salute!")

fun main() {
    // 최상위 함수를 참조
    run(::salute)
}
