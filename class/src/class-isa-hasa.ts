// Flashlight Class
class Flashlight {
    constructor(public lightIntensity) {}
}

// Bicycle Class
class Bicycle {
    constructor(public numberOfWheel: number) {}

    getNumberOfWheel(): number {
        return this.numberOfWheel
    }
}

// Bicycle Class를 상속 (IS-A)
class MountainBike extends Bicycle {
    flashlight: Flashlight

    constructor(public numberOfWheel: number, public hasBackSaddle: boolean) {
        super(numberOfWheel);

        // 자전거가 flashlight를 포함함(HAS-A)
        this.flashlight = new Flashlight(90)
    }

    getHasBackSaddle(): boolean {
        return this.hasBackSaddle
    }

    getNumberOfWheel(): number {
        return this.numberOfWheel
    }
}

let hasBackSaddle = true
let numberOfWheel = 2
let mountainBike = new MountainBike(numberOfWheel, hasBackSaddle)

console.log('자전거 안장 유무 :', mountainBike.getHasBackSaddle())
console.log('자전거 바퀴 개수 :', mountainBike.getNumberOfWheel())