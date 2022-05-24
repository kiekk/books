
// 함수는 그 자체로 독립적인 기능을 수행하는데,
// 메소드는 자신을 호출한 대상 객체에 관한 동작을 수행합니다.

// func는 독립적인 기능을 수행하므로 함수이며,
// obj의 method는 함수를 obj 객체의 method라는 프로퍼티에 값으로 할당했기 때문에 메소드입니다.

// 따라서 func와 obj.method를 호출할 때의 this는 다릅니다.

var func = function (x) {
  console.log(this, x)
}

func(1) // global {...} 1

var obj = {
  method: func
}

obj.method(2) // {method: f } 2