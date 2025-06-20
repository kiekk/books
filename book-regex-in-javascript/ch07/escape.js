var result;

result = '^ABC'.match(/^A/);
show(result);

result = 'B^AC'.match(/\^A/);
show(result);

show('-------');
result = '\\ab'.match(/\\/);
show(result);

result = '\\^'.match(/\\\^/);
show(result);


show('-------');
result = new RegExp('\^A').exec('ABC');
show(result);


result = new RegExp('\\^B').exec('A^BC');
show(result);

/*
이스케이프 시퀀스
이스케이프: \
시퀀스: 문자

ex) \a, \A

이스케이프 다음에 숫자가 올 경우 이스케이프 숫자 시퀀스라고 합니다.

ex) \2, \3

이스케이프를 문자 앞에 작성할 경우 패턴 문자가 일반 문자로 인식
 */