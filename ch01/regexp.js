var result;

var regexp = new RegExp('sp', 'i');

result = regexp.test('sports');
show(result);

result = new RegExp('sp', 'i').test('sports');
show(result);

regexp = RegExp('sp', 'i');
show(regexp);

regexp = new RegExp('sp', 'i');
show(regexp);
