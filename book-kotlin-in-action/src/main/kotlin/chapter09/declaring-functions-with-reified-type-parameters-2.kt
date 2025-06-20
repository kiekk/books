package chapter09

fun main() {
    val items = listOf("one", 2, "three")
    // 표준 라이브러리 함수 사용
    println(items.filterIsInstance<String>())
}
