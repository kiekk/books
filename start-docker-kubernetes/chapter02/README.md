### 2장. 도커 엔진

> 정리 범위: 핵심 명령어, run vs create, `-i -t -d`, 볼륨, inspect, 네트워크 구조와 종류, 자원 제한, 이미지 레이어, Dockerfile, ENTRYPOINT vs CMD, 도커 데몬, 스토리지 드라이버, 모니터링 명령어
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop, Docker Engine 28.0.4, VM 커널 `6.10.14-linuxkit`, cgroup v2, `overlay2`, buildx 0.22.0 / BuildKit 0.20.2
>
> 표기: 이 노트의 코드 블록 중 `$` 명령과 그 아래 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 8절 "확인하지 못한 부분"에 모았다.

#### 목차

1. 핵심 명령어 한눈에 보기
2. run vs create
3. `-i`, `-t`, `-d` 옵션
4. 볼륨 (volume)
5. inspect
6. 네트워크 구조와 종류
7. 컨테이너 자원 할당과 제한
8. 이미지 레이어
9. Dockerfile과 빌드 과정
10. ENTRYPOINT vs CMD
11. 도커 데몬
12. 스토리지 드라이버
13. 모니터링 명령어
14. 확인하지 못한 부분

---

#### 1. 핵심 명령어 한눈에 보기

| 분류 | 명령어 | 설명 |
|---|---|---|
| 이미지 | `pull` / `push` / `images` / `rmi` / `tag` | 받기 / 올리기 / 목록 / 삭제 / 별칭 |
| | `build` / `history` / `save` / `load` / `commit` | 빌드 / 레이어 이력 / tar 저장·복원 / 컨테이너를 이미지로 |
| 컨테이너 수명 | `create` / `start` / `run` | 생성 / 시작 / 생성+시작 |
| | `stop` / `kill` / `restart` / `pause` / `unpause` / `rm` | 종료 / 시그널 / 재시작 / 일시정지 / 해제 / 삭제 |
| 접속·실행 | `exec` / `attach` / `cp` | 실행 중인 컨테이너에서 명령 / 메인 프로세스에 붙기 / 파일 복사 |
| 조회 | `ps` / `logs` / `inspect` / `top` / `stats` / `diff` / `events` / `port` | 3·5·13절 |
| 볼륨·네트워크 | `volume ...` / `network ...` | 4·6절 |
| 시스템 | `info` / `version` / `system df` / `system prune` | 11·13절 |

##### 1-1. 실험 중 확인한 실무 포인트

**`docker stop`의 시그널과 10초 대기**

```
$ docker run -d --name s1 alpine:3.20 sleep 300      # SIGTERM 핸들러가 없는 프로세스
$ time docker stop s1                                 → real 10.16s   (exit code 137)

$ docker run -d --name s2 alpine:3.20 sh -c 'trap "echo got-TERM; exit 0" TERM; while :; do sleep 1; done'
$ time docker stop s2                                 → real 1.09s    (logs: got-TERM, exit code 0)
```

- `stop`은 PID 1에 `SIGTERM`을 보내고, 기본 10초 안에 안 죽으면 `SIGKILL`을 보낸다 (`-t`로 대기 시간 조정). `kill`은 바로 `SIGKILL`이 기본값이다.
- 컨테이너의 PID 1은 커널이 기본 시그널 동작(종료)을 적용하지 않는다. 핸들러를 등록하지 않은 프로세스는 `SIGTERM`을 무시하므로 10초를 기다린 뒤 137(128+9)로 죽는다.
- 그래서 **graceful shutdown을 하려면 앱이 SIGTERM을 처리해야 하고, PID 1이 앱이어야** 한다. 10절의 shell form 문제와 연결된다.

**재시작 정책**

```
$ docker run -d --name r1 --restart on-failure:2 alpine:3.20 sh -c 'exit 1'
$ docker inspect r1 --format 'restarts={{.RestartCount}} status={{.State.Status}}'
restarts=2 status=exited
```

`no`(기본), `on-failure[:max]`, `always`, `unless-stopped`. 최대 2회 재시작 후 포기했다.

**`pause`**: `paused=true`이고 `docker top`에는 프로세스가 그대로 보인다. 종료가 아니라 얼려 두는 것이다.

**`docker diff`**: 컨테이너 쓰기 레이어의 변경 사항을 `A`(추가) `C`(변경) `D`(삭제)로 보여준다.

```
$ docker exec c sh -c 'echo x > /tmp/f; rm /etc/motd; echo y >> /etc/hostname'
$ docker diff c
C /etc
D /etc/motd
C /tmp
A /tmp/f
```

`/etc/hostname`을 수정했는데 diff에 나오지 않았다. `/etc/hostname`, `/etc/hosts`, `/etc/resolv.conf`는 도커가 컨테이너 시작 시 **bind mount**로 덮어씌우는 파일이라 이미지/쓰기 레이어 변경으로 추적되지 않고, 컨테이너를 재생성하면 사라진다.

**`commit`, `save`, `export`**

```
$ docker exec c sh -c 'echo changed > /hello'
$ docker commit c t2-committed:v1          # 쓰기 레이어를 새 이미지 레이어로 굳힘
$ docker run --rm t2-committed:v1 cat /hello  → changed
$ docker save t2-committed:v1 -o img.tar   # blobs/sha256/... 구조의 tar (OCI 레이아웃)
$ docker export c | tar t                  # 컨테이너 rootfs만 (.dockerenv, bin/, ...) — 레이어/메타데이터 없음
```

`save/load`는 이미지(레이어+메타데이터) 단위, `export/import`는 컨테이너 파일시스템 평탄화본이다. `commit`은 재현 불가능한 이미지가 되므로 운영에서는 Dockerfile 빌드를 쓴다.

> ⚠️ 함정: `docker pull --platform linux/amd64 alpine:3.20`을 하면 **같은 태그 `alpine:3.20`이 amd64 이미지로 덮어씌워진다.** 1장 실험에서 이렇게 해서, 이후 모든 `alpine:3.20` 실행에 `requested image's platform (linux/amd64) does not match the detected host platform` 경고가 뜨고 Rosetta로 실행됐다 (`docker top`에 `/run/rosetta/rosetta`가 보였다). `docker pull --platform linux/arm64`로 되돌린 뒤 `docker image inspect --format '{{.Architecture}}'`로 `arm64`를 확인했다.

#### 2. run vs create

```
docker run  =  (이미지 없으면 pull)  +  create  +  start  (+ attach)
docker create = (이미지 없으면 pull)  +  create       ← 시작은 안 함
```

```
$ docker create --name c1 alpine:3.20 echo hi
$ docker ps -a --filter name=c1     → c1  Created
$ docker start -a c1                → hi
$ docker ps -a --filter name=c1     → c1  Exited (0) Less than a second ago
$ docker start -a c1                → hi       # 같은 컨테이너를 다시 시작
```

| 구분 | `create` | `run` |
|---|---|---|
| 결과 상태 | `Created` (프로세스 없음) | `Up` 또는 종료됨 |
| 용도 | 설정을 먼저 만들어 두고 나중에 `start` | 일반적인 실행 |
| 재사용 | `start`로 같은 컨테이너 재시작 (쓰기 레이어 유지) | 매번 새 컨테이너 생성 |
| `-d` 없을 때 | 해당 없음 | 포그라운드에서 attach |

- 컨테이너는 **종료돼도 삭제되지 않는다.** 쓰기 레이어가 남아 있고 `docker start`로 다시 살릴 수 있다. 일회성이면 `--rm`.
- `start`로 재시작하면 컨테이너 설정(`-v`, `-p`, `--memory` 등)이 그대로이고, 이것들을 바꾸려면 다시 `create/run`해야 한다 (일부 자원 제한은 `docker update`로 가능, 7절).
- `create`의 실용적 용도: 네트워크에 연결(`docker network connect`)하거나 파일을 `docker cp`로 넣은 뒤 시작하는 것처럼 **시작 전에 준비 작업이 필요한 경우**.

#### 3. `-i`, `-t`, `-d` 옵션

| 옵션 | 의미 | 실제 효과 |
|---|---|---|
| `-i` (`--interactive`) | 표준 입력(stdin)을 열어 둔다 | 없으면 stdin이 닫혀 쉘이 바로 종료 |
| `-t` (`--tty`) | 가상 터미널(pty) 할당 | 프롬프트, 색상, 줄 편집, `Ctrl+C` 처리 |
| `-d` (`--detach`) | 백그라운드 실행 | 컨테이너 ID만 출력하고 반환 |

실측:

```
$ echo "echo hello-from-stdin" | docker run --rm -i alpine:3.20 sh
hello-from-stdin                                  # -i: stdin 전달됨

$ echo "echo hello-from-stdin" | docker run --rm alpine:3.20 sh
(출력 없음, exit=0)                                # -i 없음: stdin이 닫혀 sh가 입력 없이 종료

$ docker run --rm -t alpine:3.20 sh -c 'tty; echo TERM=$TERM'
/dev/pts/0
TERM=xterm                                        # -t: pty 할당, TERM 설정

$ docker run --rm alpine:3.20 sh -c 'tty'
not a tty                                         # -t 없음
```

**`-d`로 쉘을 띄우면 바로 종료되는 이유**

```
$ docker run -d --name d2 alpine:3.20 sh
$ docker ps -a --filter name=d2     → d2  Exited (0) 1 second ago      # stdin이 없어 sh가 EOF로 종료

$ docker run -dit --name d3 alpine:3.20 sh
$ docker ps --filter name=d3        → d3  Up 1 second                  # -it로 stdin+tty를 붙여 살아 있음
```

