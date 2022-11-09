// 멤버 변수인 nickname 과 생성자 파라미터를 구분하기 위해 접두사 _ (언더바) 사용
class User2 constructor(_nickname: String) {
    val nickName: String

    // 초기화 블록은 주 생성자와 함께 사용
    init {
        nickName = _nickname
    }
}

class User3 constructor(nickname: String) {
    val nickName: String

    // 초기화 블록은 주 생성자와 함께 사용
    init {
        // _ 언더바를 사용하지 않고 this 로 구분 가능
        this.nickName = nickname
    }
}

// 프로퍼티를 주 생성자의 파라미터로 바로 초기화
class User4 constructor(_nickname: String) {
    val nickName = _nickname
}

// 프로퍼티 정의, 초기화 코드 간략화
class User5 constructor(val nickname: String) {
}

// 생성자 파라미터에 디폴트 값 설정
class User6 constructor(val nickname: String, val isSubscribed: Boolean = true) {
}

// 주 생성자를 private 로 변환하여 주 생성자를 호출할 수 없게 함
class User7 private constructor(){
}