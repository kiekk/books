abstract class AbstractBird {
    abstract birdName: string
    abstract habitat: string

    abstract flySound(sound: string)

    fly(): void {
        this.flySound('파닥파닥')
    }

    getHabitat(): void {
        console.log(`<${this.birdName}>의 서식지는 <${this.habitat}> 입니다.`)
    }
}

class WildGoose extends AbstractBird {
    constructor(public birdName: string, public habitat: string) {
        super();
    }

    flySound(sound: string) {
        console.log(`<${this.birdName}>가 <${sound}> 날아갑니다.`)
    }
}

let wildGoose = new WildGoose('기러기', '순천만 갈대밭')
wildGoose.fly()
wildGoose.getHabitat()

/*
    실행 결과
    <기러기>가 <파닥파닥> 날아갑니다.
    <기러기>의 서식지는 <순천만 갈대밭> 입니다.
 */