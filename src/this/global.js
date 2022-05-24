// this 는 함수가 호출될 때 결정됩니다.

// 전역 공간에서의 this
//console.log(window) -> 브라우저 환경에서의 전역 객체는 window
// node 환경에서의 전역 객체는 global이기 때문에 node에서 window를 사용하면 에러가 발생합니다.
/*
C:\study\study-core-javascript\src\this\global.js:5
console.log(window)
            ^

ReferenceError: window is not defined

 */

(function () {
  console.log(this) // Object [global]
  console.log(global) // Object [global]
  console.log(this === global) // true
})()
