var result;

result = '12_34_12'.search(/34$/);
show(result);

result = '12_34_12'.search(/12$/);
show(result);

show('-------');
var value = 'ccStart\u000aStart\u000aStart';

result = value.search(/Start$/);
show(result);

result = value.search(/Start$/m);
show(result);

