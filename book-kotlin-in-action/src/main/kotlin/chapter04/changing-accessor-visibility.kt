class LengthCounter {
    var counter: Int = 0
        private set  // setter 호출 불가, counter 프로퍼티는 외부에서 값을 변경할 수 없다.

    fun addWord(word: String) {
        counter += word.length
    }
}

fun main() {
    val lengthCounter = LengthCounter()
    lengthCounter.addWord("Hi!")
    println(lengthCounter.counter)
}