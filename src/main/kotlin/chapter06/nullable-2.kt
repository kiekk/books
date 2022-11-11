package chapter06

// ?. 로 safe call
// if else 로 직접 null 확인을 하지 않아도 된다.
fun printAllCaps(s: String?) {
    val allCaps: String? = s?.uppercase()
    println(allCaps)
}

fun main() {
    printAllCaps("abc")
    printAllCaps(null)
}
