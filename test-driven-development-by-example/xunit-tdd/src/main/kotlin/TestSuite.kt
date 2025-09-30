class TestSuite {
    private val tests = mutableListOf<TestCase>()

    fun add(test: TestCase) {
        tests.add(test)
    }

    fun run(result: TestResult) {
        tests.forEach { test ->
            test.run(result)
        }
    }
}
