function concat(strs: any, strs2: any): any {
    return strs + strs2
}

console.log(concat(10, 100))
console.log(concat('abc', 1))
console.log(concat('abc', true))

/*
    실행 결과
    110
    abc1
    abctrue
*/