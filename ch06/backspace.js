var result;

result = '2** '.match(/2\b/);
show(result);


result = /[\b]/.test('\u0008');
show(result);

/*
특수 문자 \b: 63개 이외 문자에 매치
[] 대괄호 안에 작성할 경우 백스페이스 값으로 인식

 */