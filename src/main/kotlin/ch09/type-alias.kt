typealias IntPredicate = (Int) -> Boolean
typealias IntMap = HashMap<Int, Int>

fun readFirst(filter: IntPredicate) = generateSequence { readLine()?.toIntOrNull() }.firstOrNull(filter)

fun main() {
    val map = IntMap().also {
        it[1] = 2
        it[2] = 3
    }
}