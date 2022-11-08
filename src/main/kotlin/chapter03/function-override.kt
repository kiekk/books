open class View {
    // 오버라이딩을 허용하기 위해서는 open 키워드를 사용해야 한다.
    open fun click() = println("View clicked")
}

class Button : View() {
    override fun click() = println("Button clicked")
}

fun main() {
    val view: View = Button()
    view.click()
}