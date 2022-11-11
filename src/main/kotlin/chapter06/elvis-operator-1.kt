package chapter06

// 엘비스 연산자(?:) : null 대신 사용할 디폴트 값을 지정한다.
fun strLenSafe2(s: String?): Int = s?.length ?: 0

fun main() {
    println(strLenSafe2("abc"))
    println(strLenSafe2(null)) // null 이 아니라 0이 출력된다.
}
