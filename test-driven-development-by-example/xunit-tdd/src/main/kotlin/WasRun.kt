class WasRun(
    name: String,
    var wasRun: Boolean = false,
    var wasSetUp: Boolean = false,
) : TestCase(name) {

    fun testMethod() {
        wasRun = true
    }

    override fun setUp() {
        wasSetUp = true
    }
}
