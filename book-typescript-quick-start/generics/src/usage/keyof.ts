// keyof extends는 상속받은 type의 union type으로 동작합니다.
function getValue<T, K extends keyof T>(obj: T, key: K) {
    return obj[key]
}

let numbersKeys = { one: 1, two: 2, three: 3 }
console.log(getValue(numbersKeys, 'one'))