let text: string = ''
let statusActive: number = 0
let isEnabled: boolean = true

// first if
if(statusActive || text) {
    console.log('1')
}

// second if
if(isEnabled && 2 > 1) {
    console.log('2')
}