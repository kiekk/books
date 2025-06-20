// ES6에서는 this가 바인딩되지 않을 경우 전역 객체를 바라보는 것을 해결하기 위해
// this를 바인딩하지 않는 arrow function을 추가했습니다.

// arrow function은 실행 컨텍스트를 생성할 때 this 바인딩 작업이 빠지게 되어
// 상위 스코프의 this를 바로 사용할 수 있게 됩니다.

var obj = {
  outer: function () {
    console.log(this) // { outer: f } == obj

    var innerFunc = () => {
      console.log(this) // { outer: f } == obj
    }

    innerFunc()
  }
}

obj.outer()