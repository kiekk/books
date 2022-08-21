var reg = new RegExp('\\w');
show(typeof reg);

var cons = reg.constructor === RegExp;
show(cons);
