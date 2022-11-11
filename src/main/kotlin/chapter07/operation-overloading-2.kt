package chapter07

data class Point2(val x: Int, val y: Int)

operator fun Point2.plus(other: Point2): Point2 {
    return Point2(x + other.x, y + other.y)
}

fun main() {
    val p1 = Point2(10, 20)
    val p2 = Point2(30, 40)
    println(p1 + p2)
}
