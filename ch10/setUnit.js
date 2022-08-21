var result, setUnits;

setUnits = function (value) {
    pattern = /^\d+$/;
    return pattern.test(value) ? value + 'px' : value;
}

result = setUnits(12);
show(result);

result = setUnits('16em');
show(result);
