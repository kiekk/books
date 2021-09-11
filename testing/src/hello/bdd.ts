import * as mocha from 'mocha'
import hello from './hello'

var assert = require('assert')

describe('동일 숫자인지 테스트', () => {
    before(() => {
        // 테스트 실행 전 한 번만 실행
        console.log('before() 호출')
    })

    after(() => {
      // 테스트 종료 후 한 번만 실행
      console.log('after() 호출')
    })

    beforeEach(() => {
        // 각 테스트 실행 전 실행
        console.log('beforeEach() 호출')
    })

    afterEach(() => {
        // 각 테스트 종료 후 실행
        console.log('afterEach() 호출')
    })

    describe('hello()', () => {
        it('1과 동일한지', () => {
            assert.equal(1, hello(1))
        })

        it('2과 동일한지', () => {
            assert.equal(2, hello(2))
        })

        it('3과 동일한지', () => {
            assert.equal(3, hello(3))
        })
    })
})