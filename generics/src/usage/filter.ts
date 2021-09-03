interface IFilter<T> {
    unique(array: Array<T>): Array<T>
}

class Filter<T> implements IFilter<T> {
    unique(array: Array<T>): Array<T> {
        return array.filter((v, i, array) => array.indexOf(v) === i)
    }
}

// T가 string type으로 정의됨
let myFilter = new Filter<string>()
let resultFilter = myFilter.unique(['a', 'b', 'c', 'd', 'a', 'b'])
console.log(resultFilter)

/*
    실행 결과
    [ 'a', 'b', 'c', 'd' ]
 */