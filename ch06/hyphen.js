var result;

result = '54321'.match(/[0-9]/);
show(result);

//에러
//result = '12345'.match(/[9-0]/);


show('-------');
result = 'cdbd'.match(/[a-e]/g);
show(result);

result = '가나다라'.match(/[가-라]/g);
show(result);


show('-------');
result = '7321'.match(/[-3]/);
show(result);

result = '721'.match(/[-3]/);
show(result);

result = '-321'.match(/[-3]/);
show(result);


show('-------');
result = '7321'.match(/[3-]/);
show(result);

result = '721'.match(/[3-]/);
show(result);

result = '-321'.match(/[3-]/);
show(result);


show('-------');
result = 'AB*^]cd'.match(/[A-D]/ig);
show(result);

result = 'AB[\^]"_cd'.match(/[A-d]/ig);
show(result);


show('-------');
result = 'aA1'.match(/[A-Za-z0-9]/g);
show(result);

/*
대괄호 안에 하이픈(-)을 작성하고 앞고 뒤에 값을 지정하지 않으면 하이픈(-)을
단지 문자로 인식합니다.
 */

