class EagerLogger {
    EagerLogger() { }
    static getLogger() {
        return this.uniqueObject;
    }
    info(message) {
        console.log(`[info] ${message}`);
    }
    warning(message) {
        console.log(`[warn] ${message}`);
    }
}
// Eager Initialization
EagerLogger.uniqueObject = new EagerLogger();
let eagerLogger = EagerLogger.getLogger();
let eagerLogger2 = EagerLogger.getLogger();
eagerLogger.info('1번 : 정보 log');
eagerLogger.warning('2번 : 경고 log');
eagerLogger.info(`3번 : ${eagerLogger === eagerLogger2}`);
/*
    실행 결과
    [info] 1번 : 정보 log
    [warn] 2번 : 경고 log
    [info] 3번 : true
 */ 
