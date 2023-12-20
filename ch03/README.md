### 트래픽 관리


### 업스트림 분기

- `split_clients` 지시자는 첫 번째 매개변수에 지정된 문자열을 활용해 해시를 생성,
- 지정된 비율에 따라 두 번째 매개벼눗에 지정된 변수에 값을 할당합니다.

```shell
split_clients "${remote_addr}AAA" $variant {
  20.0% "backendv2";
  *     "backendv1";
}

# $variant 변수는 요청의 20%에 대해 backendv2 가 할당되고, 나머지 80%에 대해서는 backendv1 이 할당됩니다.

location / {
  proxy_pass http://$variant
} 

# 그리고 엔진엑스로 수신된 트래픽은 $variant 변수를 사용해 두 개의 애플리케이션 서버 풀로 분기
```