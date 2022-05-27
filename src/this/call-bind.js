/*
  상위 컨텍스트의 this를 내부 함수에서 사용하기 위해서는
  that, self 등의 우회 방법을 사용했었는데
  call, apply, bind를 사용해서 변경해보겠습니다.
 */

// basic
var obj = {
  outer: function() {
    console.log(this) // { outer: f } === obj

    var innerFunc = function() {
      console.log(this) // global {...}       ★★★★★★★
    }

    innerFunc()
  },
}

obj.outer()

// call 메소드 사용
var obj1 = {
  outer: function() {
    console.log(this) // { outer: f } === obj

    var innerFunc = function() {
      console.log(this) // { outer: f } === obj
    }

    innerFunc.call(this)
  },
}

obj1.outer()

// bind 메소드 사용
var obj2 = {
  outer: function() {
    console.log(this) // { outer: f } === obj

    var innerFunc = function() {
      console.log(this) // { outer: f } === obj
    }.bind(this)

    innerFunc()
  },
}

obj2.outer()

