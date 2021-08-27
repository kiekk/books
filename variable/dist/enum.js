var WeekDay;
(function (WeekDay) {
    WeekDay[WeekDay["Mon"] = 0] = "Mon";
    WeekDay[WeekDay["Tue"] = 1] = "Tue";
    WeekDay[WeekDay["Wed"] = 2] = "Wed";
    WeekDay[WeekDay["Thu"] = 3] = "Thu";
})(WeekDay || (WeekDay = {}));
let thu2 = 10 + 2;
var WeekDay2;
(function (WeekDay2) {
    WeekDay2[WeekDay2["Mon"] = 10] = "Mon";
    WeekDay2[WeekDay2["Tue"] = 11] = "Tue";
    WeekDay2[WeekDay2["Wed"] = 40] = "Wed";
    WeekDay2[WeekDay2["Thu"] = thu2] = "Thu";
})(WeekDay2 || (WeekDay2 = {}));
var WeekDay3;
(function (WeekDay3) {
    WeekDay3["Mon"] = "Monday";
    WeekDay3["Tue"] = "Tuesday";
})(WeekDay3 || (WeekDay3 = {}));
console.log('1. ', JSON.stringify(WeekDay));
console.log('2. ', JSON.stringify(WeekDay2));
console.log('3. ', JSON.stringify(WeekDay3));
console.log(`4. Mon=${WeekDay.Mon}, Tue=${WeekDay['Tue']}, Wed=${WeekDay.Wed}`);
console.log(`5. 0=${WeekDay[0]} 1=${WeekDay[1]} 3=${WeekDay[3]}`);
console.log('6. ', typeof WeekDay);
console.log('7. ', typeof WeekDay.Mon, typeof WeekDay3.Mon);
console.log('8. ', typeof WeekDay[0], WeekDay[0]);
let myDay = 50;
console.log('9. ', typeof myDay, myDay);
let myDay2 = WeekDay3.Mon;
console.log('10. ', typeof myDay2, myDay2);
/*
    실행 결과
    1.  {"0":"Mon","1":"Tue","2":"Wed","3":"Thu","Mon":0,"Tue":1,"Wed":2,"Thu":3}
    2.  {"10":"Mon","11":"Tue","12":"Thu","40":"Wed","Mon":10,"Tue":11,"Wed":40,"Thu":12}
    3.  {"Mon":"Monday","Tue":"Tuesday"}
    4. Mon=0, Tue=1, Wed=2
    5. 0=Mon 1=Tue 3=Thu
    6.  object
    7.  number string
    8.  string Mon
    9.  number 50
    10.  string Monday

    1, 2번 - enum 객체는 속성의 인덱스가 키가 되기도 하고, 속성의 이름값이 되기도 합니다.
    따라서 객체에 할당되는 속성은 enum 객체 속성의 개수보다 2배 더 많습니다.
 */ 
