let a = 2

;(function () {
  console.log(a)

  let a = 2
})()

// let, const는 hoisting이 되지 않는듯 하다
// hoisting이 되면 c가 undefined로 출력되야 하는데 에러가 발생한다.
function b() {
  console.log(c)

  let c = 2
}
b()

function d() {
  console.log(e)

  const e = 3
}
d()
