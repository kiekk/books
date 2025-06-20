data class Client(val name: String, val postalCode: Int)

/*
data class 는 equals, hashCode, toString 메소드를 자동으로 생성해준다.

data class 에서는 프로퍼티를 var 로 사용해도 되지만,
불변성 클래스로 만들기 위해 val 키워드를 사용하라고 권장한다.
 */
fun main() {
    val bob = Client("Bob", 973293)
    println(bob.copy(postalCode = 382555))
}
