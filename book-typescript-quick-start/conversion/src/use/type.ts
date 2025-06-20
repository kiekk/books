type myId = string
type myAlias = string | undefined
type User = {
    id: myId
    // ?는 선택적 매개변수
    alias?: myAlias
    city: string
}

let id1: User = { id: 'happy', city: 'seoul' }
let id2: User = { id: 'happy2', alias: undefined, city: 'daegu' }
let id3: User = { id: 'happy3', alias: 'happy3!!', city: 'busan' }

console.log(typeof id1, id1)
console.log(typeof id2, id2)
console.log(typeof id3, id3)

/*
    실행 결과
    object { id: 'happy', city: 'seoul' }
    object { id: 'happy2', alias: undefined, city: 'daegu' }
    object { id: 'happy3', alias: 'happy3!!', city: 'busan' }
 */