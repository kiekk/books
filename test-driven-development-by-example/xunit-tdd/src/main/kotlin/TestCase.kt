open class TestCase(
    val name: String
) {
    fun run() {
        setUp()
        runCatching {
            val method = this::class.java.getMethod(name)
            method.invoke(this)
        }.onFailure { e -> throw RuntimeException(e) }
    }

    open fun setUp() {
    }
}