- 컨테이너는 **PID 1 프로세스가 살아 있는 동안만** 실행된다. 쉘은 stdin이 닫히면 종료한다.
- `-it` 조합은 대화형 쉘, `-dit`는 백그라운드로 쉘을 살려 두고 `docker exec -it`로 들어갈 때 쓴다.
- `exec`: 실행 중 컨테이너의 네임스페이스에 **새 프로세스**를 추가한다. 실측 `ps`에서 PID 1은 원래 `sh`, `exec`로 띄운 `ps`는 PID 7.
- `attach`: **PID 1의 stdio에 붙는다.** 거기서 `exit`하면 PID 1이 죽어 컨테이너가 종료될 수 있다 (`Ctrl+P, Ctrl+Q`로 detach). 대부분의 경우 `exec`가 안전하다. (`attach`는 이 실험에서 실행하지 않았다)

##### 3-1. 그 밖에 알아둘 `docker run` 옵션

네트워크(`-p`, `--network`), 볼륨(`-v`), 자원(`-m`, `--cpus`) 옵션은 각 절에서 다뤘다. 여기서는 그 외 자주 쓰는 옵션을 정리한다.

| 옵션 | 설명 | 실측 |
|---|---|---|
| `--rm` | 종료 시 컨테이너 자동 삭제 | 종료 후 `docker ps -a`에 0개. 일회성 작업의 기본 |
| `--name` | 컨테이너 이름 지정 | 같은 이름이 있으면 `Conflict. The container name "/x" is already in use` (종료된 컨테이너도 이름을 점유) |
| `-e K=V` / `-e K` | 환경 변수. 값 없이 `-e HOSTVAR`만 쓰면 **현재 셸의 값을 전달** | `HOSTVAR=fromhost` 전달됨 |
| `--env-file f` | 파일에서 환경 변수 로드 (`#` 주석 가능) | `A=1`, `B=two words` 로드됨. **따옴표는 벗겨지지 않는다** (`Q="quoted"` → 값에 `"`가 그대로 포함) |
| `-w` / `--workdir` | 작업 디렉터리 | `pwd` → `/tmp` (Dockerfile `WORKDIR` 덮어씀) |
| `-h` / `--hostname` | 컨테이너 호스트명 | `myhost` |
| `-u` / `--user` | 실행 사용자 | `-u 1000`만 주면 `uid=1000 gid=0(root)`로 **그룹은 root**. `-u 1000:1000`처럼 gid까지 지정 |
| `--group-add` | 보조 그룹 추가 | `groups=0(root),1234` |
| `--entrypoint` | 이미지의 ENTRYPOINT 덮어쓰기 | `--entrypoint sh img -c '...'` 처럼 디버깅에 유용 (10절) |
| `--label k=v` | 메타데이터 | `docker ps --filter label=env=dev`로 필터 |
| `--pull always\|missing\|never` | 이미지 pull 정책 | `never`이고 로컬에 없으면 `No such image` 에러 |
| `--restart` | 재시작 정책 | `--rm`과 **같이 쓸 수 없다** (`conflicting options: cannot specify both --restart and --rm`) |
| `--stop-signal` | `docker stop` 때 보낼 시그널 | `SIGINT`로 지정하자 `got-INT` 처리됨 |
| `--stop-timeout N` | SIGKILL 전 대기 시간(초) | `2` → `stop`이 2.13초에 끝남 (기본 10초) |
| `--cidfile f` | 컨테이너 ID를 파일에 기록 | 파일 내용이 `inspect`의 `.Id`와 동일 (스크립트에서 ID 추적) |
| `--ip` | 고정 IP (**사용자 정의 네트워크에서만**) | `--subnet 172.30.0.0/24` 네트워크에서 `172.30.0.50` 할당 |
| `--device host:container` | 호스트 장치를 컨테이너에 노출 | `/dev/null`을 `/dev/mynull`로 노출 (`crw-rw-rw-`) |
| `--platform` | 이미지 플랫폼 지정 | 1절의 태그 덮어쓰기 함정 참고 |

**권한/보안 옵션**

| 옵션 | 설명 | 실측 |
|---|---|---|
| `--cap-add` / `--cap-drop` | 특정 capability만 추가/제거 | 기본은 `mount` 거부. `--cap-add SYS_ADMIN`을 주면 `mount -t tmpfs`가 성공 |
| `--privileged` | 거의 모든 capability + 모든 장치 접근 | `CapEff: 000001ffffffffff`, `/dev` 항목 **167개** (기본은 `00000000a80425fb`, **15개**) |
| `--security-opt no-new-privileges` | setuid 등으로 권한 상승 차단 | `NoNewPrivs: 1` |
| `--sysctl k=v` | 컨테이너 net/ipc namespace의 커널 파라미터 변경 | `net.ipv4.ip_local_port_range="40000 40100"` 적용됨. 네임스페이스에 속하지 않는 값은 거부됨(`kernel.hostname=x` → `sysctl ... is not allowed`) |
| `--pid=host` | 호스트 PID namespace 공유 | `ps` 결과 **416줄**(VM의 전체 프로세스) vs 기본 4줄 |
| `--read-only`, `-u`, `--init` | 7-3절 | |

- `--privileged`는 컨테이너가 호스트 장치와 거의 모든 권한을 갖게 하므로 컨테이너 탈출 위험이 크다. 필요한 권한만 `--cap-add`로 주는 것이 원칙이다. 이 노트의 실험에서 VM 내부를 보려고 `--privileged --pid=host -v /:/host`를 쓴 것은 "컨테이너가 호스트 root와 같다"는 것의 예이기도 하다.
- `--env-file`은 `docker compose`의 `.env`/`env_file`과 파싱 규칙이 다르다. 따옴표 처리 때문에 값이 달라지는 사고가 잦다.
- `-e` 값은 `docker inspect`의 `.Config.Env`에 평문으로 남으므로 비밀번호 전달에는 주의가 필요하다 (시크릿 관리는 이후 장에서).
- `-e X=1 -e X=2`처럼 중복 지정하면 **뒤의 값**(`X=2`)이 적용된다.

#### 4. 볼륨 (volume)

컨테이너 쓰기 레이어는 컨테이너와 함께 사라지고 성능도 UnionFS를 거친다. 데이터를 컨테이너 수명과 분리하려면 **볼륨/바인드 마운트/tmpfs**를 쓴다.

| 종류 | 문법 | 저장 위치 | 특징 |
|---|---|---|---|
| **named volume** | `-v name:/path` | `/var/lib/docker/volumes/name/_data` | 도커가 관리, 컨테이너와 독립 |
| **anonymous volume** | `-v /path` 또는 이미지의 `VOLUME` | 위와 같은 경로 (이름이 64자 해시) | 이름이 없어 추적/정리가 어려움 |
| **bind mount** | `-v /host/path:/path` | 호스트의 임의 경로 | 호스트 파일을 그대로 노출 |
| **tmpfs** | `--tmpfs /path` | 메모리 | 컨테이너 종료 시 삭제, 디스크에 안 남음 |

##### 4-1. named volume

```
$ docker volume create vol
$ docker volume inspect vol --format '{{.Driver}} {{.Mountpoint}} scope={{.Scope}}'
local /var/lib/docker/volumes/vol/_data scope=local

$ docker run --rm -v vol:/data alpine:3.20 sh -c 'echo persisted > /data/a.txt'
$ docker run --rm -v vol:/data alpine:3.20 cat /data/a.txt
persisted                                           # 컨테이너가 사라져도 데이터 유지
```

- 실제 경로는 **Docker VM 내부**다. Mac 호스트에서 `ls /var/lib/docker/volumes/...`로는 볼 수 없다. VM 루트를 마운트한 컨테이너(`-v /:/host`)에서 `ls /host/var/lib/docker/volumes/vol/_data`로 `a.txt`를 확인했다. Linux 서버에서는 호스트의 그 경로에 직접 있다.
- 같은 볼륨을 여러 컨테이너가 동시에 마운트할 수 있고(`--volumes-from`도 가능), 동시 쓰기 정합성은 앱이 책임진다.

```
$ docker run -d --name v1 -v vol:/data alpine:3.20 sleep 100
$ docker run --rm --volumes-from v1 alpine:3.20 cat /data/a.txt   → persisted
```

##### 4-2. 이미지의 `VOLUME`과 anonymous volume

```
$ docker run -d --name pg -e POSTGRES_PASSWORD=x postgres:16-alpine
$ docker inspect pg --format '{{range .Mounts}}{{.Type}} name={{.Name}} dst={{.Destination}}{{end}}'
volume name=ff1c1508f423...(64자 해시) dst=/var/lib/postgresql/data
$ docker rm -f pg          # -v 옵션 없이 삭제 → 볼륨은 남는다 (dangling)
```

- 이미지가 `VOLUME`을 선언하면 `-v`를 안 줘도 **익명 볼륨이 자동 생성**된다. 컨테이너를 `docker rm`해도(`-v` 없이) 남아 디스크를 차지한다. 이 환경에서도 이름 없는 64자 해시 볼륨이 여러 개 쌓여 있었다.
- 컨테이너와 연결이 없는 볼륨은 `docker volume ls --filter dangling=true`로 찾는다. 정리는 `docker volume rm`/`docker volume prune`이다. 이 환경에는 컨테이너가 없어 dangling으로 분류된 **이름 있는 볼륨**(이전 프로젝트의 DB 데이터로 보이는 것)도 있었다. `prune`을 습관적으로 실행하다 데이터를 날릴 수 있으므로, 실행 전에 `ls --filter dangling=true`로 대상을 확인해야 한다. (Docker 23 이후 `volume prune`은 기본적으로 익명 볼륨만 지운다고 알려져 있으나 이 환경에서 확인하지는 않았다.)

##### 4-3. 초기 데이터 복사 vs bind mount의 덮어쓰기

```
$ docker run --rm -v newvol:/etc/apk alpine:3.20 ls /etc/apk
arch  keys  protected_paths.d              # 빈 named volume → 이미지의 기존 내용이 복사되어 채워짐

$ docker run --rm -v $PWD/bind:/etc/apk alpine:3.20 ls /etc/apk
h.txt                                        # bind mount → 이미지의 /etc/apk를 가려버림(shadow)
```

