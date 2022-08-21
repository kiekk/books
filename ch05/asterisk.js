var result;

result = 'aaaaaC'.match(/a*/);
show(result);

result = 'aaaaaC'.match(/K*/);
show(result);
show(result.index);


show('-------');
result = '12ab_12efg'.match(/12c*/g);
show(result);

result = '123ab_12efg'.match(/123c*/g);
show(result);

result = 'abc_123'.match(/123c*/g);
show(result);


show('-------');
result = '12A 12c'.match(/12c*/g);
show(result);


show('-------');
result = 'CaaBaaa'.match(/a+/);
show(result);

result = 'CaaBaaa'.match(/a*/);
show(result);
show(result.index);


show('-------');
result = 'abcde'.match(/.*/);
show(result);

result = 'abcdABC'.match(/.*AB/);
show(result);

result = ''.match(/.*/);
show(result);


show('-------');
result = 'BaaCaaa'.match(/a*/g);
show(result);
show(result.length);

/*
수량자 *: 없거나 하나 이상에 매치
 */

