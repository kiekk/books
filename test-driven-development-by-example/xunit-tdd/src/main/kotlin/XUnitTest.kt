class XUnitTest {
}

fun main() {
    val suite = TestCaseTest.suite()
    val result = TestResult()
    suite.run(result)
    println(result.summary())

    val suite2 = TestSuite()
    suite2.add(TestCaseTest("testTemplateMethod"))
    suite2.add(suite)
    val result2 = TestResult()
    suite2.run(result2)
    println(result2.summary())
}
