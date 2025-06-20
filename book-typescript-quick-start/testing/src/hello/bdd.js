"use strict";
exports.__esModule = true;
var hello_1 = require("./hello");
var assert = require('assert');
describe('동일 숫자인지 테스트', function () {
    before(function () {
        // 테스트 실행 전 한 번만 실행
        console.log('before() 호출');
    });
    after(function () {
        // 테스트 종료 후 한 번만 실행
        console.log('after() 호출');
    });
    beforeEach(function () {
        // 각 테스트 실행 전 실행
        console.log('beforeEach() 호출');
    });
    afterEach(function () {
        // 각 테스트 종료 후 실행
        console.log('afterEach() 호출');
    });
    describe('hello()', function () {
        it('1과 동일한지', function () {
            assert.equal(1, hello_1["default"](1));
        });
        it('2과 동일한지', function () {
            assert.equal(2, hello_1["default"](2));
        });
        it('3과 동일한지', function () {
            assert.equal(3, hello_1["default"](3));
        });
    });
});
