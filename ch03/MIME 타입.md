### MIME 타입

```
엔진엑스는 MIME 타입을 구성하는 데 유용한 두 가지 지시어 types와 default_type을 제공합니다.
이 둘은 문서의 기본 MIME 타입을 지정하며, 응답에 포함돼 보내질 Content-Type HTTP 헤더에 영향을 미칩니다. 
```

`types`
- 블록: http, server, location
- MIME 타입과 파일 확장자의 상관관계를 맺는데 사용됩니다.
`include mime.types;`
- 엔진엑스는 MIME 기본 타입 셋트를 `mime.types`라는 파일에 가지고 있습니다.
- 해당 파일에 작성된 타입에서 제공하려는 파일의 확장자를 찾을 수 없다면 `default_type` 지시어로 정의한 기본 타입이 사용됩니다.
- `types`블록은 아래와 같이 다시 선언해서 타입 목록을 재정의할 수 있습니다.

#### ex
```
types {
    MIME타입1 확장자1;
    MIME타입2 확장자2 [확장자3...];
    [...]
}
```

```
http {
    include mime.types;
    [...]
    location /downloads/ {
        # 모든 MIME 타입 제거
        types {}
        default_type application/octet-stream;
    } [...]
}
```

- `mime.types` 파일이 병합되지 않았을 때 기본값
```
types {
    text/html html;
    image/gif gif;
    image/jpeg jpeg;
}
```

`default_type`
- 블록: http, server, location
- 구문: MIME 타입
- 기본값: text/plain
- 기본 MIME 타입을 정의합니다.
- 엔진엑스가 파일을 제공할 때 Content-Type HTTP 응답 헤더 값으로 적절한 MIME 타입이 반환되게 하려면 파일 확장자로 types 블록에 선언된 타입 중에서 맞는 타입이 있는지 찾습니다.
- 이 확장자가 작성된 MIME 타입 어느 것과도 일치하지 않을 경우 해당 지시어의 값이 사용됩니다.

`types_hash_max_size`
- 블록: http, server, location
- 구문: 숫자 값
- 기본값: 4k | 8k
- MIME 타입 해시 테이블의 최대 크기를 정의합니다.

`types_hash_bucket_size`
- 블록: http, server, location
- 구문: 숫자 값
- 기본값: 64(CPU 캐시 라인 크기)
- MIME 타입 해시 테이블의 버킷 크기를 설정하는 지시어입니다.