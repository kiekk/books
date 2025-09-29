class Assert {
    companion object {
        fun assertEquals(expected: Any, actual: Any) {
            if (expected != actual) {
                throw AssertionError("Expected <$expected> but was <$actual>")
            }
        }
    }
}