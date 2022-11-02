import kotlin.math.PI

abstract class Shape {
    abstract val width: Double
    abstract val height: Double
    abstract fun area(): Double
}

class Circle(val radius: Double) : Shape() {
    val diameter get() = 2 * radius
    override val width get() = diameter
    override val height get() = diameter

    override fun area() = PI * radius * radius
}

class Rectangle(
    override val width: Double,
    override val height: Double
) : Shape() {
    override fun area() = width * height
}

fun Shape.print() {
    println("Bounds: $width*$height, area: ${area()}")
}

fun main() {
    // Bounds: 20.0*20.0, area: 314.1592653589793
    Circle(10.0).print()

    // Bounds: 3.0*5.0, area: 15.0
    Rectangle(3.0, 5.0).print()
}