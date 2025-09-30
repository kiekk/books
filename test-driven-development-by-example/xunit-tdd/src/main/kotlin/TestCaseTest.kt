class TestCaseTest(
    name: String
) : TestCase(name) {

    lateinit var wasRun: WasRun

    fun testTemplateMethod() {
        Assert.assertEquals(false, wasRun.wasRun)
        wasRun.run()
        Assert.assertEquals("setUp testMethod", wasRun.log)
        Assert.assertEquals(true, wasRun.wasRun)
    }

    override fun setUp() {
        wasRun = WasRun("testMethod")
    }
}
