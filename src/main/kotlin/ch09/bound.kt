class TreeNode<T>(val data: T) {
    private val _children = arrayListOf<TreeNode<T>>()
    var parent: TreeNode<T>? = null
        private set

    val children: List<TreeNode<T>> get() = _children

    fun addChild(data: T) = TreeNode(data).also {
        _children += it
        it.parent = this
    }

    override fun toString() =
        _children.joinToString(prefix = "$data {", postfix = "}")
}

fun <T : Number> TreeNode<T>.average(): Double {
    var count = 0
    var sum = 0.0
    walkDepthFirst {
        count++
        sum += it.toDouble()
    }
    return sum / count
}

fun <T> TreeNode<T>.walkDepthFirst(action: (T) -> Unit) {
    children.forEach { it.walkDepthFirst(action) }
    action(data)
}

fun main() {
    val intTree = TreeNode(1).apply {
        addChild(2).addChild(3)
        addChild(4).addChild(5)
    }
    println(intTree.average()) // 3.0

    val doubleTree = TreeNode(1.0).apply {
        addChild(2.0)
        addChild(3.0) // 2.0
    }
    println(doubleTree.average())

    val stringTree = TreeNode("Hello").apply {
        addChild("World")
        addChild("!!!")
    }
    // public fun <T : Number> TreeNode<TypeVariable(T)>.average(): Double defined in root package in file bound.kt
    // println(stringTree.average())

    // final 클래스를 상위 바운드로 사용하면 한 가지 타입만 지정할 수 있습니다.
    // Int, Double 은 Number 의 하위 타입이므로 가능, String 은 Number 의 하위 타입이 아니므로 불가능
}