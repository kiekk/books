const basicRejectPromise = new Promise((resolve, reject) => {
    // reject로 예외 발생
    reject('Error')
}).catch((err) => {
    // 예외 처리
    console.log(err)
})