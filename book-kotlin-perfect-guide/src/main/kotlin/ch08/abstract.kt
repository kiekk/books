abstract class Entity(val name: String)

class Person9(name: String, val age: Int) : Entity(name)

// 추상 클래스의 부 생성자에서 위임 호출
class Person10 : Entity {
    constructor(name: String) : super(name)
    constructor(
        firstName: String,
        familyName: String
    ) : super("$firstName $familyName")
}

fun main() {
    // 추상 클래스는 인스턴스화 불가능
    // val entity = Entity("Unknown")
}
