interface Person {
    name: string
}

class Employee {
    name: string
}

let person: Person
person = new Employee() // 타입 호환 가능

// 구조 타이핑의 경우 서로 관계가 없더라도 구조만 같다면 호환이 가능합니다.