### 고성능 부하분산

#### HTTP 부하분산
```shell
upstream backend {
  server server-1-ip weight=1;
  server server-2-ip weight=2;
  server backup-server-ip backup;
  # server-1, server-2 모두 연결이 불가능하면 backup 서버를 사용
  # weight은 가중치로 server-2가 server-1에 비해 두 배 많은 요청을 받으며, 생략 가능
}

server {
  location / {
    proxy_pass http://backend;
  }
}
```

### TCP 부하분산

```shell
stream {
  upstream mysql_read {
    server read1-server-ip:3306 weight=;
    server read2-server-ip:3306;
    server server-backup backup;
  }
  
  server {
    listen 3306;
    proxy_pass mysql_read;
  }
}

# stream 블록을 사용한 모듈은 stream.conf.d에 저장하는 것이 좋다.
# 따라서 아래와 같이 저장한다.
```

```shell
# /etc/nginx/nginx.conf

...

stream {
  include /etc/nginx/strea.conf.d/*.conf;
}

# /etc/nginx/stream.conf.d/mysql_read.conf

upstream mysql_read {
    server read1-server-ip:3306 weight=;
    server read2-server-ip:3306;
    server server-backup backup;
}
  
server {
    listen 3306;
    proxy_pass mysql_read;
}
```

### UDP 부하분산

```shell
stream {
  upstream ntp {
    server server-1:123 weight=2;
    server server-2:123;
    server server-backup backup;
  }
  
  server {
    listen 123 udp;
    proxy_pass ntp;
  }
}

# udp 도 stream 블록을 사용하므로 tcp 와 같이 stream.conf.d에 저장하는 것이 좋다. 
```

### 부하분산 종류

- round robin: 순서에 따라 요청을 분산 (기본값)
- least connection: 연결이 적은 서버를 먼저 활용 - `least_conn`
- least time: 응답 속도가 빠른 서버를 우선 사용 (엔진엑스 플러스에서만 사용 가능) - `least_time`
- generic hash: 특정 문자열 기반 해시를 활용 - `hash`
- random: 임의 서버를 할당 - `random`
- IP hash: IP 주소 기반 해시를 사용 - `ip_hash`

### 스티키 쿠키

```shell
upstream backend {
  server backend1.example.com;
  server backend2.example.com;
  sticky cookie
          affinity # cookie name
          expires=1h
          domain=.example.com
          httponly
          secure
          path=/;
}
```

### 수동적인 헬스 체크

- 동작에 문제가 없는 업스트림 서버만 사용하려면 엔진엑스 부하분산 설정에 헬스 체크 매개변수를 추가합니다.

```shell
upstream backend {
  server backend1.example.com:1234 max_fails=3 fail_timeout=3s;
  server backend2.example.com:1234 max_fails=3 fail_timeout=3s;
}
```

### 능동적인 헬스 체크

```shell
http {
  server {
    # ...
    
    location / {
      proxy_pass http://backend;
      health_check interval=2s
                    fails=2
                    passes=5
                    uri=/
                    match=welcome;
    }
  }
  # 응답 코드가 200이고 Content-Type이 "text/html"이면서
  # 응답 바디에 "Welcome to nginx!" 문자열이 있는지 확인
  
  match welcome {
    status 200;
    header Content-Type = text/html;
    body ~ "Welcome to nginx!";
  }
}
```