function a() {
  console.log(b) // (1)

  var b = 'bbb' // hoisting 1

  console.log(b) // (2)

  function b() {} // hoisting 2

  console.log(b) // (3)
}
a()

/*
  예측
  (1) = undefined
  (2) = bbb
  (3) = [Function: b]

  출력
  (1) = [Function: b]
  (2) = bbb
  (3) = bbb
 */

/*
function a() {
  var b // hoisting 1

  // hoisting 2
  1) function b() {}
  OR
  2) var b = function b() {}

  console.log(b)

  b = 'bbb'
  console.log(b)
  console.log(b)
}
a()
*/
