// ES5
for(var i=0;i<2;i++){}
console.log('i : ', i)

// typescript
for (let j: number =0; j<2;j++) {}
// console.log('j : ', j)
// let은 lexical scope로 인해 오류 발생
// Cannot find name 'j'