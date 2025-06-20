/*
 콜백 함수는 함수입니다.

 만약 콜백 함수로써 어떤 객체의 메소드를 전달하더라도
 그 메소드는 메소드가 아닌 함수로써 실행됩니다.

 */

var obj = {
  vals: [1, 2, 3],
  logValues: function(v, i) {
    console.log(this, v, i)
  },
}

obj.logValues(1, 2); // { vals: [ 1, 2, 3 ], logValues: [Function: logValues] } 1 2 === obj

[4, 5, 6].forEach(obj.logValues)

// global {...} 4 0
// global {...} 5 1
// global {...} 6 2

/*
실행 결과에서 알 수 있듯이 obj.logValues 에서 this 는 obj 이지만,
콜백 함수로써 전달하게 되면 이 때의 this 는 전역 객체인 global 이 됩니다.
 */