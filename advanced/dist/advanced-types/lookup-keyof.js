let pValue1 = "name";
// let pValue2: Profile1 = "name2"  // 에러
let pValue3 = "length";
let pValue4 = 'push';
let pValue5 = 'hello';
let pValue6 = 'length';
// let pValue7: Profile4 = 'abc'    // 에러
console.log(`
    1번 : ${pValue1} / 2번 : 오류 / 3번 : ${pValue3}
    4번 : ${pValue4} / 5번 : ${pValue5} / 6번 : ${pValue6} / 7번 : 오류
`);
/*
    실행 결과
    1번 : name / 2번 : 오류 / 3번 : length
    4번 : push / 5번 : hello / 6번 : length / 7번 : 오류
*/
