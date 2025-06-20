class Button2 : Clickable, Focusable {
    override fun click() = println("I was clicked")

    override fun showOff() {
        super<Focusable>.showOff()
        super<Clickable>.showOff()
        // super<type> 으로 어떤 상위 타입의 메소드를 호출할 지 지정
    }
}

interface Clickable {
    fun click()
    fun showOff() = println("I'm clickable!")
}

interface Focusable {
    fun setFocus(b: Boolean) = println("I ${if (b) "got" else "lost"} focus.")
    fun showOff() = println("I'm focusable!")
}

fun main() {
    val button = Button2()
    button.showOff()
    button.setFocus(true)
    button.click()
}