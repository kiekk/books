// 함수에 선언된 매개변수가 다를 경우
let funcUpper = (a: string) => a    // 상위 타입
let funcSub = (a: string, b: string) => a + b   // 하위 타입

funcSub = funcUpper
// funcUpper = funcSub // 에러

console.log(`${funcSub('hello', 'world')}`)
// 실제로는 funcUpper 함수가 호출
