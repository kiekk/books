package chapter05

fun main() {
    // sequence = java 의 stream 과 비슷
    // 최종 연산 호출 전까지는 중간 연산은 실행하지 않는다. 지연 실행
    listOf(1, 2, 3, 4).asSequence()
        .map { print("map($it) "); it * it }
        .filter { print("filter($it) "); it % 2 == 0 }

    listOf(1, 2, 3, 4).asSequence()
        .map { print("map($it) "); it * it }
        .filter { print("filter($it) "); it % 2 == 0 }
        .toList() // 최종 연산인 toList() 를 호출할 때 비로소 중간 연산이 실행된다.
}