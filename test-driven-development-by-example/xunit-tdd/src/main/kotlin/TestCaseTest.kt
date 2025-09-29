class TestCaseTest(
    name: String
) : TestCase(name) {

    fun testRunning() {
        val wasRun = WasRun("testMethod")
        Assert.assertEquals(false, wasRun.wasRun)
        wasRun.run()
        Assert.assertEquals(true, wasRun.wasRun)
    }
}
