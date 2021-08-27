type C = { a: string, b?: number }

function fruit({ a, b }: C): void {
    console.log(a, b)
}

fruit({ a: 'apple', b: 10 })
fruit({ a: 'apple' })

/*
    실행 결과
    apple 10
    apple undefined
 */