class Cube {
    constructor(public width: number, private length: number, protected height: number) {}

    getVolume(): number {
        return this.width * this.length * this.height
    }
}

let [cWidth, cLength, cHeight] = [1, 2, 3]
let cube = new Cube(cWidth, cLength, cHeight)
console.log('1번 세로 :', cube.width, "cm")
console.log('2번 부피 :', cube.getVolume(), "ml")