class WasRun(
    name: String,
) : TestCase(name) {
    var wasSetUp: Boolean = false
    var log: String = ""

    fun testMethod() {
        log += " testMethod"
    }

    override fun setUp() {
        wasSetUp = true
        log = "setUp"
    }
}
