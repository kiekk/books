type MyArrayType = Array<number | boolean>
let myArray: MyArrayType = [1, true]
console.log(myArray.toString())
console.log(typeof myArray[0], typeof myArray[1])

/*
    실행 결과
    1,true
    number boolean
*/