import annotation.Test

class TestSuite() : TestInterface {
    private val tests = mutableListOf<TestInterface>()

    constructor(testClass: Class<out TestInterface>) : this() {
        testClass.declaredMethods
            .asSequence()
            .filter { m -> m.getAnnotation(Test::class.java) != null }
            .forEach { m ->
                val ctor = testClass.getConstructor(String::class.java)
                val instance = ctor.newInstance(m.name)
                tests += instance
            }
    }

    fun add(test: TestInterface) {
        tests.add(test)
    }

    override fun run(result: TestResult) {
        tests.forEach { test ->
            test.run(result)
        }
    }
}
