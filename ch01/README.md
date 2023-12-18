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