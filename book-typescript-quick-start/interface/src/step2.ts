// 객체 리터럴의 구조를 일관되게 하려면 배열 요소의 타입을 객체 리터럴 타입으로 선언
let person2: { name: string, city: string }[]

person2 = [
    { name: 'a', city: 'seoul'},
    { name: 'b', city: 'daejeon'},
    { name: 'c', city: 'daegu'},
]

console.log(JSON.stringify(person2))

/*
    실행 결과
    [{"name":"a","city":"seoul"},{"name":"b","city":"daejeon"},{"name":"c","city":"daegu"}]
 */

// 아래 방법으로도 사용 가능
// type alias 사용 
// type objectLiteralType = { name: string, city: string }
// let person2: objectLiteralType[]