var result;

result = 'abcdef'.match(/ab(?!ef)/);
show(result);

result = 'abcdef'.match(/ab(?!cd)/);
show(result);

show('-------');
result = 'abcdef'.match(/abc(?!KK)/);
show(result);

result = 'abcdef'.match(/aAA(?!KK)/);
show(result);
