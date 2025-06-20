namespace MyInfo1 {
    export let name = 'happy1'
    export function getName2() {
        return MyInfo2.name2
    }
}

namespace MyInfo2 {
    export let name2 = 'happy2'
    export function getName() {
        return MyInfo1.name
    }
}

console.log(MyInfo1.getName2())
console.log(MyInfo2.getName())

/*
    MyInfo1, MyInfo2는 컴파일시 var 선언자로 선언되어 호이스팅 되므로
    실행 순서와 상관없이 함수 영역간 호출이 가능합니다.
 */