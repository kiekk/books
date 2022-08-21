var result;


result = '12_34_12'.replace('12', 77);
show(result);

result = '12_34_12'.replace(/12/g, 77);
show(result);

function returnValue() {
    return 'AA';
}

result = '12_34_12'.replace(/12/g, returnValue());
show(result);

