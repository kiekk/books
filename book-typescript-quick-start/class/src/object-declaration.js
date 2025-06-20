var Rectangle = /** @class */ (function () {
    function Rectangle(x, y) {
        this.x = x;
        this.y = y;
    }
    Rectangle.prototype.getArea = function () {
        return this.x * this.y;
    };
    return Rectangle;
}());
var rectangle = new Rectangle(1, 5);
var area = rectangle.getArea();
console.log(area);
