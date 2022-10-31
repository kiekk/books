fun main() {
    val w = 3.14
    var h = 3.14

    // 위치 기반 인자 (positional argument)
    println(rectangleArea(w, h))

    // 이름 붙은 인자 (named argument)
    println(rectangleArea(width = w, height = h))

    // 위치 기반 인자, 이름 붙은 인자 혼용
    println(mixTest("hello", width = w, height = h))
}

fun rectangleArea(width: Double, height: Double): Double {
    return width * height
}

fun mixTest(text: String, width: Double, height: Double) : Double {
    println(text)
    return width * height
}

