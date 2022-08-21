var result;

result = 'aaaaa'.match(/a{1,}/);
show(result);

result = 'aaaaa'.match(/a{1,}?/);
show(result);


show('--------');
result = 'aaaaa'.match(/a{1,5}/);
show(result);

result = 'aaaaa'.match(/a{1,5}?/);
show(result);

