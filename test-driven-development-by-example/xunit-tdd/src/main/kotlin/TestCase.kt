open class TestCase(
    val name: String
) {
    fun run(result: TestResult) {
        result.testStarted()

        setUp()
        runCatching {
            val method = this::class.java.getMethod(name)
            method.invoke(this)
        }.onFailure { e ->
            result.testFailed()
        }

        tearDown()
    }

    open fun setUp() {
    }

    open fun tearDown() {
    }
}
