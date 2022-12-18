// collection 캡슐화 적용 전
// 컬렉션인 courses 를 그대로 반환하며,
// 세터를 사용해 외부에서 courses 를 조작할 수 있다.
export class Person {
    constructor(name) {
        this._name = name;
        this._courses = [];
    }

    get name() {
        return this._name;
    }

    get courses() {
        return this._courses;
    }

    set courses(courses) {
        return this._courses = courses;
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