import {expect} from 'chai';
import {NumberRange, readingsOutsideRange} from '../../src/ch06/IntroduceParameterObject'

const station = {
    name: "ZB1",
    readings: [
        {temp: 47, time: "2016-11-10 09:10"},
        {temp: 53, time: "2016-11-10 09:20"},
        {temp: 58, time: "2016-11-10 09:30"},
        {temp: 53, time: "2016-11-10 09:40"},
        {temp: 51, time: "2016-11-10 09:50"},
    ]
};

const operationPlan = {
    temperatureFloor: 51,
    temperatureCeiling: 53
};

describe('readingsOutsideRange', () => {
    it('give the reading outside temperature outside range', () => {
        const range = new NumberRange(operationPlan.temperatureFloor, operationPlan.temperatureCeiling);

        // 매개변수를 객체화 하여 매개변수 개수를 최소화 한다.
        // 이 때 관심사가 같은 매개변수를 하나의 클래스로 묶어 관리한다.
        let alerts = readingsOutsideRange(station, range);

        expect(alerts).to.eql([{temp: 47, time: "2016-11-10 09:10"}, {temp: 58, time: "2016-11-10 09:30"}]);
    });

});