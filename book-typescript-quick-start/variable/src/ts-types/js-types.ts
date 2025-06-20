let myBoolean = true    // boolean 타입
let myNumber = 10   // number 타입
let myString = 'hello'  // string 타입
let myUndefined    // undefined 타입
let myNull = null   // null 타입(객체가 비어있을 경우)
let myObject = { name: 'happy'} // 객체 타입(객체 리터럴)
let myObject2 = [1, 2, 3]   // 객체 타입(배열)
let myFunction = function(a) { return a }    // 함수 타입

console.log('boolean : ', typeof myBoolean)
console.log('number : ', typeof myNumber)
console.log('string : ', typeof myString)
console.log('undefined : ', typeof myUndefined)
// myNull의 타입은 null이 아닌 object
console.log('null : ', typeof myNull, myNull === null)
console.log('object : ', typeof myObject, typeof myObject2)
console.log('function : ', typeof myFunction)
console.log('undefined == null : ', (undefined == null))
console.log('undefined === null : ', (undefined === null))

/*
    실행 결과
    boolean :  boolean
    number :  number
    string :  string
    undefined :  undefined
    null :  object true
    object :  object object
    function :  function
    undefined == null :  true
    undefined === null :  false
 */