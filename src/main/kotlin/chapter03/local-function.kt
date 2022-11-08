class User(val id: Int, val name: String, val address: String)

fun saveUser(user: User) {

    // 로컬 함수에서 외부 scope 의 변수 (user) 참조
    fun validate(value: String, fieldName: String) {
        if (value.isEmpty()) {
            throw IllegalArgumentException(
                "Can't save user ${user.id}: empty $fieldName"
            )
        }
    }

    validate(user.name, "Name")
    validate(user.address, "Address")
}

fun main() {
    saveUser(User(1, "", ""))
}