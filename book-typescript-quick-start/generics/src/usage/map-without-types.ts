let myMap = new Map()
myMap.set(1, 'one')
myMap.set(2, 'two')

// for of로 순회
for(let v of myMap) {
    console.log(v)
}
// [ 1, 'one' ]
// [ 2, 'two' ]


// 내장 이터레이터로 순회
let mapIter = myMap[Symbol.iterator]()

console.log(mapIter.next().value)
console.log(mapIter.next().value)
// [ 1, 'one' ]
// [ 2, 'two' ]
