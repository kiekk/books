package chapter07

class Point3(val x: Int, val y: Int) {
    override fun equals(obj: Any?): Boolean {
        if (obj === this) return true
        if (obj !is Point3) return false
        return obj.x == x && obj.y == y
    }
}

fun main() {
    println(Point3(10, 20) == Point3(10, 20))
    println(Point3(10, 20) != Point3(5, 5))
    println(null == Point3(1, 2))
}