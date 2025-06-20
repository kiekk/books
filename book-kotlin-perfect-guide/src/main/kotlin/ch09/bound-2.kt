fun <T> TreeNode<T>.addChildren(vararg data: T) {
    data.forEach { addChild(it) }
}

// 제네릭이 아닌 함수로도 대체 가능
// fun TreeNode<Int>.sum(): Int {...}
fun <T : Int> TreeNode<T>.sum(): Int { // Warning
    var sum = 0
    walkDepthFirst { sum += it }
    return sum
}

fun <T : Comparable<T>> TreeNode<T>.maxNode(): TreeNode<T> {
    val maxChild = children.maxByOrNull { it.data } ?: return this

    return if (data >= maxChild.data) this else maxChild
}

// U 가 T 의 하위 타입
// T 가 Number 일 경우 U 는 Int, Double 등이 올 수 있다.
fun <T, U : T> TreeNode<U>.toList(list: MutableList<T>) {
    walkDepthFirst { list += it }
}

fun main() {
    val intTree = TreeNode(1).apply {
        addChild(2)
        addChildren(3, 4, 5)
    }
    println(intTree.average())

    // Double 은 Comparable<Double>의 하위 타입
    val doubleTree = TreeNode(1.0).apply {
        addChild(2.0)
        addChild(3.0)
    }
    println(doubleTree.maxNode().data) // 3.0

    // String 은 Comparable<String>의 하위 타입
    val stringTree = TreeNode("abc").apply {
        addChildren("xyz", "def")
    }
    println(stringTree.maxNode().data) // xyz

    val list = ArrayList<Number>()

    TreeNode(1).apply {
        addChild(2)
        addChild(3)
    }.toList(list)

    println(list) // [2, 3, 1]

    TreeNode(1.0).apply {
        addChild(2.0)
        addChild(3.0)
    }.toList(list)

    println(list) // [2, 3, 1, 2.0, 3.0, 1.0]
}