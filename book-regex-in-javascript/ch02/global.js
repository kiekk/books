var result;

result = 'JavaScript'.match(/a/g);
show(result);

var regexp = new RegExp('A', 'ig');
result = regexp.exec('JavaScript');
show(result);

/*
정규 표현식 flag

g(global): 전역 탐색
= RegExp.prototype.global (en-US)
 */