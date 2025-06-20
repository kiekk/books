var value = ' abcde ';
show(value.length);

value.replace(/^\s+|\s+$/g, '');
show(value);

/*
\s 를 사용하여 앞뒤 공백 제거
 */