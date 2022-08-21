var result;

result = 'abcdef_cd'.match(/ab(c|K)(d|X)ef_/);
show(result);

result = 'abcdef_cd'.match(/ab(c|K)(d|X)ef_\1\2/);
show(result);

