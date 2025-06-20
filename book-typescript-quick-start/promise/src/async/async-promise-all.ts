function asyncDelay(order: number) {
    return new Promise((resolve, reject) => {
        let ms: number = Math.floor(Math.random() * 1000) + 1

        setTimeout(() => {
            console.log(`작업 완료 : ${order}`)
            resolve(order)
        }, ms)
    }).then()
}

function syncResultPromise() {
    let p1 = asyncDelay(1)
    let p2 = asyncDelay(2)
    let p3 = asyncDelay(3)
    let p4 = asyncDelay(4)

    Promise.all([p1, p2, p3, p4]).then((order) => {
      console.log(`동기화된 출력 : ${order}`)
    })
}

syncResultPromise()

// 작업 완료 : 4
// 작업 완료 : 3
// 작업 완료 : 1
// 작업 완료 : 2
// 동기화된 출력 : 1,2,3,4
