var result;

result = 'aabaac'.match(/(aa|aabaac|b)/);
show(result);

result = 'aabaac'.match(/(aa|aabaac|b)*/);
show(result);


show('--------');
result = 'aabkkcd'.match(/(aa|ba|b|c)*/);
show(result);

result = 'aabaac'.match(/(aa|ba|b|c)*/);
show(result);


show('--------');
result = 'aabaac'.match(/(aa|baac|b)*/);
show(result);

result = 'aacbabac'.match(/(aa|ba|b|c)*/);
show(result);
