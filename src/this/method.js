// 메소드 내부에서의 this는 호출한 객체가 담깁니다.

var obj = {
  methodA: function () {
    console.log(this)
  },
  inner: {
    methodB: function () {
      console.log(this)
    }
  }
}


obj.methodA() // { methodA: f, inner {...}}   == obj

obj['methodA']()  // { methodA: f, inner {...}}   == obj

obj.inner.methodB() // { methodB: f } == obj.inner
obj.inner['methodB']() // { methodB: f } == obj.inner
obj['inner'].methodB() // { methodB: f } == obj.inner
obj['inner']['methodB']() // { methodB: f } == obj.inner

/*
  methodA는 obj가 호출하기 때문에 this는 obj가 되며,
  methodB는 obj의 inner 가 호출하기 때문에 this는 inner가 됩니다.
 */