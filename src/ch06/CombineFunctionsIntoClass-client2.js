import {acquireReading, baseRate, taxThreshold} from "./CombineFunctionsIntoClass";

const aReading = acquireReading();
const base = baseRate(aReading.month, aReading.year) * aReading.quantity;
export const taxableCharge = Math.max(0, base - taxThreshold(aReading.year));