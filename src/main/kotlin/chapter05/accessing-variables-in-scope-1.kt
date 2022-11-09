package chapter05

fun printMessagesWithPrefix(messages: Collection<String>, prefix: String) {
    messages.forEach {
        // 람다 안에서 함수의 파라미터를 사용
        println("$prefix $it")
    }
}

fun main() {
    val errors = listOf("403 Forbidden", "404 Not Found")
    printMessagesWithPrefix(errors, "Error:")
}
