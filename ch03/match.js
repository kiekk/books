var result;

result = 'Sports'.match(/s/);
for (var i = 0; i < result.length; i++) {
    show(result[i]);
}
show(result.index);
show(result.input);
// 배열 object
console.log('result', result)

function returnValue() {
    return 'method';
}

result = returnValue().match(/met/);
for (var i = 0; i < result.length; i++) {
    show(result[i]);
}


result = 'StringClass'.match('s');
for (var i = 0; i < result.length; i++) {
    show(result[i]);
}

result = 'StringClass'.match('s', 'g');