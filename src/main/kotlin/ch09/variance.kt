fun main() {
    // 무공변 상태 (invariance)
//    var node: TreeNode<Any> = TreeNode<String>("Hello") // error: type mismatch
    // TreeNode<String> 은 TreeNode<Any> 의 하위 타입이 아니다.

    // 공변 상태 (covariant)
    val stringProducer: () -> String = { "hello" }
    val anyProducer: () -> Any = stringProducer
    println(anyProducer())

    // 반공변 상태 (contravariant)
    val anyConsumer: (Any) -> Unit = { println(it) }
    val stringConsumer: (String) -> Unit = anyConsumer
    stringConsumer("hello")

    // X<T>
    // X 가 생산자 역할을 할 경우: T 를 공변적으로 선언할 수 있고,
    // A 가 B 의 하위 타입이면 X<A> 도 X<B> 의 하위 타입이 된다.
    // X 가 소비자 역할을 할 경우: T 를 반공변적으로 선언할 수 있고,
    // B 가 A 의 하위 타입이면 X<A> 가 X<B> 의 하위 타입이 된다.
    // 나머지는 무공변 상태.
}