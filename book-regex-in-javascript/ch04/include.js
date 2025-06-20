var result;

result = 'A12A 12B 12A'.match(/12\B/g);
show(result);

result = 'A12 B12 12'.match(/12\B/g);
show(result);


show('-------');
result = 'A12 12 C12'.match(/\B12/g);
show(result);


show('-------');
result = 'A12 12 C12'.match(/\B12\B/g);
show(result);

result = 'A12B C12D E12F'.match(/\B12\B/g);
show(result);

/*
특수 문자 \B: 영문자, 소문자, 숫자, 언더바를 합친 63개 문자를 나타냄
 */