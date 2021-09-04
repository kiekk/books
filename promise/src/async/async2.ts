function delay2(msg: string) {
    let ms: number = Math.floor(Math.random() * 1000) + 1
    return new Promise((resolve) => {
        setTimeout(resolve, ms, msg)
    }).then((v) => {
        console.log(v, `${ms}ms`)
    })
}

async function sync2() {
    let start = new Date().getTime()

    await delay2('a')
    await delay2('b')
    await delay2('c')

    let end = new Date().getTime()
    console.log(`시간 : ${end - start}ms`)
}

sync2()

// 비동기 호출이지만 동기화 처리를 했기 때문에 순차적으로 호출
// a 253ms
// b 614ms
// c 330ms
// 시간 : 1226ms
