fun <T> TreeNode<T>.addSubtree(node: TreeNode<T>): TreeNode<T> {
    val newNode = addChild(node.data)
    node.children.forEach { newNode.addSubtree(it) }
    return newNode
}

fun main() {
    val root = TreeNode("abc")
    val subRoot = TreeNode("def")

    root.addSubtree(subRoot)
    println(root) // abc {def {}}

    // 이 경우는 같은 타입일 때만 정상적으로 동작한다.
    val subRoot2 = TreeNode(456.7)
//    root.addSubtree(subRoot2) // error: type mismatch
}