- **named volume**: 비어 있으면 마운트 지점의 이미지 내용을 복사해서 초기화한다.
- **bind mount**: 이미지 내용을 **가린다.** 호스트 디렉터리가 비어 있으면 컨테이너 쪽도 빈 디렉터리가 된다. 설정 디렉터리를 bind로 잡았다가 앱이 기동에 실패하는 흔한 원인이다.

##### 4-4. bind mount의 세부 동작

```
$ docker run --rm --mount type=bind,src=$PWD/nonexist,dst=/x alpine:3.20 true
docker: Error response from daemon: invalid mount config for type "bind": bind source path does not exist: ...

$ docker run --rm -v $PWD/nonexist2:/x alpine:3.20 ls -ld /x
drwxr-xr-x  2 root root 64 ... /x
$ ls -ld nonexist2                               # 호스트에 디렉터리가 자동 생성됨
drwxr-xr-x  2 shyoon  wheel  64 ...
```

- `-v host:container`는 호스트 경로가 없으면 **디렉터리로 만들어 버린다**(오타가 조용히 넘어간다). `--mount`는 에러를 낸다. 의도하지 않은 생성을 막으려면 `--mount`가 안전하다.

```
$ docker run --rm -v $PWD/bind:/b alpine:3.20 sh -c 'touch /b/c.txt; ls -ln /b'
-rw-r--r--  1 0  0  0  c.txt                    # 컨테이너 안: root(0) 소유
$ ls -ln bind                                    # Mac 호스트에서 본 같은 파일
-rw-r--r--  1 501  0  0  c.txt                  # 내 계정(501) 소유
$ docker run --rm -v $PWD/bind:/b alpine:3.20 sh -c 'mount | grep " /b "'
/run/host_mark/private on /b type fakeowner (rw,nosuid,nodev,relatime,fakeowner)
```

- Docker Desktop(Mac)에서는 `fakeowner`로 소유권을 변환해 준다. **Linux 서버에서는 이 변환이 없어** 컨테이너 root가 만든 파일이 호스트에서도 root 소유가 되어 권한 문제가 흔하다 (이 부분은 직접 확인하지 못했다).
- bind mount는 Mac/Windows에서 파일 공유 계층을 거치므로 named volume보다 느릴 수 있다 (이 환경에서 측정하지 않았다).

##### 4-5. 읽기 전용, tmpfs, 백업

```
$ docker run --rm -v vol:/data:ro alpine:3.20 sh -c 'echo x > /data/b'
sh: can't create /data/b: Read-only file system

$ docker run --rm --tmpfs /t:size=1m alpine:3.20 sh -c 'df -h /t | tail -1; mount | grep " /t "'
tmpfs  1.0M  0  1.0M  0% /t
tmpfs on /t type tmpfs (rw,nosuid,nodev,noexec,relatime,size=1024k)

# 백업: 볼륨과 호스트 디렉터리를 동시에 마운트한 임시 컨테이너로 tar
$ docker run --rm -v vol:/data -v $PWD:/backup alpine:3.20 tar czf /backup/vol.tgz -C /data .
$ tar tzf vol.tgz
./
./a.txt
```

##### 4-6. 관련 명령어와 옵션 정리

| 명령/옵션 | 설명 |
|---|---|
| `docker volume create/ls/inspect/rm/prune` | 볼륨 관리 |
| `-v name:/path[:ro]` | 볼륨/바인드 마운트. `:ro`로 읽기 전용 |
| `--mount type=volume\|bind\|tmpfs,src=..,dst=..[,readonly]` | 명시적 문법. 소스가 없으면 에러 |
| `--volumes-from <container>` | 다른 컨테이너의 볼륨을 그대로 상속 |
| `--tmpfs /path[:size=..]` | 메모리 파일시스템 |
| `docker rm -v <container>` | 컨테이너 삭제 시 익명 볼륨도 삭제 |
| `--read-only` | 루트 파일시스템을 읽기 전용으로 (쓸 곳은 볼륨/tmpfs로) |
| `docker run -v` vs `--mount` | 같은 기능. `--mount`가 길지만 명확하고 안전 |

선택 기준: 영속 데이터(DB 등) → named volume / 개발 중 소스 공유 → bind mount / 비밀·임시 데이터 → tmpfs.

#### 5. inspect

`docker inspect`는 컨테이너/이미지/볼륨/네트워크의 **전체 설정과 상태를 JSON으로** 보여준다. `--format`(Go 템플릿)으로 필요한 필드만 뽑는다.

```
$ docker run -d --name i1 -p 18080:80 -e FOO=bar -m 64m --label app=demo \
    --restart unless-stopped -v vol:/data nginx:alpine
$ docker inspect i1 --format 'State={{.State.Status}} Pid={{.State.Pid}}
Image={{.Config.Image}} Cmd={{.Config.Cmd}} Entry={{.Config.Entrypoint}}
Ports={{json .NetworkSettings.Ports}}
IP={{.NetworkSettings.IPAddress}} GW={{.NetworkSettings.Gateway}} Mac={{.NetworkSettings.MacAddress}}
Mem={{.HostConfig.Memory}} Restart={{.HostConfig.RestartPolicy.Name}} NetMode={{.HostConfig.NetworkMode}}
Mounts={{json .Mounts}}
LogPath={{.LogPath}}
GraphDriver={{.GraphDriver.Name}}'
State=running Pid=17403
Image=nginx:alpine Cmd=[nginx -g daemon off;] Entry=[/docker-entrypoint.sh]
Ports={"80/tcp":[{"HostIp":"0.0.0.0","HostPort":"18080"}]}
IP=172.17.0.2 GW=172.17.0.1 Mac=26:a9:e0:c9:92:d4
Mem=67108864 Restart=unless-stopped NetMode=bridge
Mounts=[{"Type":"volume","Name":"vol","Source":"/var/lib/docker/volumes/vol/_data","Destination":"/data",...}]
LogPath=/var/lib/docker/containers/5423...ad/5423...ad-json.log
GraphDriver=overlay2
```

자주 쓰는 필드:

| 경로 | 알 수 있는 것 |
|---|---|
| `.State.Status/Running/Paused/ExitCode/OOMKilled/StartedAt` | 상태, 종료 원인 (OOM이면 `OOMKilled=true`, 7절 참고) |
| `.State.Pid` | **VM(호스트) 커널에서의 PID.** `/proc/<pid>/ns`로 namespace 확인 (1장) |
| `.State.Health.Status` | 헬스체크 결과 (13절) |
| `.Config.Env/Cmd/Entrypoint/Labels` | 이미지+실행 옵션이 합쳐진 최종 설정 |
| `.HostConfig.Memory/NanoCpus/PidsLimit/RestartPolicy` | 자원 제한, 재시작 정책 |
| `.NetworkSettings.IPAddress/Ports/Networks` | IP, 포트 매핑, 연결된 네트워크 |
| `.Mounts` | 실제 마운트 (타입, 소스, 대상, RW) |
| `.GraphDriver.Data` | overlay2의 `LowerDir/UpperDir/MergedDir/WorkDir` (12절) |
| `.LogPath` | json-file 로그 파일 위치 (VM 내부) |

- `docker inspect <name>`은 컨테이너/이미지/볼륨 등에서 이름이 겹치면 모호할 수 있어 `--type container|image|volume|network`로 지정할 수 있다. `docker inspect --type image nginx:alpine`로 `RootFS.Layers` 개수(8)와 `Size`(68.5MB), `ExposedPorts`, `Entrypoint/Cmd`를 확인했다.
- 실무에서 "왜 이 설정으로 떴지?"를 확인하는 1순위가 inspect다. 특히 이미지의 `ENV`, `ENTRYPOINT`와 `docker run` 옵션이 병합된 결과를 본다.

#### 6. 네트워크 구조와 종류

##### 6-1. 기본 구조 (bridge)

컨테이너 안에서 본 네트워크 장치:

```
$ docker exec i1 ip addr
1: lo: <LOOPBACK,UP,LOWER_UP> ... inet 127.0.0.1/8
...
11: eth0@if87: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 65535 ...
    link/ether 26:a9:e0:c9:92:d4 ...  inet 172.17.0.2/16 ...
$ docker exec i1 ip route
default via 172.17.0.1 dev eth0
172.17.0.0/16 dev eth0 scope link  src 172.17.0.2
```

(`tunl0`, `gre0`, `sit0` 등 `DOWN` 상태의 터널 장치가 함께 보이는데, 사용되지 않는 장치이므로 무시해도 된다.)

VM(호스트 네트워크 네임스페이스) 쪽:

```
docker0  : 172.17.0.1/16   (브리지, 컨테이너의 기본 게이트웨이)
veth08f2a3e  ifindex=87    ← docker0에 연결(brctl show: docker0 → veth08f2a3e)
컨테이너 eth0@if87 ↔ 호스트 veth (ifindex 87)
$ docker exec i1 cat /sys/class/net/eth0/iflink   → 87
```

```
        컨테이너 i1 (net ns)                 VM(호스트 net ns)
   ┌──────────────────────┐          ┌──────────────────────────────┐
   │ lo      127.0.0.1    │          │  docker0 (bridge) 172.17.0.1  │
   │ eth0 172.17.0.2 ─────┼── veth ──┼─ vethXXXX (ifindex 87)        │
   └──────────────────────┘   pair   │        │                      │
                                     │   iptables NAT / ip_forward  │
                                     │        ▼                      │
                                     │      eth0 (VM 외부 NIC)       │
                                     └──────────────────────────────┘
```

| 장치 | 위치 | 역할 |
|---|---|---|
| `lo` | 컨테이너 | 컨테이너 내부 루프백. 컨테이너마다 독립이라 `localhost`는 **자기 자신**을 가리킨다 |
| `eth0` | 컨테이너 | 컨테이너 쪽 veth 끝. 이름은 `eth0`이지만 실제로는 veth 인터페이스. `@if87`은 짝(호스트 쪽 ifindex 87) |
| `vethXXXX` | 호스트 | veth pair의 호스트 쪽 끝. 컨테이너마다 하나 |
| `docker0` | 호스트 | 기본 브리지(L2 스위치 + 게이트웨이 `172.17.0.1`). veth들이 여기에 꽂힌다 |

