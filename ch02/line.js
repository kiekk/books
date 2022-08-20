var result = '\u000A'.match(/\n/);
if (result){
    show('줄 분리자');
}

/*
줄 분리자 유니코드
\u000A: 줄 바꿈 - \n
\u000D: 줄 바꿈 (첫 위치) - \r
\u2028: 줄 분리자
\u2029: 구문 분리자
 */
