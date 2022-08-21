var result;

result = /12/.exec('12_34_12');
show(result);

show(result.index);
show(result.input);

show('-------');
result = /12/g.exec('12_34_12');
show(result);


result = /a/i.exec('ABAB');
show(result);


