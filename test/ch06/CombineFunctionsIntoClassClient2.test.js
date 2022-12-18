import {expect} from 'chai';
import {taxableCharge} from '../../src/ch06/CombineFunctionsIntoClass-client2'

describe('CombineFunctionsIntoClass-Client2.js', () => {
    it('taxableCharge', () => {
        expect(taxableCharge).to.equal(0.9);
    });

});