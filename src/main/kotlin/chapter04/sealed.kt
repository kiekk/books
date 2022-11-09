import Expr3.*;

sealed class Expr3 {
    class Num2(val value: Int) : Expr3()
    class Sum2(val left: Expr3, val right: Expr3) : Expr3()
}

fun eval(e: Expr3): Int =
    when (e) {
        is Num2 -> e.value
        is Sum2 -> eval(e.right) + eval(e.left)
    }

// 에러는 발생하지 않지만, 컴파일러가 else 문을 추가하는 것을 권장합니다.
fun main() {
    println(eval(Sum2(Sum2(Num2(1), Num2(2)), Num2(4))))
}
