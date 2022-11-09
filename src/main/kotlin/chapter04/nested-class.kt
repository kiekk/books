import java.io.Serializable

interface State : Serializable

interface View2 {
    fun getCurrentState(): State
    fun restoreState(state: State) {}
}

class Button3 : View2 {
    override fun getCurrentState(): State = ButtonState()

    override fun restoreState(state: State) { /*...*/ }

    class ButtonState : State { /*...*/ }
}
