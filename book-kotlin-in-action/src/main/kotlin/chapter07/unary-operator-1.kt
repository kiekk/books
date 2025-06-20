package chapter07

import java.math.BigDecimal

operator fun Point.unaryMinus(): Point { // 단항 연산자 함수는 파라미터가 없다.
    // 좌표에서 각 성분에서 음수를 취한 새 점을 반환한다.
    return Point(-x, -y)
}

operator fun BigDecimal.inc() = this + BigDecimal.ONE

fun main() {
    val p = Point(10, 20)
    println(-p)

    var bd = BigDecimal.ZERO
    println(bd++)
    println(++bd)
}

/*
 오버로딩할 수 있는 단항 연산자
 + -> unaryPlus
 - -> unaryMinus
 ! -> not
 ++ -> inc
 ---> dec
 */