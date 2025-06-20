function error(message: string): never {
    throw new Error(message)
}

function fail() {
    return error('Error')
}

fail()