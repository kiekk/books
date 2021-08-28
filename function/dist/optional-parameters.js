function sum2(a, b) {
    // b ?? 0 은
    // b가 undefined, null일 경우 0으로 할당
    return a + (b !== null && b !== void 0 ? b : 0);
}
console.log(sum2(1));
console.log(sum2(1, 2));
