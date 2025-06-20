// 1
var Student = /** @class */ (function () {
    function Student() {
    }
    return Student;
}());
var student = new Student();
// Setter
student.name = 'happy';
student.birthYear = 2017;
// Getter
console.log(student.name);
console.log(student.birthYear);
// 2
var Student2 = /** @class */ (function () {
    function Student2() {
    }
    Object.defineProperty(Student2.prototype, "name", {
        get: function () {
            return this.studentName;
        },
        set: function (name) {
            if (name.indexOf('happy') !== 0) {
                this.studentName = name;
            }
        },
        enumerable: false,
        configurable: true
    });
    Object.defineProperty(Student2.prototype, "birthYear", {
        get: function () {
            return this.studentBirthYear;
        },
        set: function (year) {
            if (year <= 2000) {
                this.studentBirthYear = year;
            }
        },
        enumerable: false,
        configurable: true
    });
    return Student2;
}());
var student2 = new Student2();
student2.birthYear = 2001;
console.log("1\uBC88 : " + student2.birthYear);
student2.birthYear = 2000;
console.log("2\uBC88 : " + student2.birthYear);
student2.name = 'happy';
console.log("3\uBC88 : " + student2.name);
student2.name = 'lucky';
console.log("4\uBC88 : " + student2.name);
