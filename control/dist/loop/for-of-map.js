let itMap = new Map([['one', 1], ['one', 2]]);
itMap.set('one', 3);
for (let entry of itMap) {
    console.log(entry);
}
console.log(itMap.get('one'));
/*
    실행 결과
    [ 'one', 3 ]
    3
 */ 
