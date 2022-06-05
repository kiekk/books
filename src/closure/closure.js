var outer = function() {
  var a = 1
  var inner = function() {
    console.log(++a)
  }
  inner()
}

outer() // 2

/*
outer의 내부 함수인 inner에서 변수 a의 값을 1 증가시킨 후 출력합니다.

따라서 outer()를 호출하면 2가 출력되고, outer 실행 컨텍스트가 종료되면
a, inner에 대한 참조를 지워 가비지 컬렉션의 대상이 됩니다.

 */