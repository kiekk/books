fun main() {
    var hello = "hello"
    println("$hello world!!")
    // println("$helloworld!!") $변수명 형식은 띄어쓰기 중요
    println("${hello}world")
    println("\$hello world!") // escape 문자는 \와 함께 사용

    val message = """
        $hello, World!
    """.trimIndent()

    println(message)
}