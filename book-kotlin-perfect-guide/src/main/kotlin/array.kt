fun main() {
    val a = emptyArray<String>() // Array<String> 원소 0개
    val b = arrayOf("hello", "world") // Array<String> 원소 2개
    val c = arrayOf(1, 4, 9) // Array<Int> 원소 3개

    val squares = Array(4) { (it + 1) * (it + 1) } // 람다

    println(squares[0])
    println(squares[1])
    println(squares[2])
    println(squares[3])

    squares[3] = 25 // 배열은 요소 값 변경 가능
    println(squares[3])


    val b2 = intArrayOf(1, 2, 3) + 4 // 원소 하나만 추가
    val b3 = intArrayOf(1, 2, 3) + intArrayOf(4, 5, 6) // 배열 추가

    // 배열 비교
    println(intArrayOf(1, 2, 3) == intArrayOf(1, 2, 3)) // false
    println(intArrayOf(1, 2, 3).contentEquals(intArrayOf(1, 2, 3))) // true

}