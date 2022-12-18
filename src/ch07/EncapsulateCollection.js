// collection 캡슐화 적용 후
// 컬렉션 요소를 조작할 수 있는 setter 를 제거
// 컬렉션 추가 / 삭제 할 수 있는 별도의 메소드를 제공
export class Person {
    constructor(name) {
        this._name = name;
        this._courses = [];
    }

    get name() {
        return this._name;
    }

    get courses() {
        return this._courses.slice();
    }

    addCourse(course) {
        this._courses.push(course);
    }

    removeCourse(aCourse, fnIfAbsent = () => {
        throw new RangeError();
    }) {
        const index = this._courses.indexOf(aCourse);
        if (index === -1) fnIfAbsent();
        else this._courses.splice(index, 1);
    }

}

export class Course {
    constructor(name, isAdvanced) {
        this._name = name;
        this._isAdvanced = isAdvanced;
    }

    get name() {
        return this._name;
    }

    get isAdvanced() {
        return this._isAdvanced;
    }
}