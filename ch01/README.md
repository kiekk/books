## Nginx 설치

```
Nginx 설치 방법은 크게 3가지 방법이 있습니다.
    1. Debian(데비안) 기반의 운영체제에서는 apt-get install nginx
    2. Red Hat(레드햇) 기반의 운영체제에서는 yum install nginx
    ★ 3. Nginx 소스 코드를 직접 다운로드하여 설치
```

### Nginx 소스 코드로 직접 설치하기

```
Nginx가 C로 작성된 프로그램이기 때문에 GCC 같은 컴파일러 도구가 필요합니다.
```

### GCC 설치 여부 확인

![img.png](img.png)

```
💡패키지 관리자는 root 권한이 필요하기 떄문에 root 계정으로 로그인하거나 sudo 명령어로 실행해야 합니다.💡

GCC 설치
    1. Red Hat(레드햇) 기반의 운영체제에서는 yum groupinstall "Development Tools"
    2. Debian(데비안) 기반의 운영체제에서는 apt-get install build-essential  
```

![img_1.png](img_1.png)

```
설치 후 위와 같이 출력되면 설치 성공입니다.
```

### PCRE 라이브러리
```
엔진엑스를 컴파일하는 데 PCRE(펄 호환 정규 표현식)(Perl Compatible Regular Expression) 라이브러리가 필요합니다.
엔진엑스의 URL 재작성(rewrite) 모듈과 HTTP 핵심 모듈은 PCRE를 정규식 구문에 사용합니다.

pcre, pcre-devel라는 두 가지 패키지를 설치해야 합니다.
pcre: 라이브러리의 컴파일된 버전을 제공
pcre-devel: 프로젝트를 컴파일하는 데 필요한 헤더 파일과 소스를 제공
```

### pcre, pcre-devel 설치
`yum intall pcre pcre-devel`
```
    1. Red Hat(레드햇) 기반의 운영체제에서는 yum intall pcre pcre-devel 또는 intall pcre*
    2. Debian(데비안) 기반의 운영체제에서는 apt-get install libpcre3 libpcre3-dev
```
