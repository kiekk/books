import * as my from './dts/my'

var result = my.MyLibrary.getMaxNumber([10, 20, 30, 40, 50])

console.log(result)

// 타입 정의 파일(d.ts)로 인해 getMaxNumber는 number[]이기 때문에
// 배열 이외에 값을 입력하면 오류 발생