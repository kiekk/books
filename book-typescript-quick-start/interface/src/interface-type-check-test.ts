interface ICar {
    name: string
}

class MyCar  {}

let mCar: ICar = {
    name: 'car'
}

console.log(typeof mCar)
// object
// interface는 컴파일 시 타입 검사의 용도로 사용하고
// 컴파일 후에는 제거되기 때문에 typeof로 interface의 타입을 확인할 수 없습니다.

console.log(typeof MyCar)   // function
// console.log(typeof ICar)    // 유효하지 않음