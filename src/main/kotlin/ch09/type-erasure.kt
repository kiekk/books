fun main() {
    var list = listOf(1, 2, 3) // List<Int>
    list is List<Number> // Ok

    // Cannot check for instance of erased type: List<String>
    // list is List<String>

    val collection: Collection<Int> = setOf(1, 2, 3)

    // collection 은 Collection<Int> 타입,
    // Collection<Int> 와 List<Int> 는 다르다.
    if (collection is List<Int>) {
        println("list")
    }
}