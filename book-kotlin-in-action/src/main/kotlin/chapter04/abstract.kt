abstract class Animated {
    // 추상 함수는 기본적으로 열려 있다. open
    abstract fun animate()

    // 일반 함수는 기본적으로 final 이고 오버라이드를 허용하려면 open 키워드를 명시적으로 사용해야 한다.
    open fun stopAnimating() {}

    fun animateTwice() {}
}

class Movie: Animated() {
    // 추상 함수인 animate, 그리고 open 키워드가 사용된 stopAnimating 만 오버라이드가 가능하다.
    override fun animate() {}

    override fun stopAnimating() {}
}