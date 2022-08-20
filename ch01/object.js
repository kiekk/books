var result;

result = /sp/.exec('sports');
show(result);

var param = /sp/i;
result = param.exec('sports');
show(result);

/*
정규 표현식 exec 메서드
문자열에서 일치하는 부분을 탐색합니다.
일치 정보를 나타내는 배열, 또는 일치가 없는 경우 null 을 반환합니다.
 */