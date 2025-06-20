fun main() {
    println(Application)
}

// object 키워드를 사용하면 싱글톤 패턴을 사용
object Application {
    val name = "My Application"

    override fun toString(): String = name

    fun exit() {}
}