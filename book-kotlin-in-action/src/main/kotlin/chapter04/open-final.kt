// 인터페이스는 기본적으로 열려 있다. open
interface Clickable2 {
    fun click()
    fun showOff() = println("I'm clickable!")
}

// 클래스는 기본적으로 닫혀 있다. final
// 상속을 허용하려면 open 키워드를 명시적으로 사용해야 한다.
open class RichButton : Clickable {

    // 함수 또한 기본적으로 닫혀 있다. final
    // 함수도 override 를 허용하려면 open 키워드를 명시적으로 사용해야 한다.
    fun disable() {}

    open fun animate() {}

    // 오버라이드 한 메소드는 기본적으로 열려 있다. open
    // 오버라이드 한 메소드를 하위 클래스에서 다시 오버라이드 하지 못하게 하려면 final 키워드를 명시적으로 사용해야 한다.
    override fun click() {}
}