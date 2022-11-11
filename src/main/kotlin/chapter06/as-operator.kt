package chapter06

class Person2(val firstName: String, val lastName: String) {
    override fun equals(o: Any?): Boolean {
        // as? 를 사용해 안전하게 타입 캐스팅을 할 수 있다.
        // as? 는 대상 타입으로 캐스팅이 불가능할 경우 null 을 반환하며
        // 엘비스 연산자와 함께 사용하여 디폴트 값을 지정할 수 있다.
        val otherPerson = o as? Person2 ?: return false

        return otherPerson.firstName == firstName &&
                otherPerson.lastName == lastName
    }

    override fun hashCode(): Int =
        firstName.hashCode() * 37 + lastName.hashCode()
}

fun main() {
    val p1 = Person2("Dmitry", "Jemerov")
    val p2 = Person2("Dmitry", "Jemerov")
    println(p1 == p2)
    println(p1.equals(42))
}
