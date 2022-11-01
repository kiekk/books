import Direction2.*

enum class Direction2 {
    NORTH, SOUTH, EAST, WEST
}

fun main() {
    println(WEST === NORTH) // false
    println(WEST !== EAST) // true
    println(EAST < NORTH) // false
    println(SOUTH >= NORTH) // true

    // enum 비교는 ordinal 프로퍼티가 반환하는 인덱스로 비교된다.

    val direction = enumValues<Direction2>()

    println(direction[2]) // EAST
    println(enumValueOf<Direction2>("SOUTH")) // SOUTH
}