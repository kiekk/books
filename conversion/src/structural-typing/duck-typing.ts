class Duck {
    speak() {
        console.log('Quak')
    }
    swim() {
        console.log('Duck swimming')
    }
}

class Goose {
    speak() {
        console.log('Squawk')
    }
    swim() {
        console.log('Goose swimming')
    }
}

function swim(obj) {
    obj.speak()
    obj.swim()
}

let duck = new Duck()
let goose = new Goose()

swim(duck)
swim(goose)

/*
    실행 결과
    Quak
    Duck swimming
    Squawk
    Goose swimming
*/

// 타입 안전성 강화 - interface 선언
/*
interface DuckGoose {
    speak()
    swim()
}

function swim(obj: DuckGoose) {
    obj.speak()
    obj.swim()
}
*/