interface User11 {
    val email: String
    val nickname: String
        get() = email.substringBefore('@')
}
// custom getter 가 있는 nickname 은 하위 클래스에서 override 할 필요가 없지만,
// abstract property 인 email 은 하위 클래스에서 반드시 override 해야 한다.