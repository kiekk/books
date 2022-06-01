new Promise(function(resolve) {
  setTimeout(function() {
    var name = '에스프레소'
    console.log(name)
    resolve(name)
  }, 500)
}).then(function(prevName) {
  return new Promise(function(resolve) {
    setTimeout(function() {
      var name = prevName + ', 아메리카노'
      console.log(name)
      resolve(name)
    }, 500)
  })
}).then(function(prevName) {
  return new Promise(function(resolve) {
    setTimeout(function() {
      var name = prevName + ', 카페모카'
      console.log(name)
      resolve(name)
    }, 500)
  })
}).then(function(prevName) {
  return new Promise(function(resolve) {
    setTimeout(function() {
      var name = prevName + ', 카페라떼'
      console.log(name)
      resolve(name)
    }, 500)
  })
})

/*
에스프레소
에스프레소, 아메리카노
에스프레소, 아메리카노, 카페모카
에스프레소, 아메리카노, 카페모카, 카페라뗴
 */