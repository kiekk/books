var result;

result = 'sports'.match(/.s/);
show(result);

result = 'sports'.match(/s./);
show(result);

result = '사랑해 사모해 사랑함'.match(/사.해/g);
show(result);

/*
패턴 문자 .(점) : 점 위치에 문자가 있어야 하며,점의 앞과 뒤에 지정한 문자열을 매치합니다. (단, 줄바꿈 문자는 제외)
필수 지정은 아니기에 점의 앞, 뒤에 문자열을 지정하지 않아도 됩니다.
 */