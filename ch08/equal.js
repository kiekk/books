var result;

result = 'abcc'.match(/ab(?=c)/);
show(result);

result = 'abcc'.match(/ab/);
show(result);

result = 'abdcc'.match(/ab(?=c)/);
show(result);

result = 'dabcc'.match(/ab(?=c)/);
show(result);

show('-------');
result = 'abch'.match(/ab(?=c)ch/);
show(result);

