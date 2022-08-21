var result;

result = 'swim'.match(/swi?m/);
show(result);


show('----------');
result = 'swim'.match(/swiA?m/);
show(result);


show('----------');
result = 'swim'.match(/s.*?/);
show(result);


result = 'swim'.match(/s.*?m/);
show(result);
