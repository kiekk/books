var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
let _a = { a: 10, b: 20, c: 30, d: 40 }, { a, b } = _a, c = __rest(_a, ["a", "b"]);
console.log(a);
console.log(b);
console.log(c);
let test = { a: 10, b: 20, c: 30, d: 40 };
let test2 = Object.assign(Object.assign({}, test), { b: 25 });
// b가 spread-operator인 test에 존재한다면 값을 덮어 씌움
console.log(test);
console.log(test2);
