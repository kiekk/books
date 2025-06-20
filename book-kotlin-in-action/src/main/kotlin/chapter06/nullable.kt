package chapter06

// if else 로 null 확인
fun strLenSafe(s: String?): Int =
    if (s != null) s.length else 0

fun main() {
    val x: String? = null
    println(strLenSafe(x))
    println(strLenSafe("abc"))
}
