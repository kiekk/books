var newArr2 = [10, 20, 30].map(function (index, currentValue) {
  console.log(currentValue, index)
  return currentValue + 5
})

console.log(newArr2)
/*
0 10
1 20
2 30
[ 5, 6, 7 ]
 */

/*
이번에는 callback 함수에 인자를 바꿔서 실행해보면
의도한 것과 다르게 동작할 것입니다.

index, currentValue 는 개발자가 명명한 것일 뿐
실제로는 순서에 의해 파라미터가 바인딩 됩니다.

map 메소드는 첫 번째 인자로 배열의 요소, 두 번째 인자로 인덱스가 바인딩 되기 때문입니다.

이를 통해서 알 수 있는 것은 map 메소드에 정의 된 규칙에 맞춰야 한다는 것이며,
callback 함수를 실행하는 주체가 map 메소드이기 때문에 map 메소드가 callback 함수의
제어권을 가졌다고 할 수 있습니다.

따라서 callback 함수에 인자를 전달할 때에도 map 메소드가 정해놓은 규칙을 지켜야 합니다.
 */