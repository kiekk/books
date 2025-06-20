var newArr = [10, 20, 30].map(function (currentValue, index) {
  console.log(currentValue, index)
  return currentValue + 5
})

console.log(newArr)
/*
10 0
20 1
30 2
[ 15, 25, 35 ]
 */

/*
Array.prototype.map(callback[, thisArg])

map 메소드는 첫 번째 인자로 callback 함수를 받고,
map 메소드의 대상이 되는 배열의 모든 요소들을 하나씩 꺼내어
callback 함수의 인자로 전달하며 callback 함수를 반복 실행하고
callback 함수의 실행 결과들을 모아 새로운 배열을 만듭니다.
 */