"use strict";
exports.__esModule = true;
var hello_1 = require("./hello");
var assert = require('assert');
suite('동일 숫자인지 테스트', function () {
    setup(function () {
        // 객체 생성, 변수 초기화
    });
    suite('hello()', function () {
        test('1과 동일한지', function () {
            assert.equal(1, hello_1["default"](1));
        });
        test('2과 동일한지', function () {
            assert.equal(2, hello_1["default"](2));
        });
        test('3과 동일한지', function () {
            assert.equal(3, hello_1["default"](3));
        });
    });
});
