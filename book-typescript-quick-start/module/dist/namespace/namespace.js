var MyInfo1;
(function (MyInfo1) {
    MyInfo1.name = 'happy1';
    function getName2() {
        return MyInfo2.name2;
    }
    MyInfo1.getName2 = getName2;
})(MyInfo1 || (MyInfo1 = {}));
var MyInfo2;
(function (MyInfo2) {
    MyInfo2.name2 = 'happy2';
    function getName() {
        return MyInfo1.name;
    }
    MyInfo2.getName = getName;
})(MyInfo2 || (MyInfo2 = {}));
console.log(MyInfo1.getName2());
console.log(MyInfo2.getName());
/*
    MyInfo1, MyInfo2는 컴파일시 var 선언자로 선언되어 호이스팅 되므로
    실행 순서와 상관없이 함수 영역간 호출이 가능합니다.
 */ 
