setTimeout(function (name) {
  var coffeeList = name
  console.log(coffeeList)

  setTimeout(function (name) {
    coffeeList += ', ' + name
    console.log(coffeeList)

    setTimeout(function(name) {
      coffeeList += ', ' + name
      console.log(coffeeList)

      setTimeout(function (name) {
        coffeeList += ', ' + name
        console.log(coffeeList)
      }, 500, '카페라뗴')
    }, 500, '카페모카')
  }, 500, '아메리카노')
}, 500, '에스프레소')

/*
에스프레소
에스프레소, 아메리카노
에스프레소, 아메리카노, 카페모카
에스프레소, 아메리카노, 카페모카, 카페라뗴
 */