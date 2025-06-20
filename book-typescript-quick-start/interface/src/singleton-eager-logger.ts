class EagerLogger {
    // Eager Initialization
    private static uniqueObject: EagerLogger = new EagerLogger()

    private EagerLogger() {}

    public static getLogger(): EagerLogger {
        return this.uniqueObject
    }

    public info(message: string) {
        console.log(`[info] ${message}`)
    }

    public warning(message: string) {
        console.log(`[warn] ${message}`)
    }
}

let eagerLogger: EagerLogger = EagerLogger.getLogger()
let eagerLogger2: EagerLogger = EagerLogger.getLogger()

eagerLogger.info('1번 : 정보 log')
eagerLogger.warning('2번 : 경고 log')
eagerLogger.info(`3번 : ${eagerLogger === eagerLogger2}`)

/*
    실행 결과
    [info] 1번 : 정보 log
    [warn] 2번 : 경고 log
    [info] 3번 : true
 */