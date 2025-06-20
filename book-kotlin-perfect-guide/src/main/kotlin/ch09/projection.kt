// out 프로젝션
fun <T> TreeNode<T>.addSubtree(node: TreeNode<out T>): TreeNode<T> {
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
    var root2 = TreeNode<Number>(123)
    val subRoot2 = TreeNode(456.7)
    root2.addSubtree(subRoot2) // error: type mismatch
    println(root2) // 123 {456.7 {}}
}