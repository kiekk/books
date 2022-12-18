// 변수 추출의 반대 <-> 인라인 변수
// 불필요한 변수가 있다면 인라인하여 변수를 제거한다.
export function isDeliveryFree(anOrder) {
    return (anOrder.basePrice > 1000);
}