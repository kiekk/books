/*
콜백 함수를 기명 함수로 전환하여 callback hell 해결
 */

var coffeeList = ''

var addEspresso = function (name) {
  coffeeList = name
  console.log(coffeeList)
  setTimeout(addAmericano, 500, '아메리카노')
}

var addAmericano = function (name) {
  coffeeList += ', ' + name
  console.log(coffeeList)
  setTimeout(addMocha, 500, '카페모카')
}

var addMocha = function (name) {
  coffeeList += ', ' + name
  console.log(coffeeList)
  setTimeout(addLatte, 500, '카페라뗴')
}

var addLatte = function (name) {
  coffeeList += ', ' + name
  console.log(coffeeList)
}

setTimeout(addEspresso, 500, '에스프레소')

/*
에스프레소
에스프레소, 아메리카노
에스프레소, 아메리카노, 카페모카
에스프레소, 아메리카노, 카페모카, 카페라뗴
 */