interface ICount {
    readonly count: number
}

class TestReadonly implements ICount {
    readonly count: number
    static readonly count2: number
    private readonly count3: number
    readonly count4: number = 0
    getCount() {
        // this.count4 = 0 // readonly로 선언된 변수는 재할당 불가
        // readonly count5: number = 0 // readonly는 메서드에 선언할 수 없음
    }
}

function getCount() {
    // readonly count: number   // readonly는 함수에 선언할 수 없음
}

// readonly는 객체 리터럴의 속성 앞에 사용 가능
let literalObj: { readonly alias: string } = { alias: 'happy' }
// literalObj.name = 'happy'    // readonly로 지정된 타입으로 인해 할당 불가
// literalObj = 'test'  // readonly로 지정된 타입으로 인해 할당 불가
