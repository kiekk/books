fun main() {
    println(mul(2, 5))
    println(mul(2))

    printSorted(5, 1, 2, 10)

    val array = intArrayOf(5, 2, 1, 10)
    printSorted(*array) // 스프레드 연산자, 배열을 복사하기 때문에 원본 배열에는 영향을 미치지 않습니다.
    // printSorted(array) // Kotlin: Type mismatch: inferred type is IntArray but Int was expected
    println(array.contentToString())
}

// default parameter
fun mul(a: Int, b: Int = 1) = a * b

// vararg
fun printSorted(vararg items: Int) {
    items.sort()
    println(items.contentToString())
}