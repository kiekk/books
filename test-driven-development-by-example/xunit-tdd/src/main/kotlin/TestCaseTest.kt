class TestCaseTest(
    name: String
) : TestCase(name) {

    fun testTemplateMethod() {
        val wasRun = WasRun("testMethod")
        val result = TestResult()
        wasRun.run(result)
        Assert.assertEquals("setUp testMethod tearDown", wasRun.log)
    }

    fun testResult() {
        val wasRun = WasRun("testMethod")
        val result = TestResult()
        wasRun.run(result)
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
        val result = TestResult()
        wasRun.run(result)
        Assert.assertEquals("1 run, 1 failed", result.summary())
    }

    fun testSuite() {
        val suite = TestSuite()
        suite.add(WasRun("testMethod"))
        suite.add(WasRun("testBrokenMethod"))
        val result = TestResult()
        suite.run(result)
        Assert.assertEquals("2 run, 1 failed", result.summary())
    }
}
