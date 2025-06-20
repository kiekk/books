import hello from './export';
let helloMessage = hello('hello');
console.log(hello('hello'));
console.log(helloMessage);
/*
    실행 결과
    { first: 'hello', second: 'hello' }
    { first: 'hello', second: 'hello' }
*/ 
