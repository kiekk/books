var result;
var value = 'JavaScript\nMultiLine\nMultiLine';

result = value.match(/^Multi/);
show(result);

result = value.match(/^Multi/m);
show(result);

result = value.match(/^Multi/g);
show(result);

result = value.match(/^Multi/gm);
show(result);

result = value.match(/Multi/);
show(result);

result = value.match(/multi/igm);
show(result);

/*
정규 표현식 flag

m(multiline): 여러 줄에 걸쳐 탐색
= RegExp.prototype.multiline (en-US)

m 플래그는 처음 매치된 값만 반환,
g 플래그는 전역 탐색, 즉 반복 기능을 가지고 있기 때문에
g 플래그를 같이 사용할 경우 매치된 모든 값을 반환
 */
