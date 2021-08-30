/// <reference path="car1.ts" />
var Car;
(function (Car) {
    var wheels;
    console.log(Car.auto);
    var Taxi = /** @class */ (function () {
        function Taxi() {
        }
        return Taxi;
    }());
})(Car || (Car = {}));
console.log(Car.auto);
