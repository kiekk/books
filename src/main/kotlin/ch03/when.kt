fun main() {
    println(hexDigitIf(4))
    println(hexDigitIf(11))
    println(hexDigitIf(16))

    println(hexDigitWhen(4))
    println(hexDigitWhen(11))
    println(hexDigitWhen(16))

    println(hexDigitWhen2(4))
    println(hexDigitWhen2(11))
    println(hexDigitWhen2(16))
}

fun hexDigitIf(n: Int): Char {
    return if (n in 0..9) '0' + n
    else if (n in 10..15) 'A' + n - 10
    else '?'
}

fun hexDigitWhen(n: Int): Char {
    when {
        n in 0..9 -> return '0' + n
        n in 10..15 -> return 'A' + n - 10
        else -> return '?'
    }
}

// 이 경우 else 를 생략하면 에러 발생
fun hexDigitWhen2(n: Int): Char = when (n) {
    in 0..9 -> '0' + n
    in 10..15 -> 'A' + n - 10
    else -> '?'
}