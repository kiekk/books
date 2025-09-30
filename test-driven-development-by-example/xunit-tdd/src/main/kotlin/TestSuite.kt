class TestSuite() : Test {
    private val tests = mutableListOf<Test>()

    constructor(testClass: Class<out Test>) : this() {
        testClass.declaredMethods
            .asSequence()
            .filter { it.name.startsWith("test") }
            .forEach { m ->
                val ctor = testClass.getConstructor(String::class.java)
                val instance = ctor.newInstance(m.name)
                tests += instance
            }
    }

    fun add(test: Test) {
        tests.add(test)
    }

    override fun run(result: TestResult) {
        tests.forEach { test ->
            test.run(result)
        }
    }
}
