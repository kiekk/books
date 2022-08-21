var result, isAlpha;

isAlpha = function (value) {
    var pattern = /^[a-zA-Z]+$/;
    return pattern.test(value);
}

result = isAlpha('abcDE');
show(result);
