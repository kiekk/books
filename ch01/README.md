### NGINX 설치 (우분투)

```shell
# /etc/apt/sources.list.d 경로에 nginx.list 파일 생성 후 아래 내용 작성
# /etc 경로에 파일 작업은 root 계정만 가능하므로, root 계정으로 작업하거나 sudo 명령어 추가
sudo vi /etc/apt/sources.list.d/nginx.list

# 내용
deb http://nginx.org/packages/mainline/ubuntu/ trusty nginx
deb-src http://nginx.org/packages/mainline/ubuntu/ trusty nginx

### 해당 내용으로 nginx 설치 후 실행
# 마찬가지로 root 계정이거나 sudo 명령어로 실행
sudo wget http://nginx.org/keys/nginx_signing.key
sudo apt-key add nginx_signing.key
sudo apt-get update
sudo apt-get install -y nginx
sudo /etc/init.d/nginx start
```

### NGINX 상태 점검

```shell
# 버전 확인
nginx -v
# nginx version: nginx/1.18.0 (Ubuntu)

# nginx 동작 여부 확인
ps -ef | grep nginx
# root        2389       1  0 11:40 ?        00:00:00 nginx: master process /usr/sbin/nginx -g daemon on; master_process on;
# www-data    2392    2389  0 11:40 ?        00:00:00 nginx: worker process
```

### nginx 주요 설정 파일, 디렉터리

````shell
/etc/nginx/
# nginx 기본 설정이 저장된 루트 디렉터리
# 이곳에 저장된 설정 파일의 내용에 따라 동작

/etc/nginx/nginx.conf
# nginx 기본 설정 파일로 모든 설정에 대한 진입점
# 모든 설정 파일을 포함하는 최상위 http 블록을 갖고 있습니다.

/etc/nginx/conf.d/
# 기본 http 설정 파일을 포함
# 디렉터리 내 파일 중 이름이 .conf로 끝나는 파일은 앞서 설명한 /etc/nginx/nginx.conf 파일이 가진 최상위 http 블록에 포함됩니다.

/var/log/nginx
# 엔진엑스의 로그가 저장되는 디렉터리로 access.log와 error.log 파일이 있습니다.
````

`
💡 엔진엑스는 설치 시 conf.d 디렉터리 대신 site-enabled 디렉터리가 있고, symlink를 통해 site-available 디렉터리에 저장된 설정 파일들이
연결돼 있을 수 잇습니다. 하지만 이 방식은 더는 사용되지 않습니다.
`

- 참고 링크: http://bit.ly/3RkigE0
- 참고 링크: https://bit.ly/3ts9yeQ
- 참고 링크: https://bit.ly/48pno0D