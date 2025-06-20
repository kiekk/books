function a(x) {
  // hoisting 1
  console.log(x) // (1)
  var x // hoisting 2
  console.log(x) // (2)
  var x = 2 // hoisting 3
  console.log(x) // (3)
}

a(1)

/*
  예측
  (1) = 1
  (2) = undefined
  (3) = 2

  출력
  (1) = 1
  (2) = 1
  (3) = 2
 */

/*
function a() {
  var x     // hoisting 1
  var x     // hoisting 2
  var x     // hoisting 3

  x = 1
  console.log(x)
  console.log(x)

  x = 2
  console.log(x)
}
a(1)
*/
