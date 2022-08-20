var result;

result = '12_34_56'.match(/23|34|56/);
show(result);

result = '12_34_56'.match(/23|56|34/);
show(result);


show('-------');
result = /bc|c/.exec("abc");
show(result);

result = /c|bc/.exec("abc");
show(result);


show('-------');
result = /c|bc|abc/.exec("abc");
show(result);

result = /c|bc|a|abc/.exec("abc");
show(result);

result = /c|bc|abc|a/.exec("abc");
show(result);


show('-------');
result = '12_34_56'.match(/12|34|56/g);
show(result);

/*
| (대체) : alternative

정규 표현식의 |(바)의 왼쪽, 오른쪽 모두를 매치하여 매치 결과를 보관합니다.
그리고 왼쪽 인덱스, 오른쪽 인덱스를 비교하여 같거나 작으면 왼쪽 매치 결과를 반환하고 아니면 오른쪽 매치 결과를 반환합니다.

즉, 인덱스 값에 따라 매치된 문자열이 결정됩니다.
그리고 인덱스 값이 같을 경우에는 먼저 매치된 문자열이 반환됩니다.
 */