/*
콜백 함수 내부의 this 에 다른 값을 바인딩 하기 - 2
 */

var obj1 = {
  name: 'obj1',
  func: function() {
    console.log(this.name)
  },
}


setTimeout(obj1.func.bind(obj1), 1000)  // obj1

var obj2 = { name: 'obj2' }
setTimeout(obj1.func.bind(obj2), 1500)  // obj2