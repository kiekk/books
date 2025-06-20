var result;

result = 'aaaaa'.match(/a{2,4}/);
show(result);

result = 'aaa'.match(/a{2,4}/);
show(result);


show('-------');
result = 'ccc'.match(/a{0,2}/);
show(result);

/*
{숫자, 숫자} 로 개수 범위 지정
 */