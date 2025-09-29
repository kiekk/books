open class TestCase(
    val name: String
) {
    fun run() {
        runCatching {
            val method = this::class.java.getMethod(name)
            method.invoke(this)
        }.onFailure { e -> throw RuntimeException(e) }
    }
}
