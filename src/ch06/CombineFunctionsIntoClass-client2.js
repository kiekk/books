import {acquireReading, baseRate, Reading, taxThreshold} from "./CombineFunctionsIntoClass";

const rawReading = acquireReading();
const aReading = new Reading(rawReading);
const base = baseRate(aReading.month, aReading.year) * aReading.quantity;
export const taxableCharge = Math.max(0, base - taxThreshold(aReading.year));