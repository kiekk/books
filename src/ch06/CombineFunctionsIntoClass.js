const reading = {customer: "ivan", quantity: 10, month: 5, year: 2017};

export function acquireReading() {
    return reading;
}

export function baseRate(month, year) {
    if (year === 2017 && month === 5) return 0.1;
    return 0.2;
}

export function taxThreshold(year) {
    return 0.1;
}

// reading 레코드를 클래스로 변환하여 관리
export class Reading {
    constructor(data) {
        this._customer = data.customer;
        this._quantity = data.quantity;
        this._month = data.month;
        this._year = data.year;
    }

    get customer() {
        return this._customer;
    }

    get quantity() {
        return this._quantity;
    }

    get month() {
        return this._month;
    }

    get year() {
        return this._year;
    }

    get baseCharge() {
        return baseRate(this.month, this.year) * this.quantity;
    }
}