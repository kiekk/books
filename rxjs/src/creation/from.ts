import * as Rx from '@reactivex/rxjs'

Rx.Observable.from([1, 2, 3]).subscribe(val => console.log(val))
Rx.Observable.from(new Promise(resolve => resolve('Hello!'))).subscribe(val => console.log(val))

const arr = Array.from([100, 200, 300], x => x + x)
Rx.Observable.from(arr).subscribe(val => console.log(val))

// 1
// 2
// 3
// 200
// 400
// 600
// Hello!
