var result;


result = 'aaa'.match(/a{2}/);
show(result);

result = 'aaa'.match(/a{4}/);
show(result);


show('-------');
result = 'aaaKK'.match(/a{2}K/);
show(result);

