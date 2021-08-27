var testUndefined;
console.log(testUndefined, typeof testUndefined);
var testUndefined2 = undefined;
console.log(testUndefined2, typeof testUndefined2);
var testNull = null;
console.log(testNull, typeof testNull);
/*
    실행 결과
    undefined undefined
    undefined undefined
    null object

    undefined는 선언은 됐지만 값이 할당되지 않았고,
    null은 값은 할당 됐지만 값이 없습니다.
 */ 
