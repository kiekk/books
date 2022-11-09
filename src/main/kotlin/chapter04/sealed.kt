interface Expr2
class Num2(val value: Int) : Expr2
class Sum2(val left: Expr2, val right: Expr2) : Expr2

fun eval(e: Expr2): Int =
    when (e) {
        is Num2 -> e.value
        is Sum2 -> eval(e.right) + eval(e.left)
        // else 를 추가하지 않을 경우 에러 발생
        else ->
            throw IllegalArgumentException("Unknown expression")
    }
/*
    Expr2 는 확장이 가능하기 때문에 Num2, Sum2 이외의 클래스가 추가될거라 판단하여
    else 를 반드시 작성하도록 강제함.
 */

fun main() {
    println(eval(Sum2(Sum2(Num2(1), Num2(2)), Num2(4))))
}
