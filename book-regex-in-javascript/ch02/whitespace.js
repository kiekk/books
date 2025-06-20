var result;

result = '\u0009'.match(/\t/);
show(result);

result = '\u000c'.match(/\f/);
if (result) {
    show('공백 문자');
}

/*
공백 문자 유니코드
\u0009: 탭 - \t
\u000B: 수직 탭 - \v
\u000C: 폼 넘김 - \f
\u0020: 공백
\u00A0: 자동 줄바꿈 방지
 */