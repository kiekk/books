open class TestCase(
    val name: String
) {
    fun run() {
        setUp()
        runCatching {
            val method = this::class.java.getMethod(name)
            method.invoke(this)
        }.onFailure { e -> throw RuntimeException(e) }

        tearDown()
    }

    open fun setUp() {
    }

    open fun tearDown() {
    }
}
