class Animal {
    name: number
    constructor(name: string, weight: number) {}
}

class Bird {
    name: number
    constructor(speed: number) {}
}

let animal: Animal = new Animal('happy', 10)
let bird: Bird = new Bird(10)

// 타입 호환 가능 - 멤버 변수가 같기 때문
animal = bird
bird = animal