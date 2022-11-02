interface Car2 {
    fun move() {
        println("I'm riding")
    }
}

interface Ship {
    fun move()
}

class Amphibia : Car2, Ship {
    override fun move() {
        super.move()
    }
}

fun main() {
    // Car 에서 상속 받은 메소드를 호출
    // 합쳐지는 멤버에 대한 구현이 둘 이상의 상위 타입에 존재할 경우
    // super 호출 자체가 모호해집니다.
    Amphibia().move()
}