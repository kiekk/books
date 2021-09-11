import * as mocha from 'mocha'

import * as chai from 'chai'
import hello from './hello'

describe('hello 테스트', () => {
    it('동일 문자를 반환하는지 테스트', () => {
        chai.expect(hello('world')).to.be.equal('world')
    })
})