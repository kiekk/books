package chapter06

fun sendEmailTo(email: String) {
    println("Sending email to $email")
}

fun main() {
    // let 함수는 ?. 와 함께 사용되며 null 이 아닐 경우에만 let 함수가 실행된다.
    var email: String? = "yole@example.com"
    email?.let { sendEmailTo(it) }
    email = null
    email?.let { sendEmailTo(it) } // email 은 null 이기 때문에 let 함수는 실행되지 않는다.
}