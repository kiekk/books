import {acquireReading, Reading, taxThreshold} from "./CombineFunctionsIntoClass";

const rawReading = acquireReading();
const aReading = new Reading(rawReading);
export const taxableCharge = taxableChargeFn(aReading);

// 세금 부과 코드를 함수로 추출
function taxableChargeFn(aReading) {
    return Math.max(0, aReading.baseCharge - taxThreshold(aReading.year));
}