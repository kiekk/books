fun main() {
    val map = mapOf(1.to("one"), 2.to("two"), 3.to("three"))

    println("일반 호출")
    println(map)

    val map2 = mapOf(1 to "one", 2 to "two", 3 to "three")
    println("중위 호출")
    println(map2)

    // 중위 호출은 .(dot) 이나 () (소괄호) 생략

    val (number, name) = 1 to "one"
    println("구조 분해 할당")
    println("$number : $name")


    println("구조 분해 할당 - iteration")
    for((number, name) in map) {
        println("$number : $name")
    }
}