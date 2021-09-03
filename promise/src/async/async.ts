function delay(msg) {
    let ms: number = Math.floor(Math.random() * 1000) + 1

    setTimeout(() => {
        console.log(msg)
    }, ms)
}

function async() {
    delay('hello1')
    delay('hello2')
    delay('hello3')
}

async()

// 실행할 때마다 출력 순서 불일치

// 1
// hello2
// hello3
// hello1

// 2
// hello3
// hello1
// hello2

