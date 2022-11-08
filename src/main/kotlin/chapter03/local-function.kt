class User(val id: Int, val name: String, val address: String)

// 로컬 함수를 확장 함수로 추출
fun User.validateBeforeSave() {
    fun validate(value: String, fieldName: String) {
        if (value.isEmpty()) {
            throw IllegalArgumentException("Can't save user $id: empty $fieldName")
        }
    }

    validate(name, "Name")
    validate(address, "Address")
}

fun saveUser(user: User) {
    user.validateBeforeSave()
}

fun main() {
    saveUser(User(1, "", ""))
}