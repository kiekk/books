let { a, b, ...c } = { a: 10, b: 20, c: 30, d: 40 }
console.log(a)
console.log(b)
console.log(c)

let test = { a: 10, b: 20, c: 30, d: 40 }
let test2 = { ...test, b: 25 }
// b가 spread-operator인 test에 존재한다면 값을 덮어 씌움

console.log(test)
console.log(test2)