function check(p: string | number | boolean): string | number | boolean {
    if(typeof p === 'string') {
        // p가 string일 때 처리
        return p
    } else if (typeof p === 'number') {
        // p가 number일 때 처리
        return p
    } else {
        // p가 boolean일 때 처리
        return p
    }
}

console.log(typeof check(1), check(1))
console.log(typeof check('hello'), check('hello'))
console.log(typeof check(true), check(true))

/*
    실행 결과
    number 1
    string hello
    boolean true
*/