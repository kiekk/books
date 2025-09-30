class WasRun(
    name: String,
) : TestCase(name) {
    var wasRun: Boolean = false
    var wasSetUp: Boolean = false
    var log: String = ""

    fun testMethod() {
        wasRun = true
        log += " testMethod"
    }

    override fun setUp() {
        wasSetUp = true
        log = "setUp"
    }
}
