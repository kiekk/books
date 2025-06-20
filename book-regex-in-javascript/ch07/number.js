var result;

result = 'A123'.match(/\d/);
show(result);


show('-------');
var num = /^\d+$/;
result = 'A123'.match(num);
show(result);

result = '123'.match(num);
show(result);

/*
\d: 숫자만 매치 [0-9]와 동일한 기능
 */