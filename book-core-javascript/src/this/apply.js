// apply 메소드는 call 메소드와 기능이 동일합니다.

// call 메소드는 첫 번째 인자를 제외한 모든 인자들을 함수의 매개변수로 지정하지만
// apply 메소드는 두 번째 인자를 배열로 받아 함수의 매개변수로 설정합니다.

var func = function(a, b, c) {
  console.log(this, a, b, c)  // { x: 1 } 4 5 6
}

func.apply({ x: 1 }, [4, 5, 6])

var obj = {
  a: 1,
  method: function(x, y) {
    console.log(this.a, x, y) // 4 5 6
  },
}

obj.method.apply({ a: 4 }, [5, 6])