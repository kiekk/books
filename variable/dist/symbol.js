let hello = Symbol('hello');
let hello2 = Symbol('hello');
console.log(hello === hello2);
console.log(hello, hello2);
console.log(typeof hello);
/*
    실행 결과
    false
    Symbol(hello) Symbol(hello)
    symbol
 */ 
