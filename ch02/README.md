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