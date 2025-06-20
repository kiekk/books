class TestReadonly {
    constructor() {
        this.count4 = 0;
    }
    getCount() {
        // this.count4 = 0 // readonly로 선언된 변수는 재할당 불가
        // readonly count5: number = 0 // readonly는 메서드에 선언할 수 없음
    }
}
function getCount() {
    // readonly count: number   // readonly는 함수에 선언할 수 없음
}
// readonly는 객체 리터럴의 속성 앞에 사용 가능
let literalObj = { alias: 'happy' };
// literalObj.name = 'happy'    // readonly로 지정된 타입으로 인해 할당 불가
// literalObj = 'test'  // readonly로 지정된 타입으로 인해 할당 불가
