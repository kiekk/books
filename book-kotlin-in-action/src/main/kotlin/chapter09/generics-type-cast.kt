package chapter09

fun printSum(c: Collection<*>) {
    // Unchecked cast: Collection<*> to List<Int>
    // 에러는 나지 않지만, 경고 발생
    val intList = c as? List<Int> ?: throw IllegalArgumentException("List is expected")
    println(intList.sum())
}

fun main() {
    printSum(listOf(1, 2, 3))
}
