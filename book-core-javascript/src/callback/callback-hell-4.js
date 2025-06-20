/*
ES6의 generator 를 이용하여 callback-hell 해결
 */

var addCoffee = function(prevName, name) {
  setTimeout(function () {
    coffeeMaker.next(prevName ? prevName + ', ' + name : name)
  }, 500)
}

var coffeeGenerator = function* () {
  var espresso = yield addCoffee('', '에스프레소');
  console.log(espresso)
  var americano = yield addCoffee('', '아메리카노');
  console.log(americano)
  var mocha = yield addCoffee('', '카페모카');
  console.log(mocha)
  var latte = yield addCoffee('', '카페라뗴');
  console.log(latte)
}

var coffeeMaker = coffeeGenerator();

coffeeMaker.next()