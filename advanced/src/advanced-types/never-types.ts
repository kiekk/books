const neverTouch = function(): never {
    while(true) {
        console.log('Never')
    }

    // console.log()   // Unreachable code
}

let resultNever: never = neverTouch()