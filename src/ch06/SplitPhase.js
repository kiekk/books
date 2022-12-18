export function priceOrder(product, quantity, shippingMethod) {
    const priceData = calculatePriceData(product, quantity);

    return applyingShippingCost(priceData, shippingMethod);
}

// 중간 데이터 계산 로직 함수 추출
function calculatePriceData(product, quantity) {
    const basePrice = product.basePrice * quantity;
    const discount = Math.max(quantity - product.discountThreshold, 0)
        * product.basePrice * product.discountRate;

    return {basePrice, quantity, discount};
}

// 배송비 계산 부분 함수 추출
function applyingShippingCost(priceData, shippingMethod) {
    const shippingPerCase = (priceData.basePrice > shippingMethod.discountThreshold)
        ? shippingMethod.discountedFee : shippingMethod.feePerCase;
    const shippingCost = priceData.quantity * shippingPerCase;
    return priceData.basePrice - priceData.discount + shippingCost;
}
