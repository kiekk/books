// any 타입 대신 string으로 타입 가드
function concat2(strs: string, strs2: string): string {
    return strs + strs2
}

console.log(concat2('abc', '1'))
console.log(concat2('abc', '123'))

/*
    실행 결과
    abc1
    abc123
*/