package chapter05

// stringBuilder 를 변수로 생성하지 않고 바로 with 함수에 작성하여 불필요한 변수 생성 X
fun alphabet() = with(StringBuilder()) {
    for (letter in 'A'..'Z') {
        append(letter)
    }
    append("\nNow I know the alphabet!")
    toString()
}

fun main() {
    println(alphabet())
}