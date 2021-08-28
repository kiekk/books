let numberList = [1, 2, 3, 4, 5]
numberList = numberList.filter(n => {
    return n % 2 === 0
})

console.log(numberList)