var result,
    pattern;

var comma = function (value) {
    value = value.toString();
    pattern = /(^[+-]?\d+)(\d{3})/;
    while (pattern.test(value)) {
        value = value.replace(pattern, '$1' + ',' + '$2');
    }
    return value;
}

result = comma(123456789);
show(result);
