fun getFacebookName(accountId: Int) = "fb:$accountId"

interface User10 {
    val nickname: String
}
class PrivateUser(override val nickname: String) : User10

class SubscribingUser(val email: String) : User10 {
    // 매번 nickname 호출 시 마다 substringBefore() 를 호출
    override val nickname: String
        get() = email.substringBefore('@')
}

class FacebookUser(accountId: Int) : User10 {
    // 초기화 시 getFacebookName() 을 호출
    override val nickname = getFacebookName(accountId)
}

fun main() {
    println(PrivateUser("test@kotlinlang.org").nickname)
    println(SubscribingUser("test@kotlinlang.org").nickname)
}