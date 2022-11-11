package chapter06

fun ignoreNulls(s: String?) {
    // !! 은 해당 값이 null 아님을 선언하여 null 이 될 수 없는 타입으로 바꿀 수 있다.
    // 이 경우 실제 null 이 전달되면 NPE 가 발생한다.
    val sNotNull: String = s!!
    println(sNotNull.length)
}

fun main() {
    ignoreNulls(null) // Error, NPE (NullPointerException)
}