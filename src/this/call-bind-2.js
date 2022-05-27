var obj = {
  logThis: function () {
    console.log(this)
  },
  logThisLater1: function () {
    setTimeout(this.logThis, 500)
  },
  logThisLater2: function () {
    setTimeout(this.logThis.bind(this), 1000)
  },
}

obj.logThisLater1() // global {...}
obj.logThisLater2() // obj