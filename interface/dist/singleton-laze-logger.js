class LazeLogger {
    LazeLogger() { }
    static getLogger() {
        if (this.uniqueObject == null) {
            this.uniqueObject = new LazeLogger();
        }
        return this.uniqueObject;
    }
    info(message) {
        console.log(`[info] ${message}`);
    }
    warning(message) {
        console.log(`[warn] ${message}`);
    }
}
let lazeLogger = LazeLogger.getLogger();
let lazeLogger2 = LazeLogger.getLogger();
lazeLogger.info('1번 : 정보 log');
lazeLogger.warning('2번 : 경고 log');
lazeLogger.info(`3번 : ${lazeLogger === lazeLogger2}`);
/*
    실행 결과
    [info] 1번 : 정보 log
    [warn] 2번 : 경고 log
    [info] 3번 : true
ㅅ */
