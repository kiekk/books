class TestCaseTest(
    name: String
) : TestCase(name) {

    fun testTemplateMethod() {
        val wasRun = WasRun("testMethod")
        wasRun.run()
        Assert.assertEquals("setUp testMethod tearDown", wasRun.log)
    }
}