- **veth**는 가상 이더넷 케이블이다. 한쪽에 들어간 패킷이 반대쪽으로 나온다. 한쪽 끝은 컨테이너 net ns, 다른 쪽은 호스트 net ns의 브리지에 연결된다. 컨테이너의 `eth0@if87`과 호스트의 `ifindex 87`이 짝임을 `iflink`로 확인했다.
- **외부로 나갈 때 (SNAT/MASQUERADE)**: `iptables -t nat`에서 확인한 규칙

```
-A POSTROUTING -s 172.17.0.0/16 ! -o docker0 -j MASQUERADE        # 컨테이너 → 외부: 소스 IP를 호스트 IP로 변환
$ docker exec b1 wget -qO- http://example.com | head -c 60         # 실제로 외부 접속 성공
$ cat /proc/sys/net/ipv4/ip_forward                                → 1
```

- **포트 퍼블리시 (DNAT)**: `-p 18080:80`이 만든 규칙

```
-A DOCKER -p tcp -m tcp --dport 18080 -j DNAT --to-destination 172.17.0.2:80
$ docker port i1                  → 80/tcp -> 0.0.0.0:18080
$ curl localhost:18080            → 200   (Mac 호스트에서)
```

  Mac에서는 Docker Desktop이 Mac의 `localhost:18080`을 VM으로 전달해 준다. Linux에서는 호스트 iptables가 직접 처리한다. 이 환경의 `daemon.json`은 `"userland-proxy": false`라서 `docker-proxy` 프로세스 대신 iptables DNAT로 동작한다.
- **컨테이너 간 통신(같은 브리지)**: 같은 브리지의 컨테이너끼리는 IP로 직접 통신한다 (L2 스위칭).

##### 6-2. 네트워크 종류

`docker info`에서 확인한 사용 가능 드라이버: `bridge host ipvlan macvlan null overlay`

| 종류 | 옵션 | 동작 | 용도 |
|---|---|---|---|
| **bridge** (기본) | `--network bridge` | veth + 브리지 + NAT. 컨테이너마다 독립 net ns와 IP | 일반적인 단일 호스트 |
| **user-defined bridge** | `docker network create` | bridge + **내장 DNS** + 네트워크 간 격리 | 사실상 권장되는 기본 |
| **host** | `--network host` | 컨테이너가 호스트 net ns를 공유. IP/포트 직접 사용 | 성능, 포트 많은 서비스 |
| **none** | `--network none` | `lo`만 있음 | 완전 격리, 네트워크 불필요한 배치 |
| **container** | `--network container:<name>` | 다른 컨테이너의 net ns를 공유 | 사이드카, 디버깅 |
| overlay | swarm/멀티호스트 | VXLAN으로 호스트 간 통신 | 3장(스웜) |
| macvlan / ipvlan | | 컨테이너가 물리 네트워크에 직접 IP를 가짐 | 레거시 연동 |

##### 6-3. 기본 bridge vs 사용자 정의 bridge

```
# 기본 bridge (docker0)
$ docker exec b1 ping -c1 -W1 b2                  → ping: bad address 'b2'        # 이름 해석 안 됨
$ docker exec b1 ping -c1 -W1 172.17.0.4          → 1 packets received            # IP로는 됨
$ docker exec b1 cat /etc/resolv.conf             → nameserver 192.168.65.7       # 호스트의 DNS를 그대로 사용

# 사용자 정의 bridge
$ docker network create net1                      → bridge  172.18.0.0/16  gw=172.18.0.1
$ docker run -d --name web --network net1 alpine:3.20 sleep 300
$ docker run -d --name cli --network net1 alpine:3.20 sleep 300
$ docker exec cli ping -c1 web                    → PING web (172.18.0.2) ...     # 이름으로 해석됨
$ docker exec cli cat /etc/resolv.conf            → nameserver 127.0.0.11         # 도커 내장 DNS

# 네트워크 간 격리
$ docker exec cli ping -c1 -W1 172.17.0.4         → 100% packet loss               # net1 → docker0 컨테이너 불가
```

- 기본 bridge에서는 컨테이너 이름으로 서로를 찾을 수 없다. 사용자 정의 bridge는 **컨테이너 이름과 alias를 내장 DNS(127.0.0.11)로 해석**한다.
- 서로 다른 브리지 간 통신은 차단된다 (격리).
- **한 컨테이너를 여러 네트워크에 연결**(multi-homed)할 수 있고 alias도 줄 수 있다.

```
$ docker network connect --alias backend net1 b1      # 인자 순서: [옵션] 네트워크 컨테이너
$ docker exec b1 ping -c1 web                         → 성공
$ docker exec cli ping -c1 backend                    → PING backend (172.18.0.4)   # alias로 해석
$ docker exec b1 ip -o addr                           → eth0 172.17.0.3/16, eth1 172.18.0.4/16
$ docker network disconnect net1 b1
```

##### 6-4. host / none / container

```
$ docker run --rm --network host alpine:3.20 sh -c 'hostname; ip -o addr | head'
docker-desktop                                  # VM의 호스트명
eth0 192.168.65.3/24, eth0 fdc4:..., services1 192.168.65.6/32 ...   # VM의 NIC들

$ docker run --rm --network none alpine:3.20 sh -c 'ip -o addr; ping -c1 -W1 8.8.8.8'
lo 127.0.0.1/8, lo ::1/128
ping: sendto: Network unreachable

$ docker run -d --name nginx2 nginx:alpine
$ docker run --rm --network container:nginx2 alpine:3.20 sh -c 'ip -o addr; wget -qO- localhost | head -3'
lo ..., eth0 172.17.0.5/16                      # nginx2와 같은 IP
<!DOCTYPE html> ...                              # localhost로 nginx2의 80포트에 접속됨
```

- **host**: 컨테이너가 호스트(여기서는 VM)의 `eth0`와 IP를 그대로 쓴다. NAT가 없어서 빠르고 `-p`가 필요 없고(무시됨), 포트 충돌을 직접 관리해야 한다. **Docker Desktop의 host 네트워크는 Mac이 아니라 Docker VM의 네트워크**다. Linux 서버에서만 "진짜 호스트 네트워크"다.
- **none**: `lo`만 있다. 외부 통신 불가 (`Network unreachable`).
- **container**: 다른 컨테이너의 네트워크 스택(IP, 포트, `lo`)을 공유한다. 같은 `localhost`를 쓴다. 쿠버네티스 파드의 컨테이너들이 네트워크를 공유하는 방식이 이것이다 (파드의 `pause` 컨테이너가 net ns를 쥐고 있다).

##### 6-5. 포트 퍼블리시 옵션

```
$ docker run -d --name pp -p 127.0.0.1:18081:80 -p 80 nginx:alpine
$ docker port pp
80/tcp -> 0.0.0.0:51775          # -p 80 : 호스트 포트를 임의 할당
80/tcp -> 127.0.0.1:18081        # 특정 호스트 IP에만 바인딩

$ docker run -d --name P -P nginx:alpine    # -P : EXPOSE 된 모든 포트를 임의 호스트 포트로
$ docker port P                 → 80/tcp -> 0.0.0.0:55001
```

- `-p`의 기본 바인딩은 `0.0.0.0`(전체)이다. 개발 DB를 `-p 3306:3306`으로 띄우면 외부 네트워크에서도 접근된다. 로컬 전용이면 `127.0.0.1:`을 붙인다.
- Dockerfile의 `EXPOSE`는 **문서화일 뿐 포트를 열지 않는다.** `-P`를 쓸 때만 의미가 있다.
- 그 밖에 `--hostname`, `--add-host db:10.9.9.9`(`/etc/hosts` 추가), `--dns 1.1.1.1`을 확인했다. `--add-host`는 `/etc/hosts`에 `10.9.9.9 db`로, `--dns`는 `resolv.conf`의 `nameserver 1.1.1.1`로 반영됐다.

#### 7. 컨테이너 자원 할당과 제한

##### 7-1. 옵션과 cgroup v2 파일 매핑 (실측)

```
$ docker run -d --name lim --memory 100m --memory-swap 100m --memory-reservation 50m \
    --cpus 1.5 --cpu-shares 512 --pids-limit 50 alpine:3.20 sleep 300
```

| 도커 옵션 | 의미 | cgroup v2 파일 | 확인한 값 |
|---|---|---|---|
| `--memory`, `-m` | 메모리 하드 한도 | `memory.max` | `104857600` |
| `--memory-reservation` | 소프트 한도(경합 시 회수 기준) | `memory.low` | `52428800` |
| `--memory-swap` | 메모리+스왑 합계 | `memory.swap.max` | `0` (`-m`과 같게 줘서 스왑 0) |
| `--cpus 1.5` | CPU 시간 상한 | `cpu.max` | `150000 100000` (100ms 주기에 150ms) |
| `--cpu-shares 512` | 경합 시 상대 가중치 | `cpu.weight` | `20` (기본 1024 → 100, v2 값으로 변환) |
| `--pids-limit` | 프로세스/스레드 수 | `pids.max` | `50` |
| `--cpuset-cpus 0,1` | 사용할 CPU 코어 고정 | `cpuset.cpus.effective` | `0-1` (`nproc`도 2) |
| `--ulimit nofile=256:256` | rlimit | (cgroup 아님) | `ulimit -n` → 256 |
| `--shm-size 16m` | `/dev/shm` 크기 | (tmpfs 크기) | `df /dev/shm` → 16.0M |

**메모리 스왑 의미** (cgroup v2에서 `memory.swap.max`는 "스왑만"의 값)

```
$ docker run --rm -m 50m alpine:3.20 cat /sys/fs/cgroup/memory.swap.max
52428800                      # --memory-swap 생략 → 스왑도 메모리와 같은 크기 허용(메모리+스왑 = 2배)
$ docker run --rm -m 50m --memory-swap -1 alpine:3.20 cat /sys/fs/cgroup/memory.swap.max
max                           # 스왑 무제한
```

