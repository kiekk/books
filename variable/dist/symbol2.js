const uniqueKey = Symbol();
let obj = {};
obj[uniqueKey] = 1234;
console.log(obj[uniqueKey]);
console.log(obj);
/*
    실행 결과
    1234
    { [Symbol()]: 1234 }

    심볼 키는 충돌을 피할 목적으로 생성헀으므로 객체에서
    심볼키는 무시
 */ 
