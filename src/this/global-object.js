
// 자바스크립트의 모든 변수는 특정 객체의 프로퍼티로써 동작합니다.

// 전역 변수를 선언하면 전역 객체의 프로퍼티로 등록합니다.


// 하지만 node에서 전역 객체로써 this를 호출하면 {} 빈 객체가 할당됩니다.
// 이는 module.exports 인데 전역 객체에서의 this는 module.exports이고
// 전역 객체로써 global 객체를 호출하려면 함수 내부에서 this를 호출해야 합니다.

// node는 module 을 사용하기 때문에 var로 선언해도 전역 객체의 프로퍼티로 등록되지 않습니다.


function test () {
  var a = 1;
  console.log(global.a) // undefined
  console.log(this.a) // undefined
}

test();