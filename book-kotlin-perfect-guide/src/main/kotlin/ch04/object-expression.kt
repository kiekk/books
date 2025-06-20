fun main() {
    fun midPoint(xRange: IntRange, yRange: IntRange) = object {
        val x = (xRange.first + xRange.last) / 2
        val y = (yRange.first + yRange.last) / 2
    }

    val midPoint = midPoint(1..5, 2..6)

    println("${midPoint.x}, ${midPoint.y}")
}