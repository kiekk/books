var result;

var value = 'aaaaac';
result = value.match(/a/);
show(result);

result = value.match(/a+/);
show(result);

show('-------');
result = 'aab aac'.match(/a/g);
show(result);

result = 'aab aac'.match(/a+/g);
show(result);

show('-------');
result = 'aab aac'.match(/K+/);
show(result);

show('-------');
result = 'abcdefg'.match(/.+/);
show(result);

show('-------');
result = 'abcABC'.match(/.+A/);
show(result);

show('-------');
result = 'abcABC'.match(/.+AB/);
show(result);

/*
수량자 +: 하나 이상에 매치
 */