var outer = function () {
  var a = 1
  var inner = function () {
    return ++a
  }
  return inner
}

console.log(outer())
console.log(outer())
outer = null  // outer 식별자의 inner 함수 참조를 끊음
// console.log(outer())  // TypeError: outer is not a function
