fun main() {
    val chars = 'a'..'h'
    val twoDigits = 10..99
    val zero2One = 0.0..1.0

    // 단순히 출력하면 문자열로 취급
    println(chars) // a..h
    println(twoDigits) // 10..99
    println(zero2One) // 0.0..1.0

    // in : 포함 여부
    println('b' in chars) // true
    println('b' !in chars) // false
    println(3 in twoDigits) // false
    println(3 !in twoDigits) // true

    val twoDigits2 = 10 until 99

    // .. 은 <=, <=
    // until 은 <= < (마지막 요소는 포함 X)

    println(99 in twoDigits) // true
    println(99 in twoDigits2) // false
}