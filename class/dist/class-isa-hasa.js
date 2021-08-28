// Flashlight Class
class Flashlight {
    constructor(lightIntensity) {
        this.lightIntensity = lightIntensity;
    }
}
// Bicycle Class
class Bicycle {
    constructor(numberOfWheel) {
        this.numberOfWheel = numberOfWheel;
    }
    getNumberOfWheel() {
        return this.numberOfWheel;
    }
}
// Bicycle Class를 상속 (IS-A)
class MountainBike extends Bicycle {
    constructor(numberOfWheel, hasBackSaddle) {
        super(numberOfWheel);
        this.numberOfWheel = numberOfWheel;
        this.hasBackSaddle = hasBackSaddle;
        // 자전거가 flashlight를 포함함(HAS-A)
        this.flashlight = new Flashlight(90);
    }
    getHasBackSaddle() {
        return this.hasBackSaddle;
    }
    getNumberOfWheel() {
        return this.numberOfWheel;
    }
}
let hasBackSaddle = true;
let numberOfWheel = 2;
let mountainBike = new MountainBike(numberOfWheel, hasBackSaddle);
console.log('자전거 안장 유무 :', mountainBike.getHasBackSaddle());
console.log('자전거 바퀴 개수 :', mountainBike.getNumberOfWheel());