`-m`만 주면 스왑이 같은 크기로 허용되므로 메모리를 엄격히 제한하려면 `--memory-swap`을 `-m`과 같게 준다.

##### 7-2. 동작 확인

**CPU 상한(throttling)**: 쉘 무한 루프를 `--cpus 0.25`로 실행

```
$ docker stats --no-stream --format 'cpu={{.CPUPerc}}'  → cpu=25.17%
$ cat /sys/fs/cgroup/cpu.stat
nr_periods 59
nr_throttled 58
throttled_usec 4380359
```

59개 주기 중 58번 throttle되며 CPU가 0.25코어로 고정됐다. 상한을 넘으면 **죽지 않고 느려진다.** CPU 제한이 응답 지연으로 나타나는 이유다.

**CPU 가중치(cpu-shares)**: 같은 코어 하나(`--cpuset-cpus 0`)에 busy loop 두 개

```
$ --cpu-shares 2048 : 79.47%
$ --cpu-shares  512 : 20.32%          # 2048:512 = 4:1 비율로 분배
```

가중치는 **경합할 때만** 의미가 있고 한가할 때는 남는 자원을 쓸 수 있다. 반면 `--cpus`는 한가해도 상한이 걸린다.

**메모리 상한(OOM)**: 상한을 넘으면 커널 OOM killer가 프로세스를 죽인다.

```
$ docker run --name oom -m 20m --memory-swap 20m alpine:3.20 sh -c 'head -c 60m /dev/zero | tail >/dev/null'
Killed
$ docker inspect oom --format 'OOMKilled={{.State.OOMKilled}} exit={{.State.ExitCode}}'
OOMKilled=true exit=137
```

CPU는 "느려짐", 메모리는 "종료"가 기본 동작이다.

**실행 중 변경 `docker update`**

```
$ docker update --memory 200m --memory-swap 200m --cpus 0.5 lim
$ cat /sys/fs/cgroup/memory.max /sys/fs/cgroup/cpu.max
209715200
50000 100000                  # 재시작 없이 cgroup 값이 바뀜
```

**미지원 옵션**: `--blkio-weight 300`은 이 환경에서 `openat2 .../io.weight: no such file or directory`로 컨테이너 생성이 실패했다. VM 커널/cgroup 설정에 `io` 컨트롤러가 없기 때문으로 보인다. 같은 옵션이 Linux 서버에서는 동작할 수 있다 (확인하지 못함).

##### 7-3. 그 밖의 런타임 제한 옵션

```
$ docker run --rm --read-only alpine:3.20 sh -c 'touch /x'            → Read-only file system
$ docker run --rm --read-only --tmpfs /tmp alpine:3.20 sh -c 'touch /tmp/x && echo ok'  → ok
$ docker run --rm -u 1000:1000 alpine:3.20 id                          → uid=1000 gid=1000
$ docker run --rm --init alpine:3.20 ps                                → PID 1: /sbin/docker-init -- ps
```

`--init`은 `tini`류의 초기화 프로세스(`docker-init`)를 PID 1로 넣어 좀비 프로세스를 정리하고 시그널을 전달한다.

> 1장에서 확인한 대로 컨테이너 안의 `free`, `top`은 제한값이 아니라 VM 전체를 보여준다. 제한 확인은 `docker stats`의 `MEM USAGE / LIMIT` 또는 cgroup 파일로 한다. `docker stats`에서 제한이 없는 컨테이너의 LIMIT은 VM 총 메모리(7.653GiB)로 나왔다.

#### 8. 이미지 레이어

##### 8-1. 레이어 구조

```
$ docker history t2-img:1 --format '{{.Size}}\t{{.CreatedBy}}'
0B     CMD ["cat" "/app/built.txt"]
0B     USER nobody
0B     EXPOSE map[8080/tcp:{}]
0B     LABEL purpose=demo
0B     RUN rm /app/big
21MB   RUN dd if=/dev/zero of=/app/big bs=1M count=20
8B     RUN echo "build-$VER" > /app/built.txt && sleep 1
2B     COPY a.txt .
0B     WORKDIR /app
0B     ENV APP_ENV=prod
0B     ARG VER=1
0B     CMD ["/bin/sh"]                       ← 베이스 이미지(alpine)의 설정
8.82MB ADD alpine-minirootfs-3.20.10-aarch64.tar.gz /

$ docker image inspect t2-img:1 --format 'layers={{len .RootFS.Layers}} size={{.Size}}'
layers=6 size=29794227
```

- 이미지 = **읽기 전용 레이어의 스택 + 설정(JSON)**. 각 레이어는 파일시스템 변경분(diff) tar이고, 내용 해시(sha256)로 식별된다.
- 파일시스템을 바꾸는 명령(`FROM`의 베이스, `RUN`, `COPY`, `ADD`, `WORKDIR`가 디렉터리를 만들 때)만 레이어가 된다. 레이어가 총 6개로, 베이스 1 + `WORKDIR` 1 + `COPY` 1 + `RUN` 3이다. `ENV`, `ARG`, `LABEL`, `EXPOSE`, `USER`, `CMD`는 **설정 메타데이터만** 바꾸고 레이어는 만들지 않는다.
- 컨테이너를 실행하면 이미지 레이어 위에 얇은 **쓰기 가능 레이어**(container layer) 하나가 붙는다. `docker ps -s`에서 `size=652B (virtual 68.5MB)`처럼 컨테이너 자체 변경분(652B)과 이미지 포함 크기(virtual)가 구분되어 나온다.

##### 8-2. 레이어 공유

- 같은 베이스 레이어는 이미지 간에, 그리고 컨테이너 간에 **한 번만 저장되고 공유**된다. 실측에서 `nginx:alpine`을 pull할 때 첫 레이어가 `a9986cd6f37d: Already exists`(이미 있는 alpine 베이스)로 건너뛰어졌다.
- 레이어 다운로드는 병렬로 진행되고 해시 검증 후 풀린다 (`Pulling fs layer → Download complete → Pull complete`).

##### 8-3. 삭제해도 이미지는 줄지 않는다

```
RUN dd if=/dev/zero of=/app/big bs=1M count=20    ← 21MB 레이어
RUN rm /app/big                                   ← 0B 레이어 (삭제 표시만 추가)
→ 최종 이미지 29.8MB (= 8.82MB + 21MB)
```

레이어는 **불변**이라 뒤 레이어에서 파일을 지워도 앞 레이어의 데이터는 그대로 남는다. 삭제는 "whiteout" 표시로만 기록된다 (12절에서 직접 확인). 용량을 줄이려면 **같은 `RUN` 안에서 만들고 지워야** 한다 (`RUN apt-get install ... && rm -rf /var/lib/apt/lists/*`). 또는 멀티 스테이지 빌드를 쓴다 (9절).

##### 8-4. 레이어 캐시와 순서

- 레이어는 빌드 캐시의 단위이기도 하다. 자주 바뀌는 것(소스 코드)은 아래로, 잘 안 바뀌는 것(의존성 설치)은 위로 두는 것이 원칙이다 (9절 실측).

#### 9. Dockerfile과 빌드 과정

##### 9-1. 주요 명령어

| 명령 | 설명 | 레이어 | 비고 |
|---|---|---|---|
| `FROM image[:tag] [AS name]` | 베이스 이미지, 멀티 스테이지의 단계 이름 | 베이스 | 모든 Dockerfile의 시작 (`ARG`만 앞에 가능) |
| `ARG name[=default]` | **빌드 시** 변수 (`--build-arg`) | X | 런타임에는 없다. 이미지 히스토리에 남아 비밀 전달에 부적합 |
| `ENV k=v` | 환경 변수 (빌드 + 런타임) | X | `docker run -e`로 덮어쓰기 가능 |
| `WORKDIR path` | 작업 디렉터리 (없으면 생성) | O(디렉터리 생성) | `RUN cd`는 다음 명령에 유지되지 않으므로 `WORKDIR`를 쓴다 |
| `COPY src dest` | 빌드 컨텍스트의 파일 복사 | O | 단순 복사. `--from=stage`로 다른 스테이지에서 복사 |
| `ADD src dest` | `COPY` + tar 자동 압축 해제 + URL 다운로드 | O | 특별한 이유가 없으면 `COPY` 권장 |
| `RUN cmd` | 빌드 중 명령 실행 | O | shell form(`/bin/sh -c`) / exec form |
| `USER name` | 이후 명령과 컨테이너 실행 사용자 | X | root로 돌리지 않기 |
| `EXPOSE port` | 사용 포트 문서화 | X | 실제로 포트를 열지 않는다 |
| `VOLUME path` | 익명 볼륨 지점 선언 | X | 4-2절 부작용 |
| `LABEL k=v` | 메타데이터 | X | |
| `CMD` / `ENTRYPOINT` | 컨테이너 시작 명령 | X | 10절 |
| `HEALTHCHECK` | 헬스체크 정의 | X | 13절 |
| `SHELL`, `STOPSIGNAL`, `ONBUILD` | 쉘 변경, 종료 시그널, 자식 빌드 트리거 | X | (이 노트에서 실행 검증하지 않음) |

**`COPY` vs `ADD`** (실측)

```dockerfile
ADD a.tgz /extracted/
COPY a.tgz /copied/a.tgz
```

```
$ docker run --rm img
/copied:    a.tgz                # COPY: 압축 파일 그대로
/extracted: f.txt                # ADD : tar를 자동으로 풀어 줌
```

##### 9-2. 빌드 과정 (BuildKit 기준)

이 환경은 BuildKit이 기본 빌더다 (`docker buildx version`: v0.22.0-desktop.1, `docker builder ls`: `default` 드라이버, BuildKit v0.20.2).

