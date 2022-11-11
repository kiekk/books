package chapter06

fun main() {
    val array = arrayOf(1, 2, 3, 4, 5, 6)
    // 배열의 인덱스 값의 범위에 대해 이터레이션하기 위해 indices 확장 함수를 사용한다.
    for (i in array.indices) {
        println("Argument $i is : ${array[i]}")
    }

    // 배열 초기화, 제네릭은 타입 추론이 가능할 때 생략 가능
    val letters = Array(26) { i -> ('a' + i).toString() }
    println(letters.joinToString(""))

    // 컬렉션 스프레드 연산자 사용
    val strings = listOf("a", "b", "c")
    println("%s/%s/%s".format(*strings.toTypedArray()))

    // 특정 타입에 대한 배열 생성 기능 지원
    // LongArray, IntArray, DoubleArray ...
    val squares = IntArray(5) { i -> (i + 1) * (i + 1) }
    println(squares.joinToString())

    // forEachIndexed 를 통해 index, element 순회
    array.forEachIndexed { index, element ->
        println("Argument $index is: $element")
    }
}