typealias IntPredicate = (Int) -> Boolean
typealias IntMap = HashMap<Int, Int>

fun readFirst(filter: IntPredicate) = generateSequence { readLine()?.toIntOrNull() }.firstOrNull(filter)

// type alias 를 사용하여 내포된 클래스를 더 축약하
sealed class Status {
    object Success : Status()
    class Error(val message: String) : Status()
}

typealias StSuccess = Status.Success
typealias StError = Status.Error

fun main() {
    val map = IntMap().also {
        it[1] = 2
        it[2] = 3
    }
}