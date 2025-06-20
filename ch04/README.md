### 콘텐츠 캐싱

### 캐시 영역

```shell
# 캐시 영역, 콘텐츠의 위치 지정

proxy_cache_path /var/nginx/cache
                keys_zone=CACHE:60m
                levels=1:2
                inactive=3h
                max_size=20g;
proxy_cache CACHE;

# /var/nginx/cache 디렉터리를 생성하고 메모리에 CACHE 영역을 60메가바이트 크기로 생성하여 캐시 저장

# proxy_cache 지시자는 http, server, location 블록에서 사용 가능
```

### 캐시 락

```shell
# 생성 중인 캐시에 대해 다른 요청이 들어와도 요청을 처리하지 않고 캐시 생성이 완료될 때까지 대기
proxy_cache_lock on;
# 10초안에 생성돼야 함, 시간이 초과되면 다른 요청을 업스트림 서버로 보내 응답 결과 캐시를 다시 시도
proxy_cache_lock_age 10s; 
# 3초안에 캐시 생성이 완료되지 못하면 다른 요청을 업스트림 서버로 보내 필요한 컨테츠를 가져오도록 함
proxy_cache_lock_timeout 3s; 
# 대신 캐시는 생성하지 않음
```

### 해시 키 값

```shell
proxy_cache_key "$host$request_uri $cookie_user";

# 호스트명, uri, 쿠키 값으로 사용자마다 서로 다른 해시를 캐시 키로 사용하여
# 동적인 페이지를 캐시하여도 다른 사용자의 콘텐츠가 잘못 전달되지 않도록 함
```

### 캐시 우회

```shell
proxy_cache_bypass $http_cache_bypass;

# cache-bypass라는 http 헤더 값이 0이 아닐 때 엔진엑스가 캐시를 우회하도록 합니다.
```

### 캐시 성능

```shell
location ~* \.(css|js)$ {
  expires 1y;
  add_header Cache-Control "public";
}

# Cache-Control 헤더를 추가하며 값을 public으로 지정해 사용자에게
# 콘텐츠가 전달되는 중간에 위치한 어떤 캐시 서버라도 리소스를 캐시할 수 있도록 합니다.
# private으로 지정하면 실제 사용자 환경에만 리소스를 캐시합니다.
```

### 캐시 분할

```shell
proxy_cache_path /tmp/mycache keys_zone=mycache:10m;

server {
  proxy_cache mycache;
  slice 1m;
  proxy_cache_key $host$uri$is_args$args$slice_range;
  proxy_set_header Range $slice_range;
  proxy_http_version 1.1;
  proxy_cache_valid 200 206 1h;
  
  location / {
    proxy_pass http://origin:80;
  }
}

# 캐시 영역을 정의하고 활성화한 후 
# slice 지시자를 사용하여 서버의 응답을 1메가바이트 크기 파일 조각으로 나눕니다.
```