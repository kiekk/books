var result;

result = 'aaa'.match(/a{1,}/);
show(result);

result = 'aaa'.match(/a{4,}/);
show(result);


show('-------');
result = 'aaaBB'.match(/a{2,}B/);
show(result);

/*
{숫자, } 은 숫자 이상의 경우는 매치
 */