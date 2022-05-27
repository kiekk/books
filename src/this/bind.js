// bind 메소드는 ES5에 추가된 기능으로,
// call 메소드와는 다르게 실행하지는 않고
// 넘겨 받은 this 와 매개변수들을 바탕으로 새로운 함수를 반환합니다.

var func = function(a, b, c, d) {
  console.log(this, a, b, c, d)
}

func(1, 2, 3, 4)  // global {...} 1 2 3 4

var bindFunc1 = func.bind({ x: 1 })

bindFunc1(5, 6, 7, 8) // { x:1 } 5 6 7 8

var bindFunc2 = func.bind({ x: 1 }, 4, 5)

bindFunc2(6, 7)
bindFunc2(7, 8)