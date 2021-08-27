let number = 50;
console.log(typeof number, number.toFixed(1));
let number2 = 50;
console.log(typeof number2);
// console.log(typeof number2, number2.toFixed(1))
// 오류 발생
// TS2339: Property 'toFixed' does not exist on type 'Object'.
let number3 = 50;
console.log(typeof number3, number3.toFixed(1));
let number4 = 'happy';
console.log(typeof number4, number4.charAt(0));
