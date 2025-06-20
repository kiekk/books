"use strict";
exports.__esModule = true;
var export_variables_1 = require("./export-variables");
console.log("API Version : " + export_variables_1.ver);
console.log("API Name : " + export_variables_1.author);
export_variables_1.extensions.forEach(function (s) {
    console.log("extensions : " + s);
});
console.log("display() : " + (0, export_variables_1.display)());
