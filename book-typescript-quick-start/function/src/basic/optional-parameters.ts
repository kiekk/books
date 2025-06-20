function sum2(a: number, b?: number): number {
    // b ?? 0 은
    // b가 undefined, null일 경우 0으로 할당
    return a + (b ?? 0)
}

console.log(sum2(1))
console.log(sum2(1, 2))