package chapter06

/*
Nothing 타입은 아무 값도 포함하지 않는다.
따라서 함수의 반환 타입, 반환 타입으로 쓰일 타입 파라미터로만 사용할 수 있다.
Nothing 은 이 함수가 결코 정상적으로 끝나지 않음을 알려준다.
 */
fun fail(message: String): Nothing {
    throw IllegalStateException(message)
}

fun main() {
    fail("Error occurred")
}
