var reg = new RegExp(/\w/);
var result = reg.exec('123');

show(result);

result = /\w/.exec('123');
show(result);

