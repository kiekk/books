var result;

result = 'sports'.match(/sports/);
show(result);

result = 'sports'.match(/sp/);
show(result);

result = 'sports'.match(/spt/);
show(result);

result = 'sports'.match(/s/);
show(result);

result = 'sports'.match(/SPORTS/);
show(result);

/*
정규 표현식 match 메서드
조건과 일치하는 문자열만 반환.
일치하는 문자열이 없을 경우 null 반환.
대소문자 구분 O
 */