fun main() {
    val map = mapOf(1.to("one"), 2.to("two"), 3.to("three"))

    println("일반 호출")
    println(map)

    val map2 = mapOf(1 to "one", 2 to "two", 3 to "three")
    println("중위 호출")
    println(map2)

    // 중위 호출은 .(dot) 이나 () (소괄호) 생략
}