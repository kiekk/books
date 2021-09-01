function neverTouch2(): never {
    //TS2534: A function returning 'never' cannot have a reachable end point
    console.log('test')
}

neverTouch2()