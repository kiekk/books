var result = 'abcdef'.match(/ab(c|p)(d|q)ef/);
show(result);


show(RegExp.$1);
show(RegExp.$2);

show(RegExp.$3);
show(RegExp.$9);
show(RegExp.$0);
show(RegExp.$12);
