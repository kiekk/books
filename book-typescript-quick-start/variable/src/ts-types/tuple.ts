let x: [string, number] = ['tuple', 100]
console.log(typeof x, typeof x[0], typeof x[1])
console.log(x[0].substr(0, 2), x[1].toFixed(2))

// 튜플에 선언된 타입 수와 할당될 배열의 요소 수가 정확히 일치해야 합니다.

/*
    실행 결과
    object string number
    tu 100.00
 */