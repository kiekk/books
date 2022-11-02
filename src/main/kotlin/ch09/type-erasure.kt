import java.util.*

fun <T> TreeNode<T>.cancellableWalkDepthFirst(
    onEach: (T) -> Boolean
): Boolean {
    val nodes = LinkedList<TreeNode<T>>()

    nodes.push(this)

    while (nodes.isNotEmpty()) {
        val node = nodes.pop()
        if (!onEach(node.data)) return false
        node.children.forEach { nodes.push(it) }
    }

    return true
}

// 파라미터를 구체화하기 위해서는 reified 키워들르 사용
inline fun <reified T> TreeNode<*>.isInstanceOf() = cancellableWalkDepthFirst { it is T }

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

    val n = (listOf(1, 2, 3) as List<Number>)[0] // Ok, No cast needed
//    val s = (listOf(1, 2, 3) as List<String>)[0] // Warning, Unchecked cast: List<Int> to List<String>
    // 실행 시 에러 발생
    // class java.lang.Integer cannot be cast to class java.lang.String

    val tree = TreeNode<Any>("abc").addChild("def").addChild(123)
    println(tree.isInstanceOf<String>()) // false, Not Error
}