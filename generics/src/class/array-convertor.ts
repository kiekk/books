class ArrayConvertor<T> {
    elements: Array<T>
    constructor(elms: Array<T>) {
        this.elements = elms
    }

    array2String(): string {
        let text = ''
        for (let i=0; i < this.elements.length; i++) {
            if(i > 0) text += ' '
            text += this.elements[i].toString()
        }

        return text
    }

    getValue(elms: Array<T>, index: number): T {
        return elms[index]
    }
}

let arr = [1, 2]
// var arr = [1, 2, 'hello']    // 문자열을 추가할 수 없음

let numConvertor = new ArrayConvertor<number>(arr)

console.log(numConvertor.array2String())
console.log(numConvertor.getValue(arr, 0))

let arr2 = new Array<string>()

arr2.push('a')
arr2.push('b')
// arr2.push(1234)  // 숫자는 추가할 수 없음
let stringConvertor = new ArrayConvertor<string>(arr2)

console.log(stringConvertor.array2String())
console.log(stringConvertor.getValue(arr2, 0))

/*
    실행 결과
    1 2
    1
    a b
    a
*/