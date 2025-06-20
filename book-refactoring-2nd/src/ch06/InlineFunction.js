// 함수 추출의 반대 <-> 함수 인라인
// 불필요하게 함수가 분리되어 있다면 함수 인라인으로 정리
export function rating(aDriver) {
    return aDriver.numberOfLateDeliveries > 5 ? 2 : 1;
}