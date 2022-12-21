export function temperatureAlerts(low, high, aPlan) {
    const alerts = [];
    if (!aPlan.withinRange(low, high)) {
        alerts.push("room temperature went outside range");
    }

    return alerts
}

export class HeatingPlan {
    constructor(low, high) {
        this._low = low;
        this._high = high;
    }

    withinRange(bottom, top) {
        return (bottom >= this._low) && (top <= this._high);
    }
}