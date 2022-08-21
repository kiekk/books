var result;

result = 'az'.match(/\s/);
show(result);

result = '\u0009'.match(/\s/);
if (result) {
    show('u0009');
}

/*
\s: 보이지 않는 문자, 공백(whitespace) 또는 줄 구분(line terminator) 에 해당
 */