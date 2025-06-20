// 콜백 함수 내부에서의 this

setTimeout(function() {
  console.log(this) // global {...}
}, 300);

[1, 2, 3, 4, 5].forEach(function(x) {
  console.log(this, x) // global {...}
})

// setTimeout, forEach 메소드는 내부에서 콜백 함수를 호출 할 때 this 를 지정하지 않기 때문에 전역 객체인 global 이 출력됩니다.