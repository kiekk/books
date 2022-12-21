import {expect} from 'chai';
import {HeatingPlan, temperatureAlerts} from '../../src/ch11/PreserveWholeObject1'

describe('temperatureAlerts', () => {
    it('should not report alert when room low and high temperature are in range', () => {
        const alerts = temperatureAlerts(
            1,
            2
            , new HeatingPlan(0, 3));

        expect(alerts).to.eql([]);
    });

    it('should report alert when room low temperature is outside range', () => {
        const alerts = temperatureAlerts(
            -1,
            2
            , new HeatingPlan(0, 3));

        expect(alerts).to.eql(["room temperature went outside range"]);
    });

    it('should report alert when room high temperature is outside range', () => {
        const alerts = temperatureAlerts(
            1,
            4
            , new HeatingPlan(0, 3));

        expect(alerts).to.eql(["room temperature went outside range"]);
    });
});