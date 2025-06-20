import * as mocha from 'mocha'
import hello from './hello'

var assert = require('assert')

suite('동일 숫자인지 테스트', () => {
    setup(() => {
        // 객체 생성, 변수 초기화
    })

    suite('hello()', () => {
        test('1과 동일한지', () => {
            assert.equal(1, hello(1))
        })

        test('2과 동일한지', () => {
            assert.equal(2, hello(2))
        })

        test('3과 동일한지', () => {
            assert.equal(3, hello(3))
        })
    })
})