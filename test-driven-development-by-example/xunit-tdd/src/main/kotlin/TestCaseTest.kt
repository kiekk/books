class TestCaseTest(
    name: String
) : TestCase(name) {

    fun testTemplateMethod() {
        val wasRun = WasRun("testMethod")
        wasRun.run()
        Assert.assertEquals("setUp testMethod tearDown", wasRun.log)
    }

    fun testResult() {
        val wasRun = WasRun("testMethod")
        val result = wasRun.run()
        Assert.assertEquals("1 run, 0 failed", result.summary())
    }

    fun testFailedResultFormatting() {
        val result = TestResult()
        result.testStarted()
        result.testFailed()
        Assert.assertEquals("1 run, 1 failed", result.summary())
    }

    fun testFailedResult() {
        val wasRun = WasRun("testBrokenMethod")
        val result = wasRun.run()
        Assert.assertEquals("1 run, 1 failed", result.summary())
    }
}
