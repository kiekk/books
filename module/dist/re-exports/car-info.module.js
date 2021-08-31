import { Mycar as SuperCar, SuperEngine } from "./my-car";
export var CarInfo;
(function (CarInfo) {
    CarInfo.car = SuperCar;
    CarInfo.engine = SuperEngine;
    function Hello() {
        console.log('Hello');
    }
    CarInfo.Hello = Hello;
})(CarInfo || (CarInfo = {}));
