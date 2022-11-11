package chapter06

fun main() {
    val x = 1
    println(x.toLong() in listOf(1L, 2L, 3L))
    // 코틀린은 타입 직접 변환 메소드를 제공한다.
    // toLong, toInt, toChar, toByte...
//    println(x in listOf(1L, 2L, 3L)) // Error
    // Type inference failed. The value of the type parameter T should be mentioned in input types (argument types, receiver type or expected type).
}
