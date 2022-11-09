package chapter05

fun main() {
    // generateSequence() 를 호출해 sequence 를 생성한다.
    val naturalNumbers = generateSequence(0) { it + 1 }
    val numbersTo100 = naturalNumbers.takeWhile { it <= 100 }
    println(numbersTo100.sum())
}