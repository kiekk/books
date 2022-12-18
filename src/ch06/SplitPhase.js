export function priceOrder(product, quantity, shippingMethod) {
    const basePrice = product.basePrice * quantity;

    const discount = Math.max(quantity - product.discountThreshold, 0)
        * product.basePrice * product.discountRate;

    const priceData = {basePrice, quantity, discount};

    return applyingShippingCost(priceData, shippingMethod);
}

// 배송비 계산 부분 함수 추출
function applyingShippingCost(priceData, shippingMethod) {
    const shippingPerCase = (priceData.basePrice > shippingMethod.discountThreshold)
        ? shippingMethod.discountedFee : shippingMethod.feePerCase;
    const shippingCost = priceData.quantity * shippingPerCase;
    return priceData.basePrice - priceData.discount + shippingCost;
}
