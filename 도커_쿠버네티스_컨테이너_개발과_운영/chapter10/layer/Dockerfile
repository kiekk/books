FROM ubuntu:22.04

# 1 GH 버전 정의
ARG GH_VERSION=2.33.0

# 2-1 apt 리포지터리 업데이트
RUN apt update

# 2-2 curl 설치
RUN apt install -y curl

# 3-1 아카이브 다운로드
RUN curl -L -O https://github.com/cli/cli/releases/download/v${GH_VERSION}/gh_${GH_VERSION}_linux_amd64.tar.gz

# 3-2 아카이브 압축 해제
RUN tar xvzf gh_${GH_VERSION}_linux_amd64.tar.gz

# 3-3 gh 실행 파일 이동
RUN mv gh_${GH_VERSION}_linux_amd64/bin/gh /usr/local/bin

# 4-1 아카이브와 압축 해제 디렉터리 제거
RUN rm -rf gh_${GH_VERSION}_linux_amd64*

# 4-2 curl 제거
RUN apt purge -y curl

# 4-3 apt 캐시 삭제
RUN apt clean

# 4-4 apt 패키지 리스트 삭제
RUN rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["gh"]
