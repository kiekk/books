let chainingPromise = new Promise((resolve, reject) => {
    resolve(1)
}).then((value) => {
    console.log(value)
    return 2
}).then((value) => {
    console.log(value)
    throw 'Exception'
}).catch((e) => {
    console.log(e)
})

// 1
// 2
// Exception
