/*
callback-hell-2 에서 작성한 프로미스 기반의 코드를 함수를 이용해 더 짧게 표현
 */

var addCoffee = function (name) {
  return function (prevName) {
    return new Promise(function (resolve) {
      setTimeout(function () {
        var newName = prevName ? (prevName + ', ' + name) : name
        console.log(newName)
        resolve(newName)
      }, 500)
    })
  }
}

addCoffee('에스프레소')()
.then(addCoffee('아메리카노'))
.then(addCoffee('카페모카'))
.then(addCoffee('카페라떼'));