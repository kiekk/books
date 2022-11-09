package chapter05

// StringBuilder 를 사용해 String 을 생성하는 기능을 지원하는 buildString API 를 사용하여 리팩토링
fun alphabet2() = buildString {
    for (letter in 'A'..'Z') {
        append(letter)
    }
    append("\nNow I know the alphabet!")
}

fun main() {
    println(alphabet2())
}
