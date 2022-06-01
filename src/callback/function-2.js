/*
콜백 함수 내부의 this 에 다른 값을 바인딩 하기
 */

var obj1 = {
  name: 'obj1',
  func: function () {
    var self = this
    return function () {
      console.log(self.name)  // obj1
    }
  }
}

var callback = obj1.func()
setTimeout(callback, 1000)