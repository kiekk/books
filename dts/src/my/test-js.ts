import * as my from './library/my'

var result = my.MyLibrary.getMaxNumber([10, 20, 30, 40, 1, 50]) 

console.log(typeof result, result)

// getMaxNumber가 any타입이라 배열 이외에 값을 입력해도 오류 발생 X
// 컴파일, 코드 실행시 런타임 에러 발생
// 타입 안정성 X