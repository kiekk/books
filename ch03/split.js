var result;

result = '12_34_56'.split('_');
show(result);

result = '12_34_56'.split(/_/);
show(result);

show('-------');
result = '12_34_56'.split('S');
show(result);

result = '12_34_56'.split();
show(result);


show('-------');
result = '12_34_56'.split('');
show(result);


show('-------');
result = '_12_34'.split('_');
show(result);

result = '_12_34'.split(/_/);
show(result);

show('-------');
result = '12A34A56'.split(/(A)/);
show(result);

result = '12A34A56'.split(/A/);
show(result);

result = '12A34A56'.split('A');
show(result);

/*
패턴 문자(splitter)에 괄호() 가 있을 경우 작성한 구분자를 배열 엘리먼트로 설정
 */

show('-------');

result = '12_34_56'.split('_', 2);
show(result);
