class TestCaseTest(
    name: String
) : TestCase(name) {

    lateinit var wasRun: WasRun

    fun testRunning() {
        Assert.assertEquals(false, wasRun.wasRun)
        wasRun.run()
        Assert.assertEquals(true, wasRun.wasRun)
    }

    fun testSetUp() {
        Assert.assertEquals(false, wasRun.wasSetUp)
        wasRun.run()
        Assert.assertEquals(true, wasRun.wasSetUp)
    }

    override fun setUp() {
        wasRun = WasRun("testMethod")
    }
}
