function neverTest(value: string | number) {
    if(typeof value === 'string') {
        return value
    } else if(typeof value === 'number') {
        return value
    } else {
        return value // Unreachable code
    }
}

console.log(neverTest('test'))