fun View.showOff() = println("I'm a view!")
fun Button.showOff() = println("I'm a button!")

// 확장 함수는 override 불가능
// 별개의 메소드로 동작
fun main() {
    val view: View = Button()
    view.showOff() // "I'm a view!"

    val button = Button()
    button.showOff() // I'm a button!
}