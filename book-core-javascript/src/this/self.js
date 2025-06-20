// 메소드 내부 함수에서의 this 우회
// 호출 시 this가 없을 경우 전역 객체가 아닌 상위 스코프의 this를 사용할 수 있도록 합니다.

var obj = {
  outer: function () {
    console.log(this) // { outer: f } == obj

    var innerFunc1 = function () {
      console.log(this) // global {...}
    }

    innerFunc1()

    var self = this
    var innerFunc2 = function () {
      console.log(this) // global {...}
      console.log(self) // { outer: f } == obj
    }

    innerFunc2()
  }
}

obj.outer()