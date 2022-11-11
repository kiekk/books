package chapter07

data class Point(val x: Int, val y: Int) {
    // 연산자를 오버로딩하는 함수 앞에 operator 키워드를 붙여야 한다.
    operator fun plus(other: Point): Point {
        println("Plus Operation Overloading Function")
        return Point(x + other.x, y + other.y)
    }
}

fun main() {
    val p1 = Point(10, 20)
    val p2 = Point(30, 40)
    println(p1 + p2)
}
