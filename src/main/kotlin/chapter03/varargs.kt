fun main() {
    // 가변 인자, spread 연산자 *
    var varargs: Array<String> = arrayOf("1", "2", "3")
    val list = listOf("args: ", *varargs)
    println(list)
}
