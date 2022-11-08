class Button2 : Clickable, Focusable {
    override fun click() = println("I was clicked")

    // showOff 를 오버라이드 하지 않을 경우
    // Kotlin: Class 'Button2' must override public open fun showOff(): Unit defined in Clickable because it inherits multiple interface methods of it
    // Clickable, Focusable 모두 showOff 메소드가 있어, 둘 중 어느 showOff 메소드를 호출해야 할지 모름
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