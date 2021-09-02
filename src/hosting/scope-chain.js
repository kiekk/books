// hoisting.js 예제
// var a = 1
//
// function outer() {
//   function inner() {
//     console.log(a)
//     var a = 3
//   }
//
//   inner()
//   console.log(a)
// }
//
// outer()
// console.log(a)

/*
function outer() {
  function inner() {
    var a
    console.log(a)
    a = 3
  }

  inner()
  console.log(a)
}

outer()
console.log(a)
*/

/*
var a = 1

function outer() {
  function inner() {
    console.log(a)
    //var a = 3
  }

  inner()
  console.log(a)
}

outer()
console.log(a)
*/

var a = 1

function outer() {
  var a = 2
  function inner() {
    console.log(a)
    //var a = 3
  }

  inner()
  console.log(a)
}

outer()
console.log(a)
