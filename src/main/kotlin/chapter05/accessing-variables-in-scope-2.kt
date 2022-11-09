package chapter05

fun printProblemCounts(responses: Collection<String>) {
    var clientErrors = 0
    var serverErrors = 0
    responses.forEach {
        // 코틀린은 자바와 달리 람다 밖의 변수를 변경할 수 있다.
        if (it.startsWith("4")) {
            clientErrors++
        } else if (it.startsWith("5")) {
            serverErrors++
        }
    }
    println("$clientErrors client errors, $serverErrors server errors")
}

fun main() {
    val responses = listOf(
        "200 OK", "418 I'm a teapot",
        "500 Internal Server Error"
    )
    printProblemCounts(responses)
}
