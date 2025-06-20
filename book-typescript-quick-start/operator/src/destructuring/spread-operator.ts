let arr: number[] = [1, 2]
let arr2: number[] = [...arr, 3, 4]
console.log('1. ', arr2)

// array destructuring
let [firstItem, ...rest]: [number, number, number] = [10, 20, 30]
console.log('2. ', firstItem)
console.log('3. ', rest)
console.log('4. ', rest[0])

/*
    실행 결과
    1.  [ 1, 2, 3, 4 ]
    2.  10
    3.  [ 20, 30 ]
    4.  20
 */
