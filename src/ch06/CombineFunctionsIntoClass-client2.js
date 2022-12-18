import {acquireReading, Reading, taxThreshold} from "./CombineFunctionsIntoClass";

const rawReading = acquireReading();
const aReading = new Reading(rawReading);
export const taxableCharge = Math.max(0, aReading.baseCharge - taxThreshold(aReading.year));