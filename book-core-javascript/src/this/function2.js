// 함수 내부에서의 this

// 함수를 함수로서 호출할 경우 this는 지정되지 않습니다.
// this가 지정되지 않을 경우 전역 객체(window, global)을 바라봅니다.

var obj1 = {
  outer: function () {
    console.log(this) // { outer: f } == obj1

    var innerFunc = function () {
      console.log(this) // global {...} // { innserMethod: f } == obj2
    }

    innerFunc() // global {...}

    var obj2 = {
      innerMethod: innerFunc
    }

    obj2.innerMethod() // { innserMethod: f } == obj2
  }
}

obj1.outer()