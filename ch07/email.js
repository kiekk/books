
var email = /^[\w][\w-\.]+@[\w]+(\.[A-Za-z0-9]+)*(\.[A-Za-z]{2,3})$/i;

var result = email.test('abcd@efgh.co.kr');
show(result);
