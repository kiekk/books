var result;

result = 'abcabc'.match(/abc*/);
show(result);

result = 'abcabc'.match(/abc*?/);
show(result);

result = 'abcabc'.match(/abQ*?/);
show(result);


show('-------');
result = 'aaaaa'.match(/a*/);
show(result);

result = 'aaaaa'.match(/a*?/);
show(result);
show(result.index);


show('--------');
result = 'aaaKK'.match(/a*K/);
show(result);

result = 'aaaKK'.match(/a*?K/);
show(result);

/*
수량자 *?: 최소 매치
 */