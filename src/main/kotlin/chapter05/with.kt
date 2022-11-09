package chapter05

// with 함수를 사용하여 코드 리팩토링
fun alphabet(): String {
    val stringBuilder = StringBuilder()
    // with 함수 내부에서 사용하려는 수신 객체를 지정한다.
    return with(stringBuilder) {
        for (letter in 'A'..'Z') {
            // 이 때의 this 는  stringBuilder 를 가리킨다.
            this.append(letter)
        }
        // this 는 생략이 가능하다.
        append("\nNow I know the alphabet!")
        this.toString()
    }
}

fun main() {
    println(alphabet())
}