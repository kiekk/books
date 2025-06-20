var result;

result = 'ABC'.match(/(A)/);
show(result);

result = 'ABC'.match(/((A))/);
show(result);

result = 'ABCDEF'.match(/AB(C|P)(D|Q)EF/);
show(result);


show('--------');
result = /((a)|(ab))/.exec("abc");
show(result);


show('--------');
result = /((im)|(sw))/.exec("swim");
show(result);

