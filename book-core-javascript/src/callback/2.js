// 이번에는 callback 함수를 익명 함수가 아니라 함수를 작성해
// 작성한 함수명을 전달했습니다.

var count = 0
var cbFunc = function () {
  console.log(count)
  if(++count > 4) {
    clearInterval(timer)
  }
}

var timer = setInterval(cbFunc, 300)

/*
  여기서 중요한건 '제어권'입니다.

  setInterval 의 callback 함수로 cbFunc 를 전달했기 때문에
  setInterval 이 cbFunc 의 제어권을 가져 일정 시간마다 스스로 cbFunc 를 실행했습니다.

  이와 같이 어떤 함수에 callback 함수를 전달하게 되면 그 함수는 callback 함수에 대한
  제어권을 가진다고 말할 수 있습니다.
 */