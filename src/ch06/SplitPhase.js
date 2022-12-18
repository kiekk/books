export function priceOrder(product, quantity, shippingMethod) {
    const basePrice = product.basePrice * quantity;

    const discount = Math.max(quantity - product.discountThreshold, 0)
        * product.basePrice * product.discountRate;

    return applyingShippingCost(basePrice, shippingMethod, quantity, discount);
}

// 배송비 계산 부분 함수 추출
function applyingShippingCost(basePrice, shippingMethod, quantity, discount) {
    const shippingPerCase = (basePrice > shippingMethod.discountThreshold)
        ? shippingMethod.discountedFee : shippingMethod.feePerCase;
    const shippingCost = quantity * shippingPerCase;
    return basePrice - discount + shippingCost;
}
