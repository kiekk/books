open class TestCase(
    val name: String
) {
    fun run(): TestResult {
        val result = TestResult()
        result.testStarted()

        setUp()
        runCatching {
            val method = this::class.java.getMethod(name)
            method.invoke(this)
        }.onFailure { e ->
            result.testFailed()
        }

        tearDown()
        return result
    }

    open fun setUp() {
    }

    open fun tearDown() {
    }
}
