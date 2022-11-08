import Color.*;

fun getWarmth(color: Color) = when (color) {
    // 한 분기 안에 여러 enum 사용
    RED, ORANGE, YELLOW -> "warm"
    GREEN -> "neutral"
    BLUE, INDIGO, VIOLET -> "cold"
}

fun main() {
    println(getWarmth(ORANGE))
}
