class Planet {
    public diameter: number
    protected isTransduction: boolean = true

    getIsTransduction(): boolean {
        return this.isTransduction
    }

    stopTransduction(): void {
        console.log('stop1')
        this.isTransduction = false
    }
}

class Earth extends Planet {
    public features: string[] = ['soil', 'water', 'oxyzen']

    stopTransduction() {
        console.log('stop2')
        this.isTransduction = false
    }
}

let earth: Planet = new Earth()
earth.diameter = 12656.2
console.log(`1번 : ${earth.diameter}`)
console.log(`2번 : ${earth.getIsTransduction()}`)
earth.stopTransduction()
console.log(`3번 : ${earth.getIsTransduction()}`)

/*
    실행 결과
    1번 : 12656.2
    2번 : true
    stop2
    3번 : false
 */