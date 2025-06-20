"use strict";
exports.__esModule = true;
var jsdom = require("jsdom");
jsdom.env("https://en.wikipedia.org/wiki/Korea", ["http://code.jquery.com/jquery.js"], function (err, window) {
    var $ = window.$;
    $('a').each(function () {
        console.log($(this).text());
    });
});
