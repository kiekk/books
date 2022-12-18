// 변수 인라인의 반대 <-> 변수 추출
// 가독성 / 디버깅 향상
export function price(order) {
    // 각 단계별로 변수로 추출
    let basePrice = order.quantity * order.itemPrice;
    let quantityDiscount = Math.max(0, order.quantity - 500) * order.itemPrice * 0.05;
    let shippingCost = Math.min(basePrice * 0.1, 100);
    return basePrice - quantityDiscount + shippingCost;
}