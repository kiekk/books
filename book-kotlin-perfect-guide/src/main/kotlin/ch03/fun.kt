fun main() {
    prompt("World!")
    prompt2("World!")
}

fun prompt(name: String) {
    println("Hello, $name!")
}

// 동일한 함수 Unit 은 java 의 void 와 같으며 생략 가능
//fun prompt(name: String): Unit {
//    println("Hello, $name!")
//}

fun prompt2(name: String) = println("Hello, $name!!!")