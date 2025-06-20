var result, pattern;

var setLimit = function (digit, dec, negative) {
    var check = digit + '';
    if (dec) {
        check += ('\\.');
    }
    if (negative) {
        check += '-';
    }
    return new RegExp('[' + check + ']');
}

pattern = setLimit('0123456789', true, true);

result = pattern.test('1');
show(result);

result = pattern.test('.');
show(result);

result = pattern.test('a');
show(result);
