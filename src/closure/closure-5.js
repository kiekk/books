(function() {
  var a = 0
  var intervalId = null
  var inner = function() {
    if (++a >= 10) {
      clearInterval(intervalId)
      inner = null
    }
    console.log(a)
  }
  intervalId = setInterval(inner, 1000)
})()
