var result;

result = 'abcAAA'.match(/abcs?/);
show(result);


show('-------');
result = 'abcsss'.match(/abcs?/);
show(result);


show('-------');
result = 'abcsssA'.match(/abcs?A/);
show(result);

result = 'abcsssA'.match(/abcs*A/);
show(result);


show('-------');
result = 'abcdABC'.match(/.?AB/);
show(result);

result = ''.match(/.?/);
show(result);

/*
수량자 ?: 없거나 하나만 매치
 */

