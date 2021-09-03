type upper = { a: string, b: string }
type sub = { a: string, b: string, c: string }

let objectUpper: upper = { a: 'a', b: 'b' }
let objectSub: sub = { a: 'a', b: 'b', c: 'c' }

objectUpper = objectSub
// objectSub = objectUpper // 에러 발생

console.log(objectUpper)
