// 명시적으로 this 에 직접 값을 바인딩 할 수 있습니다.

// call 메소드는 메소드의 호출 주체인 함수를 즉시 실행하도록 합니다.
// 이 때 call 메소드의 첫 번째 인자를 this 로 바인딩하고, 이후 인자들을 호출할
// 함수의 매개변수로 사용합니다.

var func = function(a, b, c) {
  console.log(this, a, b, c)
}

func(1, 2, 3) // global {...} 1 2 3

func.call({ x: 1 }, 4, 5, 6) // { x: 1 } 4 5 6

// func 함수를 그냥 호출할 경우 this 는 전역 객체인 global 이 되지만,
// call 메소드를 통해 직접 this 에 값을 할당하면 할당한 값이 this 로 출력됩니다.

var obj = {
  a: 1,
  method: function(x, y) {
    console.log(this.a, x, y)
  },
}

obj.method(2, 3)  // 1 2 3
obj.method.call({ a: 4 }, 5, 6) // 4 5 6