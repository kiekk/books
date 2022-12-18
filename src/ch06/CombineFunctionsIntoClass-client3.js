import {acquireReading, baseRate} from "./CombineFunctionsIntoClass";

const aReading = acquireReading();

export const basicChargeAmount = calculateBaseCharge(aReading);

function calculateBaseCharge(aReading) {
    return baseRate(aReading.month, aReading.year) * aReading.quantity;
}