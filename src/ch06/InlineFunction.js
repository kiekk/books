export function rating(aDriver) {
    return getRating(aDriver) ? 2 : 1;
}

function getRating(aDriver) {
    return aDriver.numberOfLateDeliveries > 5;
}