var a = 1

function outer() {
  function inner() {
    console.log(a) // (1)
    var a = 3
  }

  inner()
  console.log(a) // (2)
}

outer()
console.log(a) // (3)

/*
  예측
  (1) = 1, 3
  (2) = 1
  (3) = 1

  출력
  (1) = undefined
  (2) = 1
  (3) = 1
 */
