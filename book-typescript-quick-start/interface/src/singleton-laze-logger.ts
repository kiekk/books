class LazeLogger {
    private static uniqueObject: LazeLogger

    private LazeLogger() {}

    public static getLogger(): LazeLogger {
        if(this.uniqueObject == null) {
            this.uniqueObject = new LazeLogger()
        }
        return this.uniqueObject
    }

    public info(message: string) {
        console.log(`[info] ${message}`)
    }

    public warning(message: string) {
        console.log(`[warn] ${message}`)
    }
}

let lazeLogger: LazeLogger = LazeLogger.getLogger()
let lazeLogger2: LazeLogger = LazeLogger.getLogger()
lazeLogger.info('1번 : 정보 log')
lazeLogger.warning('2번 : 경고 log')
lazeLogger.info(`3번 : ${lazeLogger === lazeLogger2}`)

/*
    실행 결과
    [info] 1번 : 정보 log
    [warn] 2번 : 경고 log
    [info] 3번 : true
ㅅ */
