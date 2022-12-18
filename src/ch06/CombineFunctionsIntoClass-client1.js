import {acquireReading, baseRate, Reading} from "./CombineFunctionsIntoClass";

const rawReading = acquireReading();
const aReading = new Reading(rawReading);

export const baseCharge = baseRate(aReading.month, aReading.year) * aReading.quantity;