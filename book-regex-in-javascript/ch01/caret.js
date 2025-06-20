var result;

result = '12_34_12'.search(/^34/);
show(result);

result = '12_34_12'.search(/^12/);
show(result);

/*
정규 표현식 search 메서드
문자열에서 일치하는 부분을 탐색합니다. 일치하는 부분의 인덱스, 또는 일치가 없는 경우 -1을 반환합니다.
 */