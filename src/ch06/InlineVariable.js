export function isDeliveryFree(anOrder) {
    let basePrice = (anOrder.basePrice > 1000);
    return basePrice;
}