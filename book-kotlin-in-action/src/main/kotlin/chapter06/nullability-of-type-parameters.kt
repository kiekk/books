package chapter06

/*
    null 이 될 수 없게 하려면 타입 상한 (upper bound) 를 지정해야 한다.
*/
fun <T> printHashCode(t: T) {
    println(t?.hashCode())
}

// t 는 null 이 될 수 없다.
fun <T: Any> printHashCode2(t: T) {
    // t 는 null 이 될 수 없기 때문에 ?. 대신 . 만 사용해도 안전
    println(t.hashCode())
}

fun main() {
    printHashCode(null) // 이 때 t 의 타입은 Any? 로 추론된다.
//    printHashCode2(null) // Error, printHashCode2 에서 t 는 null 이 될 수 없다.
}
