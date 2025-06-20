var count = 0
var timer = setInterval(function() {
  console.log(count)
  if (++count > 4) {
    clearInterval(timer)
  }
}, 300)

// setInterval 의 구조는 아래와 같습니다.
/*
  var intervalId = scope.setInterval(func, delay[, param1, param2, ...])

  매개변수로 func, delay 는 필수로 전달해야 하며,
  뒤에 param 은 옵션입니다.

  이 때 func 가 바로 callback 함수이며 위에서 callback 함수로써
  익명 함수를 전달했습니다.
 */