fun recognize(c: Char) = when (c) {
    // when 조건에 in 사용
    in '0'..'9' -> "It's a digit!"
    in 'a'..'z', in 'A'..'Z' -> "It's a letter!"
    else -> "I don't know…"
}

fun main() {
    println(recognize('8'))
}