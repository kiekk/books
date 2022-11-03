fun main() {
    // 타입 파라미터를 지정
    val map = HashMap<Int, String>()
    val list = arrayListOf<String>()

    // 타입을 명시하면 컴파일러가 타입을 추론할 수 있기 때문에
    // 타입 생략 가능
    val map2: Map<Int, String> = HashMap()

    // 전달된 인자의 타입으로 타입을 추론할 수 있기 때문에
    // 타입 생략 가능
    val list2 = arrayListOf("abc", "def")
}