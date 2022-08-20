var result;

result = 'JavaScript'.match(/s/i);
show(result);


var regexp = new RegExp('s', 'i');
result = regexp.test('JavaScript');
show(result);

/*
정규 표현식 flag

i(ignore): 대소문자를 구분하지 않음
= RegExp.prototype.ignoreCase (en-US)
 */