```
docker build .  (CLI)
   │  ① Dockerfile + 빌드 컨텍스트를 빌더(BuildKit)에 전달
   ▼
BuildKit
   ② Dockerfile 파싱 → 단계(stage)/명령을 의존 그래프(DAG, LLB)로 변환
   ③ 각 단계마다 캐시 키 계산 (부모 레이어 + 명령 문자열 + COPY/ADD 대상 파일 체크섬 + ARG 값)
        └ 일치하는 캐시가 있으면 CACHED, 없으면 실행
   ④ 실행: 이전 레이어 위에 임시 컨테이너(runc)를 만들어 RUN 실행 → 변경분(diff)을 새 레이어로 저장
        COPY/ADD는 파일을 복사한 diff가 레이어, 메타 명령은 이미지 설정만 변경
   ⑤ 최종 레이어들 + 설정(JSON) + 매니페스트로 이미지 생성, 태그 부여 (writing image, naming to ...)
```

관찰한 빌드 로그:

```
#4 [1/6] FROM docker.io/library/alpine:3.20
#6 [2/6] WORKDIR /app
#7 [3/6] COPY a.txt .
#8 [4/6] RUN echo "build-1" > /app/built.txt && sleep 1       DONE 1.1s
#9 [5/6] RUN dd if=/dev/zero of=/app/big bs=1M count=20 ...   DONE 0.1s
#10 [6/6] RUN rm /app/big
#11 writing image sha256:157cca74...  / naming to docker.io/library/t2-img:1
```

- 각 `RUN`은 **그 시점의 새 컨테이너**에서 실행된다. 이전 `RUN`에서 실행한 프로세스, `cd`, `export`는 다음 `RUN`으로 이어지지 않는다 (파일시스템 변경분만 이어진다).
- 빌드 로그의 단계 번호가 `#6 [3/6] ... #8 [2/6]` 식으로 뒤섞여 나오는 것은 BuildKit이 의존 그래프를 병렬로 처리하기 때문이다 (캐시 조회 순서에서 관찰).
- BuildKit은 **컨텍스트를 필요한 파일만 전송**한다. 컨텍스트에 30MB 더미 파일을 두고 빌드해도 `transferring context: 63B`였다. (옛 빌더는 컨텍스트 전체를 데몬에 보냈다.) 그래도 `.dockerignore`로 `.git`, `node_modules` 등을 제외하는 것이 `COPY . .`의 캐시 안정성과 이미지 크기에 좋다.

##### 9-3. 캐시 동작 (실측)

```
① 같은 Dockerfile로 재빌드 → 모든 단계가 CACHED

② a.txt 내용 변경
#6 [2/6] WORKDIR /app           CACHED
#7 [3/6] COPY a.txt .           ← 파일 체크섬이 달라 무효화
#8 [4/6] RUN echo ...           ← 이후 모든 단계가 다시 실행
#9 [5/6] RUN dd ...
#10 [6/6] RUN rm ...

③ --build-arg VER=9 (ARG 값 변경)
#6 [2/6] WORKDIR /app           CACHED
#7 [3/6] COPY a.txt .           CACHED       ← ARG를 쓰기 전 단계는 캐시 유지
#8 [4/6] RUN echo "build-9" ... ← ARG를 사용하는 RUN부터 무효화, 이후도 재실행
```

규칙: **한 단계가 무효화되면 그 뒤 모든 단계가 무효화된다.** 그래서 `package.json`만 먼저 `COPY` → `npm install` → 소스 `COPY` 순서로 쓴다.

**캐시의 함정**: `RUN` 명령은 **명령 문자열이 같으면** 결과가 달라질 수 있어도 캐시를 재사용한다.

```dockerfile
RUN date +%s%N > /stamp
```
```
$ docker build -t stamp . ; docker run --rm stamp   → 1792417138
$ sleep 1; docker build -t stamp . ; docker run --rm stamp   → 1792417138   (캐시: 같은 값)
$ docker build --no-cache -t stamp . ; docker run --rm stamp → 1792417139   (새로 실행)
```

`apt-get update`, `git clone` 같은 명령이 오래된 캐시로 남는 문제의 원리다. 최신 상태가 필요하면 `--no-cache`나 `ARG`로 캐시 키를 바꾼다.

##### 9-4. 멀티 스테이지 빌드

```dockerfile
FROM alpine:3.20 AS build
RUN dd if=/dev/zero of=/big bs=1M count=50 2>/dev/null && echo ok > /result
FROM alpine:3.20
COPY --from=build /result /result
CMD ["cat","/result"]
```
```
$ docker image ls ms → size=8.82MB        # 50MB짜리 build 스테이지는 최종 이미지에 포함되지 않음
```

빌드 도구/중간 산출물이 있는 스테이지를 버리고 결과물만 복사한다. 8-3절의 "삭제해도 줄지 않는다" 문제를 구조적으로 해결하는 방법이다.

#### 10. ENTRYPOINT vs CMD

| | `ENTRYPOINT` | `CMD` |
|---|---|---|
| 역할 | 컨테이너의 **고정된 실행 파일** | 실행 파일에 넘길 **기본 인자** (또는 기본 명령) |
| `docker run image [args]`의 args | **ENTRYPOINT 뒤에 덧붙음** | **CMD 전체를 대체** |
| 덮어쓰기 | `--entrypoint`로만 | `docker run` 뒤에 쓴 명령으로 |
| 둘 다 있을 때 | `ENTRYPOINT` + `CMD`가 이어 붙은 것이 최종 명령 | |

실측 (모두 exec form):

```
CMD ["echo","cmd-default"]
  docker run img            → cmd-default
  docker run img hello      → 에러: exec: "hello": executable file not found in $PATH
                              (CMD 전체가 "hello"로 대체되어 실행 파일로 해석됨)

ENTRYPOINT ["echo","ep"]
  docker run img            → ep
  docker run img hello      → ep hello                       (인자가 덧붙음)

ENTRYPOINT ["echo","ep"] + CMD ["cmd-default"]
  docker run img            → ep cmd-default
  docker run img hello      → ep hello                       (CMD만 대체)
  docker run --entrypoint echo img overridden-ep → overridden-ep
```

**조합 매트릭스 (최종 실행 명령)**

| | ENTRYPOINT 없음 | `ENTRYPOINT ["e"]` |
|---|---|---|
| **CMD 없음** | 에러/베이스 이미지 상속 | `e` |
| **`CMD ["c"]`** | `c` | `e c` |

**shell form vs exec form**

| | exec form `["a","b"]` | shell form `a b` |
|---|---|---|
| 실제 실행 | `a b` 를 직접 실행 | `/bin/sh -c "a b"` |
| PID 1 | 해당 프로세스 | 쉘 (`sh`) |
| 변수 확장(`$HOME`) | 안 됨 (쉘이 없음) | 됨 |
| 시그널 전달 | 앱이 직접 받음 | 쉘이 받고 자식에게 전달 안 할 수 있음 |
| ENTRYPOINT에서 인자 | 받음 | **무시됨** |

```
$ docker inspect img --format 'ep={{json .Config.Entrypoint}}'
ep=["/bin/sh","-c","echo shell-ep"]        # shell form이 이렇게 변환됨
$ docker run --rm img-shell-ep extra-arg   → shell-ep      # "extra-arg"는 무시됨 (sh -c "..." 뒤의 인자가 되어 쓰이지 않음)
$ docker run --rm img-shell-cmd            → shell-cmd /root   # shell form CMD는 $HOME이 확장됨

$ docker run -d alpine:3.20 sh -c 'sleep 300; true'
  PID 1: sh -c sleep 300; true      PID 7: sleep 300        # 쉘이 PID 1, 앱은 자식
```

- shell form이면 PID 1이 쉘이라 `docker stop`의 `SIGTERM`이 앱에 전달되지 않을 수 있다 → graceful shutdown이 안 되고 10초 뒤 `SIGKILL` (1절). 서비스는 **exec form**을 쓴다.
- (참고) `sh -c 'sleep 300'`처럼 쉘 명령이 하나뿐이면 BusyBox `sh`는 자식을 만들지 않고 `exec`해서 PID 1이 `sleep`이었다. 쉘 구현에 따라 다를 수 있다.
- 베이스 이미지의 설정은 상속된다. 아무것도 지정하지 않은 이미지(`FROM alpine:3.20`만)는 `ep=[] cmd=[/bin/sh]`이고, 터미널이 없으면 `docker run`하자마자 쉘이 종료된다 (`rc=0`).

사용 패턴:
- **앱을 실행 파일처럼 쓰는 이미지**(CLI 도구): `ENTRYPOINT ["tool"]` + `CMD ["--help"]` → `docker run img subcommand`
- **서비스 이미지**: `CMD ["nginx","-g","daemon off;"]` 정도, 래퍼 스크립트가 필요하면 `ENTRYPOINT ["/docker-entrypoint.sh"]` + `CMD`. nginx 이미지가 이 구조다 (`Entry=[/docker-entrypoint.sh] Cmd=[nginx -g daemon off;]`).
- 래퍼 스크립트에서는 마지막에 `exec "$@"`로 앱으로 PID 1을 넘겨야 시그널이 전달된다.

#### 11. 도커 데몬

##### 11-1. 구조

도커는 **클라이언트-서버** 구조다. `docker` CLI는 데몬(`dockerd`)의 REST API를 호출하는 클라이언트일 뿐이다.

```
$ docker run --rm --pid=host alpine:3.20 sh -c 'ps -eo pid,args | grep -E "dockerd|containerd$"'
328  /usr/local/bin/dockerd --config-file /run/config/docker/daemon.json \
       --containerd /run/containerd/containerd.sock --pidfile /run/desktop/docker.pid ...
1212 /usr/local/bin/containerd
```

| 구성요소 | 역할 |
|---|---|
| `docker` CLI | 사용자 명령 → API 호출. 소켓(Mac: `~/.docker/run/docker.sock`)으로 통신 |
| `dockerd` | API 서버. 이미지 빌드, 네트워크, 볼륨, 로그 드라이버 관리 |
| `containerd` | 컨테이너 수명주기와 이미지/스냅샷 관리 |
| `containerd-shim` → `runc` | 컨테이너 프로세스 생성 (1장) |

