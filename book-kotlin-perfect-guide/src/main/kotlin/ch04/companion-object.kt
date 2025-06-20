fun main() {
    val app = Application2.Factory.create(arrayOf("Hello")) ?: return
    println("Application2 started: ${app.name}")

    // companion 키워드를 사용하면 동반 객체가 들어있는 외부 객체의 이름을 사용할 수 있습니다.
    val app2 = Application3.create(arrayOf("Hello")) ?: return
    println("Application3 started: ${app2.name}")
}

class Application2 private constructor(val name: String) {
    object Factory {
        fun create(args: Array<String>): Application2? {
            val name = args.firstOrNull() ?: return null
            return Application2(name)
        }
    }
}

class Application3 private constructor(val name: String) {
    companion object Factory {
        fun create(args: Array<String>): Application3? {
            val name = args.firstOrNull() ?: return null
            return Application3(name)
        }
    }
}

class Application4 private constructor(val name: String) {
    // 정의에서 이름 자체를 제거할 수 있습니다.
    companion object {
        fun create(args: Array<String>): Application4? {
            val name = args.firstOrNull() ?: return null
            return Application4(name)
        }
    }
}