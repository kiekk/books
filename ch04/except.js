var result;

result = 'A12A 12B 12A'.match(/12\b/g);
show(result);

result = 'A12 12B 12C'.match(/12\b/g);
show(result);

show('-------');
result = 'A12 12B 12'.match(/12\b/g);
show(result);

show('-------');
result = '표현 표현 표현'.match(/표현\b/g);
show(result);

result = '12표현 표현12표현 12표현'.match(/12\b/g);
show(result);

show('-------');
result = 'A급 '.match(/A급\b/);
show(result);

result = 'A와B '.match(/A와B\b/);
show(result);

show('-------');
result = 'A12 12 C12'.match(/\b12/g);
show(result);

result = '12 12 C12'.match(/\b12/g);
show(result);


show('-------');
result = 'A12 12 C12'.match(/12\b/g);
show(result);

result = 'A12 12 C12'.match(/\b12\b/g);
show(result);

/*
특수 문자 \b: \B에 해당하는 영문자, 소문자, 숫자, 언더바를 합친 63개를 제외한 문자
\b 는 단어의 경계 위치를 가리킵니다. (b = boundary)
여기서 단어는 \w 와 일치하며 [a-zA-Z0-9_]와 동일합니다.
즉, 단어와 단어가 아닌 문자와의 사이를 가리키는 것입니다.
한글은 포함되어 있지 않기 때문에 \b 로 한글의 경계를 처리할 수 없습니다.

*/