- 데몬이 **root 권한**으로 돌고 소켓 접근 권한 = 사실상 호스트 root 권한이다. `docker` 그룹 멤버나 `docker.sock`을 컨테이너에 마운트하는 것은 root 권한 부여와 같다.
- `-H tcp://...`로 TCP에 열 때는 TLS 없이 열면 안 된다 (이 환경에서 실행하지 않음, 개념).

##### 11-2. 주요 정보 (`docker info`)

```
Server Version: 28.0.4
Storage Driver: overlay2          Logging Driver: json-file
Cgroup Driver: cgroupfs           Cgroup Version: 2
Runtimes: io.containerd.runc.v2 runc   Default Runtime: runc
Init Binary: docker-init          Live Restore Enabled: false
Docker Root Dir: /var/lib/docker  Swarm: inactive
Plugins: Volume: local / Network: bridge host ipvlan macvlan null overlay
containerd version: 753481ec...   runc version: v1.2.5
```

##### 11-3. 설정 (`daemon.json`)

이 Docker Desktop VM에서 읽은 데몬 설정:

```json
{"builder":{"gc":{"defaultKeepStorage":"20GB","enabled":true}},
 "experimental":false, "features":{"cdi":true},
 "hosts":["fd://"],
 "insecure-registries":["hubproxy.docker.internal:5555"],
 "mtu":65535, "seccomp-profile":"unconfined", "userland-proxy":false}
```

- `seccomp-profile: unconfined`: 1장에서 `Seccomp: 0`이던 이유가 이것이다.
- `userland-proxy: false`: 포트 퍼블리시를 `docker-proxy` 대신 iptables로 처리 (6절).
- `builder.gc`: 빌드 캐시 자동 정리 (최대 20GB 유지).
- `hosts: ["fd://"]`: systemd/소켓 활성화로 받은 파일 디스크립터에서 listen.
- Linux 서버에서는 보통 `/etc/docker/daemon.json`을 수정하고 `systemctl restart docker`한다. `live-restore: true`를 주면 데몬 재시작 중에도 컨테이너가 유지된다 (이 환경에서는 `false`로 확인했을 뿐 동작을 실험하지는 않았다).

##### 11-4. 데이터 디렉터리

```
/var/lib/docker (VM 내부)
├── containers/   컨테이너별 디렉터리: config.v2.json, hostconfig.json, <id>-json.log
├── image/        이미지 메타데이터
├── overlay2/     레이어 데이터 (스토리지 드라이버)
├── volumes/      named volume
├── network/      네트워크 설정
├── buildkit/     빌드 캐시
└── ...
```

- 컨테이너 로그(`<id>-json.log`)가 `containers/` 아래에 있어서, 로그가 쌓이면 이 디렉터리가 커진다 (13절).
- 정리: `docker system df`로 종류별(Images/Containers/Volumes/Build Cache) 크기와 회수 가능량을 보고, `docker system prune`으로 정리. **`-a`, `--volumes` 옵션은 사용 중이 아닌 이미지/볼륨까지 지운다.**

#### 12. 스토리지 드라이버

##### 12-1. overlay2 동작 (실측)

```
$ docker inspect i1 --format '{{json .GraphDriver.Data}}'
{ ID, LowerDir: <9개 레이어>, MergedDir, UpperDir, WorkDir }
```

- `LowerDir`: 읽기 전용 레이어들(nginx 이미지 8개 + 컨테이너 init 레이어로 보이는 1개, 합쳐서 9개). `UpperDir`: 컨테이너 쓰기 레이어. `MergedDir`: 합쳐진 뷰(컨테이너가 보는 `/`). `WorkDir`: overlayfs 내부 작업용.

컨테이너에서 파일 추가/수정/삭제 후 `UpperDir`을 확인:

```
$ docker exec i1 sh -c 'echo new > /newfile; echo x >> /etc/nginx/nginx.conf; rm /etc/nginx/conf.d/default.conf'

UpperDir:  data  etc  newfile  run  var

$ ls -la UpperDir/etc/nginx/conf.d/
c---------  2 root root  0, 0  default.conf          ← 삭제 = whiteout (문자 장치 0,0)
$ stat -c '%F %t:%T' UpperDir/etc/nginx/conf.d/default.conf
character special file 0:0
$ ls -la UpperDir/etc/nginx/nginx.conf
-rw-r--r-- 1 root root 646 ...                        ← 수정 = 원본 전체가 복사(copy-up)된 뒤 수정
```

| 동작 | overlay2에서 일어나는 일 |
|---|---|
| 읽기 | 위 레이어부터 찾아 첫 번째 것을 반환 (lower에 있으면 그대로 읽음) |
| 새 파일 쓰기 | `UpperDir`에 생성 |
| 기존 파일 수정 | **copy-up**: 파일 전체를 `UpperDir`로 복사한 뒤 수정 (큰 파일을 조금 고쳐도 전체 복사가 일어남) |
| 파일 삭제 | `UpperDir`에 **whiteout**(문자 장치 `0/0`) 생성. 하위 레이어의 파일은 그대로 있음 |
| 디렉터리 삭제 | opaque 디렉터리 표시 (이번 실험에서는 확인 안 함) |

`docker diff`의 `A/C/D`는 이 `UpperDir` 내용을 해석한 결과다. 이 때문에 레이어 내 파일을 지워도 이미지 크기가 줄지 않는다 (8-3절).

**쓰기 성능**: 컨테이너 레이어에 대량 쓰기는 copy-up/UnionFS 오버헤드가 있으므로 DB 데이터 같은 쓰기 집중 디렉터리는 **볼륨**을 쓴다 (4절). (성능 차이는 측정하지 않았다)

##### 12-2. overlay2 vs vfs 직접 비교

Docker-in-Docker(`docker:28-dind`)를 `--storage-driver overlay2`와 `vfs`로 각각 띄워서 같은 이미지(alpine + 20MB 파일 + 작은 파일 두 개, 4레이어)를 빌드하고 디스크 사용량을 비교했다.

| | overlay2 | vfs |
|---|---|---|
| 레이어 저장 | 각 레이어는 **diff만** 저장, overlayfs로 합침 | 각 레이어를 **통째로 복사**해서 저장 |
| 빌드 직후 스토리지 디렉터리 크기 | **30MB** | **97MB** |
| 컨테이너 2개 실행 후 | **30MB** (diff 디렉터리 합계, 컨테이너 쓰기 레이어는 사실상 0) | **213MB** (컨테이너당 약 58MB씩 전체 복사) |
| `docker system df` 이미지 크기 | 29.79MB | 29.79MB (논리 크기는 같음) |
| 디렉터리 수 | `overlay2/` 11개 + `l/` 심볼릭 링크 10개 | `vfs/dir/` 10개 |

- 논리 크기는 같지만 vfs는 **레이어 수와 컨테이너 수에 비례해 디스크를 낭비**한다. 컨테이너 시작도 복사 때문에 느려진다 (시간은 측정 안 함).
- 참고: overlay2 쪽을 `du`로 잴 때 실행 중 컨테이너의 `merged` 마운트까지 세어져서 값이 부풀어 보였다. 레이어 실제 용량은 `*/diff`만 합산했다.

##### 12-3. 드라이버 종류와 특징

| 드라이버 | 방식 | 특징 | 비고 |
|---|---|---|---|
| **overlay2** | 파일 단위 OverlayFS | 기본값, 안정적, 성능/메모리 효율 좋음. 파일 단위 copy-up | 현재 권장. 리눅스 커널 4.0+ |
| overlay | 구버전 overlayfs | inode 소모 등 한계 | overlay2로 대체 |
| **fuse-overlayfs** | FUSE 기반 overlay | rootless 모드에서 커널 overlay를 못 쓸 때 | 느림 |
| **btrfs / zfs** | 파일시스템의 스냅샷/서브볼륨 | 블록(또는 파일시스템) 수준 CoW. 해당 FS 위에서만 가능. 기능 풍부(스냅샷, 쿼터) | 운영 복잡도 |
| devicemapper | 블록 단위 thin provisioning | 과거 RHEL/CentOS 기본값 | **deprecated** |
| aufs | 구형 union FS | 예전 Ubuntu 기본값 | 제거됨 |
| **vfs** | 레이어마다 전체 복사 | CoW 없음. 어디서나 동작, 가장 느리고 용량 낭비 | 테스트/특수 환경, 위에서 실측 |
| windowsfilter | Windows 컨테이너 | Windows 전용 | |

- 같은 드라이버를 쓰더라도 동작 상세(파일 단위 vs 블록 단위 CoW)에 따라 큰 파일 일부 수정 시 성능이 갈린다. 파일 단위(overlay2)는 파일 전체 copy-up이 발생하고, 블록 단위(btrfs/zfs/devicemapper)는 변경된 블록만 복사한다 (개념 설명이며 직접 비교하지 않았다).
- 확인 방법: `docker info | grep "Storage Driver"`. 변경은 `daemon.json`의 `storage-driver`. **드라이버를 바꾸면 기존 이미지/컨테이너가 보이지 않게 되므로** 신중해야 한다.
- 최근 Docker는 containerd 이미지 스토어(snapshotter)로 옮겨가는 중이다 (이 환경은 클래식 `overlay2`로 확인되었고, containerd 스냅샷터는 직접 확인하지 않았다).

#### 13. 모니터링 명령어

##### 13-1. `docker stats`

```
$ docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}\t{{.PIDs}}'
NAME     CPU %   MEM USAGE / LIMIT     MEM %   NET I/O        BLOCK I/O        PIDS
mon      0.00%   11.02MiB / 7.653GiB   0.14%   740B / 84B     1.11MB / 8.19kB  13
lim      0.00%   532KiB / 200MiB       0.26%   3.77kB / 126B  0B / 0B          1
cpu      24.55%  560KiB / 7.653GiB     0.01%   4.23kB / 126B  0B / 0B          1     # --cpus 0.25
```

