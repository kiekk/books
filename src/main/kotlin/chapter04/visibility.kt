internal open class TalkativeButton : Focusable {
    private fun yell() = println("Hey!")
    protected fun whisper() = println("Let's talk!")
}

// public 멤버가 internal 인 TalkativeButton 을 노출
//fun TalkativeButton.giveSpeech() {
    // yell 은 private 멤버
//    yell()
    // whisper 은 protected 멤버
//    whisper()
//}

/*
    public(기본 가시성): 모든 곳에서 볼 수 있다.
    internal: 같은 모듈 안에서만 볼 수 있다.
    protected: 하위 클래스 안에서만 볼 수 있다.
    private: 같은 클래스 안에서만 볼 수 있다.
 */