let arr = [1, 2];
let itObj = arr[Symbol.iterator]();
console.log('1 :', typeof itObj);
console.log('2 :', itObj.next());
console.log('3 :', itObj.next());
console.log('4 :', itObj.next());
/*
    실행 결과
    1 : object
    2 : { value: 1, done: false }
    3 : { value: 2, done: false }
    4 : { value: undefined, done: true }
 */
/*
    Array, Map, Set은 이미 [Symbol.iterator]() 메서드를 구현하고 있으므로
    for of, for in 문에서 사용 가능
 */ 
