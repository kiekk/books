var result;

result = 'sports'.match(/sp/);
show(result);

var param = /sp/i;
result = 'sports'.match(param);
show(result);

/*
정규 표현식 match 메서드

캡쳐 그룹을 포함해 조건식과 일치하는 문자열이 있을 경우 해당 문자열을 반환합니다.
일치하는 문자열이 없을 경우 null 을 반환합니다.
 */