- 컨테이너별 CPU/메모리/네트워크/블록 I/O/PID 수를 cgroup 통계로 보여준다. 기본은 실시간 갱신, `--no-stream`은 1회 출력.
- `CPU %`는 코어 1개를 100%로 본 값이라 멀티 코어면 100%를 넘을 수 있다. 메모리 LIMIT은 제한이 없으면 VM 전체 메모리다.
- **한계**: 호스트/컨테이너 단위 현재값만 보여준다. 이력과 알림이 필요하면 cAdvisor + Prometheus + Grafana 같은 별도 도구 (이번에는 실행하지 않음).

##### 13-2. `docker top`, `ps`, `diff`, `port`

```
$ docker top mon
UID   PID    PPID   C  STIME  TTY  TIME      CMD
root  29816  29793  0  13:40  ?    00:00:00  nginx: master process nginx -g daemon off;
statd 29851  29816  0  13:40  ?    00:00:00  nginx: worker process  (x12)
$ docker top mon -eo pid,ppid,user,comm      # ps 옵션 전달 가능
```

- `top`의 PID는 **VM(호스트) 기준 PID**다 (inspect의 `.State.Pid`와 같은 계열). 컨테이너 안에서 보는 PID와 다르다.
- nginx worker의 사용자가 `statd`로 표시됐다. 컨테이너 안에서는 `nginx` 사용자(UID 101)지만 `docker top`은 호스트(VM) 쪽 `/etc/passwd`로 UID를 이름으로 바꿔서 보여주기 때문으로 보인다 (원인은 직접 확인하지 못했다). **이름이 아니라 UID로 판단**해야 한다.

```
$ docker ps --filter label=tier=test --format '{{.Names}} {{.Image}} {{.Status}} {{.Ports}}'
$ docker ps -s --filter name=i1 --format '{{.Names}} size={{.Size}}'
i1 size=652B (virtual 68.5MB)
```

`docker ps`의 `--filter`(name, label, status, ancestor, health), `--format`, `-s`(쓰기 레이어 크기)가 유용하다.

##### 13-3. `docker logs`와 로그 드라이버

```
$ docker logs --tail 2 log           → line4 / err4              # stdout/stderr 모두
$ docker logs -t --since 3s log      → 2026-10-19T13:40:57.767435388Z line3   # -t: 타임스탬프
$ docker inspect log --format '{{.HostConfig.LogConfig.Type}} {{json .HostConfig.LogConfig.Config}}'
json-file {}                                                      # 기본: json-file, 옵션 없음(=로테이션 없음)
```

- 기본 `json-file` 드라이버는 `max-size` 기본값이 **무제한**이다. 장기 실행 컨테이너의 로그가 디스크를 채울 수 있다.

```
$ docker run -d --log-opt max-size=10k --log-opt max-file=3 alpine:3.20 sh -c 'yes hello | head -n 20000'
<id>-json.log    4838
<id>-json.log.1  10070
<id>-json.log.2  10061        # 10KB 단위로 로테이션, 최대 3개 파일 유지
```

```
$ docker run --name lognone --log-driver none alpine:3.20 echo hi     → hi (출력은 됨)
$ docker logs lognone → Error response from daemon: configured logging driver does not support reading
```

- `--log-driver none`은 로그를 저장하지 않아 `docker logs`로 못 읽는다. 중앙 로깅은 `fluentd`, `syslog`, `gelf`, `awslogs` 등 드라이버나 앱이 stdout을 수집기(Loki 등)로 보내는 방식으로 한다. 전역 설정은 `daemon.json`의 `log-driver`/`log-opts`.
- 도커는 **앱이 stdout/stderr로 출력**한 것만 수집한다. 파일 로그만 쓰는 앱은 로그 파일을 stdout으로 연결하거나 볼륨으로 뺀다.

##### 13-4. `docker events`

```
$ docker events --filter container=ev --format '{{.Time}} {{.Type}} {{.Action}}'
… container create / attach / start / die / destroy

$ docker events --since 30s --until 0s --filter event=die --filter event=oom \
    --format '{{.Type}} {{.Action}} exit={{.Actor.Attributes.exitCode}} {{.Actor.Attributes.name}}'
container die exit=0 t2-log
```

- 데몬이 발생시키는 이벤트(생성/시작/종료/OOM/네트워크 연결 등)를 스트림으로 본다. `die` 이벤트에 `exitCode`가 담기고 OOM이면 `oom` 이벤트가 함께 발생한다.
- 컨테이너 종료 원인 추적(`inspect`의 `.State.ExitCode`, `.State.OOMKilled`)과 같이 쓴다. 자동 알림/모니터링 도구가 이 스트림을 사용한다.

##### 13-5. 헬스체크

```
$ docker run -d --name hc --health-cmd 'wget -qO- localhost || exit 1' --health-interval 2s --health-retries 2 nginx:alpine
$ docker inspect hc --format '{{.State.Health.Status}} fail={{.State.Health.FailingStreak}}'
healthy fail=0
$ docker ps --filter name=hc --format '{{.Status}}'      → Up 5 seconds (healthy)

$ docker run -d --name hc2 --health-cmd 'exit 1' --health-interval 1s --health-retries 2 alpine:3.20 sleep 100
$ docker inspect hc2 --format '{{.State.Health.Status}} fail={{.State.Health.FailingStreak}} running={{.State.Running}}'
unhealthy fail=4 running=true
```

- `starting → healthy / unhealthy` 상태가 `docker ps`에 표시된다.
- `unhealthy`여도 **컨테이너를 재시작하지 않는다** (`running=true` 유지). 단독 `docker run`에서 헬스체크는 정보 제공용이고, 재시작/교체 같은 조치는 오케스트레이터(스웜, 쿠버네티스 probe)나 별도 로직이 한다.

##### 13-6. 모니터링 명령 요약

| 목적 | 명령 |
|---|---|
| 지금 자원 사용량 | `docker stats [--no-stream]` |
| 컨테이너 내부 프로세스 | `docker top <c>` |
| 상태/종료 원인/설정 | `docker inspect <c>` (`.State`, `.HostConfig`) |
| 로그 | `docker logs -f --tail N -t --since 10m <c>` |
| 이벤트 스트림 | `docker events --filter ...` |
| 디스크 사용량 | `docker system df [-v]`, `docker ps -s` |
| 변경 파일 | `docker diff <c>` |
| 헬스 | `docker ps` 의 Status, `inspect .State.Health` |
| 장기 이력/알림 | cAdvisor, Prometheus, Grafana 등 외부 도구 |

#### 14. 확인하지 못한 부분

- **3-1절 옵션**: `--ip`는 사용자 정의 네트워크에서만 쓴다는 점은 기본 bridge에서 실행해 보지 않고 일반 설명으로 적었다. `--privileged`의 위험성, `--cap-add` 목록별 효과는 `SYS_ADMIN`/`mount` 한 가지만 확인했다. `--device`는 `/dev/null`로만 시험했고 실제 하드웨어 장치(GPU 등)는 해보지 않았다.
- **`docker attach`, `Ctrl+P Ctrl+Q`, `-t` 단독 + 파이프 stdin 동작**: 대화형 터미널이 필요해 실행하지 못했다. 3절의 `attach` 설명은 일반 설명이다.
- **`pause`의 내부 방식(cgroup freezer)**: `paused=true`와 프로세스 유지만 확인했고 freezer 동작은 확인하지 않았다.
- **Linux 서버에서의 bind mount 소유권 문제**: Docker Desktop의 `fakeowner` 변환만 확인했다. bind mount와 named volume의 성능 차이도 측정하지 않았다.
- **`volume prune`의 기본 삭제 범위**: 익명 볼륨만 지운다는 설명은 문서 기준이며 실행하지 않았다 (사용자의 기존 볼륨이 있어 실행을 피했다).
- **`--blkio-weight`**: 이 VM에서는 `io.weight`가 없어 실패했다. 다른 환경에서의 동작은 확인하지 못했다. 블록 I/O 제한 옵션(`--device-read-bps` 등)은 실행하지 않았다.
- **host 네트워크에서 Mac의 `localhost`로 접근 가능한지**: Docker Desktop에서는 VM 네트워크라는 것까지만 확인했다. macvlan/ipvlan/overlay 네트워크는 실행하지 않았다 (overlay는 3장 스웜과 연결).
- **Dockerfile `SHELL`, `STOPSIGNAL`, `ONBUILD`, `HEALTHCHECK` 명령(Dockerfile 내 정의)**: 표의 설명은 일반 설명이다. 헬스체크는 `docker run --health-*` 옵션으로만 확인했다.
- **shell form과 시그널 전달**: PID 1이 쉘이 되는 것까지는 확인했지만 쉘이 `SIGTERM`을 자식에게 전달하지 않아 종료가 지연되는 현상을 shell form 이미지로 직접 재현하지는 않았다. 쉘 구현(BusyBox/dash/bash)에 따라 다를 수 있다.
- **스토리지 드라이버**: overlay2와 vfs만 직접 비교했다. btrfs/zfs/devicemapper/fuse-overlayfs는 실행하지 않았고 표의 설명은 일반 설명이다. vfs 비교는 DinD 안에서 한 결과이며 컨테이너 시작 시간 등 성능은 측정하지 않았다. overlay2의 opaque 디렉터리 표시도 확인하지 않았다. containerd 이미지 스토어(snapshotter) 방식은 확인하지 않았다.
- **`docker top`의 `statd` 표시 원인**: 호스트 `/etc/passwd`로 UID를 변환하기 때문이라는 설명은 추정이다.
- **cAdvisor/Prometheus/Grafana 연동**: 실행하지 않았다.
- **데몬 설정 변경 후 재시작 동작**(`live-restore`, `log-driver` 전역 설정): Docker Desktop의 `daemon.json`을 수정하지 않았다. Linux에서의 `/etc/docker/daemon.json`, `systemctl` 동작은 확인하지 못했다.
- 책 본문의 구체적인 예제·그림은 반영하지 않았다 (요청받은 토픽 + 실측 중심).
