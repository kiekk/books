var result;

result = 'abcdef'.match(/ab(?:cd)ef/);
show(result);

show(RegExp.$1);
