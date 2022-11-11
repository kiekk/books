package chapter09

fun main() {
    val letters = ('a'..'z').toList()
    println(letters.slice<Char>(0..2))
    println(letters.slice(10..13)) // 타입 추론이 가능할 경우 타입 생략 가능
}