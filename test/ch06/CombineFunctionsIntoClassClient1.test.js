import {expect} from 'chai';
import {baseCharge} from '../../src/ch06/CombineFunctionsIntoClass-client1'

describe('CombineFunctionsIntoClass-Client1.js', () => {
    it('baseCharge', () => {
        expect(baseCharge).to.equal(1);
    });

});