var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (Object.prototype.hasOwnProperty.call(b, p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        if (typeof b !== "function" && b !== null)
            throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var Bird = /** @class */ (function () {
    function Bird() {
    }
    Bird.prototype.flight = function (kmDistance, kmSpeed) {
        if (kmDistance === void 0) { kmDistance = 0; }
        if (kmSpeed === void 0) { kmSpeed = 0; }
        console.log("\uC0C8\uAC00 " + kmDistance + "km\uB97C " + kmSpeed + "km\uC758 \uC18D\uB3C4\uB85C \uBE44\uD589\uD588\uC2B5\uB2C8\uB2E4.");
    };
    return Bird;
}());
var Eagle = /** @class */ (function (_super) {
    __extends(Eagle, _super);
    function Eagle() {
        return _super.call(this) || this;
    }
    Eagle.prototype.flight = function (kmDistance2) {
        if (kmDistance2 === void 0) { kmDistance2 = 0; }
        console.log("\uB3C5\uC218\uB9AC\uAC00 " + kmDistance2 + "km\uB97C \uBE44\uD589\uD588\uC2B5\uB2C8\uB2E4.");
    };
    return Eagle;
}(Bird));
var bird = new Bird();
bird.flight(1000, 100);
var eagle = new Eagle();
eagle.flight();
eagle.flight(1000);
/*
    실행 결과
    새가 1000km를 100km의 속도로 비행했습니다.
    독수리가 0km를 비행했습니다.
    독수리가 1000km를 비행했습니다.
 */ 
