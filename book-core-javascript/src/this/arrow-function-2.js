// arrow function의 경우 앞서 설명헀듯이
// this binding 작업이 생략되기 때문에
// arrow function 내부에서는 this가 없기 때문에
// 스코프 체인으로 인해 상위 스코프의 this를 참조하게 됩니다.

var obj = {
  outer: function() {
    console.log(this) // { outer: f }

    var innerFunc = () => {
      console.log(this) // { outer: f }
    }

    innerFunc()
  },
}

obj.outer()