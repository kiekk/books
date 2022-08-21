var result;


result = 'abcd'.match(/[^a]/);
show(result);

result = 'abcde'.match(/[^acd]/);
show(result);

show('-------');
result = '1525'.match(/[^1-2]/);
show(result);

result = '정규표현식'.match(/[^정표]/g);
show(result);

/*
^(caret)을 대괄호 밖에 사용하면 시작 값부터 매칭을 의미하지만,
대괄호 안에 사용하면 not 을 의미한다.

ex) [^a] -> a 가 아닌
 */