function delay3(msg, ms): Promise<any> {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve(msg)
        }, ms)
    }).then((v) => {
        console.log(`${v} ${ms}ms`)
        return v
    })
}

async function sync3() {
    let start = new Date().getTime()

    let result1: Promise<any> = await delay3('a', 1000)
    let result2: Promise<any> = await delay3('b', 500)
    let result3: Promise<any> = await delay3('c', 100)

    let end = new Date().getTime()
    console.log(`시간 : ${end - start}ms`)
}

sync3()