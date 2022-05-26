// call / apply 메소드 활용

// 유사 배열 객체에는 배열 메소드를 적용할 수 없습니다.

// 이 경우 call, apply 메소드를 사용해 유사 배열 객체에도 배열 메소드를 사용할 수 있습니다.

var obj = {
  0: 'a',
  1: 'b',
  2: 'c',
  length: 3
}

Array.prototype.push.call(obj, 'd')
console.log(obj)  // { '0': 'a', '1': 'b', '2': 'c', '3': 'd', length: 4 }

var arr = Array.prototype.slice.call(obj)
console.log(arr)  // [ 'a', 'b', 'c', 'd' ]