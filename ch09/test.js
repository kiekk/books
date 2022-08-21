var reg = new RegExp(/\w/);
var result = reg.test('123');

show(result);

result = /\w/.test('123');
show(result);
