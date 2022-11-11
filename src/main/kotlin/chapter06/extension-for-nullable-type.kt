package chapter06

fun verifyUserInput(input: String?) {
    // null 이 될 수 있는 타입에 대해서도 확장 함수를 호출 할 수 있다.
    if (input.isNullOrBlank()) {
        println("Please fill in the required fields")
    }
}

fun main() {
    verifyUserInput(" ")
    verifyUserInput(null)
}
