class TestSuite : Test {
    private val tests = mutableListOf<Test>()

    fun add(test: Test) {
        tests.add(test)
    }

    override fun run(result: TestResult) {
        tests.forEach { test ->
            test.run(result)
        }
    }
}
