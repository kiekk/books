// name 프로퍼티는 함수의 이름을 반환하는데,
// bind 메소드로 생성한 함수의 경우 name 프로퍼티를 호출하면
// bound XXX 와 같이 bound 문자열이 prefix 로 붙는 특징이 있습니다.
// 따라서 name 프로퍼티를 통해 현재 이 함수가 bind 메소드로 생성된
// 함수인지 확인할 수 있습니다.

var func = function(a, b, c, d) {
  console.log(this, a, b, c, d)
}

var bindFunc = func.bind({ x: 1 }, 4, 5)

console.log(func.name)  // func
console.log(bindFunc.name)  // bound func