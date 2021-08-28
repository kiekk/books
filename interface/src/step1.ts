// 배열 요소로 객체 리터럴이 올 수 있는데,
// 배열 타입을 지정하지 않을 경우 객체 리터럴의 구조가 임의의 형태가 됩니다.
let person = [
    { name1: 'a', city1: 'seoul'},
    { name2: 'b', city2: 'daejeon'},
    { name3: 'c', city3: 'daegu'}
]

console.log(JSON.stringify(person))
/*
    실행 결과
    [{"name1":"a","city1":"seoul"},{"name2":"b","city2":"daejeon"},{"name3":"c","city3":"daegu"}]
 */