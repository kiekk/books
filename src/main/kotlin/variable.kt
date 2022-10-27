fun main() {
    val timeInSeconds = 15
    println(timeInSeconds)

    var helloText = "hello"
    // : String 타입 추론에 의해 타입 생략 가능
    // var helloText: String = "hello"
    println(helloText)

    // null 허용일 경우 타입 뒤에 ? 작성
    // 이 경우에는 타입 생략 불가능
    var nullText: String? = null
    // nullText.chars() // Compile Error 발생 NPE 경고

    printCharsText(helloText)
    printCharsText(nullText)

    /*
    val: value 의 약자로 js 의 const 와 동일
    var: variable 의 약자로 js 의 var 와 동일
     */

    // var fun = "fun" // fun 키워드는 예약어이기 때문에 변수명으로 사용 불가
    var `fun` = "fun"   // `` 로 감싸주면 예약어도 변수명으로 사용 가능
    // 이 경우는 java 에서 이미 사용중인 변수명인데 코틀린에서 예약어인 경우 호환하기 위해 사용
    // 왠만하면 예약어는 변수명으로 사용 X
}

fun printCharsText(text: String?) {
    // !! 은 해당 변수가 null 이 아님을 확실히 하여 Compile Error 가 발생하지 않습니다.
    // 하지만 실제 null 이 전달될 경우 런타임에 NPE 가 발생하기 때문에
    // 확실하지 않을 경우에는 지양해야 합니다.
    println(text!!.chars())
}