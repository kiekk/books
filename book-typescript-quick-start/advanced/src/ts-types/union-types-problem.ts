// 타입 가드 적용 X
function myIndexOf(x: number | string, y: string) {
    //return x.indexOf(y) // 오류
}

console.log(myIndexOf('hello', 'e'))