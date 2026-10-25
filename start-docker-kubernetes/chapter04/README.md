### 4장. 도커 컴포즈

> 정리 범위: docker compose의 특징과 기본 사용법, YAML 주요 옵션, 도커 엔진 / containerd / runc의 관계
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop, Docker Engine 28.0.4, **Docker Compose v2.34.0**(플러그인, `docker compose`), VM 안의 containerd + runc 1.2.5
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.

#### 목차

1. 도커 컴포즈의 특징
2. 기본 사용법과 명령어
3. YAML 주요 옵션
4. 변수와 환경 파일, 설정 합치기 (override)
5. 도커 엔진, containerd, runc의 관계
6. 확인하지 못한 부분

---

#### 1. 도커 컴포즈의 특징

**여러 컨테이너로 구성된 애플리케이션을 YAML 파일 하나로 정의하고, 한 번의 명령으로 생성/실행/삭제하는 도구**다.

| 구분 | `docker run` 반복 | docker compose |
|---|---|---|
| 정의 방식 | 긴 옵션을 명령어로 나열 | 선언형 YAML (`compose.yaml`) |
| 재현성 | 스크립트/문서로 관리 | 파일이 곧 환경 정의 (Git 관리) |
| 네트워크 | 직접 생성/연결 | 프로젝트별 네트워크 자동 생성 |
| 서비스 간 이름 해석 | 사용자 정의 네트워크를 직접 만들어야 함 | **서비스 이름으로 자동 DNS** |
| 실행 순서 | 직접 제어 | `depends_on` |
| 정리 | 컨테이너/네트워크/볼륨 각각 삭제 | `docker compose down` |
| 대상 | 단일 호스트 | **단일 호스트** (여러 호스트는 스웜/쿠버네티스) |

- **단일 호스트용**이다. 여러 노드에 분산하거나 장애 시 다른 노드로 옮기지 않는다 (3장 스웜, 6장 이후 쿠버네티스의 영역).
- Compose는 **Docker Engine의 API를 호출하는 클라이언트**일 뿐이다. YAML을 읽어 `create`/`start`/`network create` 같은 호출로 바꾼다. 컨테이너를 직접 관리하지 않으므로 Compose를 종료해도 컨테이너는 계속 돈다.
- 버전: `docker-compose`(Python, v1)는 종료되었고, 현재는 Go로 다시 만든 **Compose v2 (`docker compose`, 플러그인)** 가 표준이다. 이 환경에서 `docker compose version`과 `docker-compose version`이 모두 `v2.34.0-desktop.1`을 출력했다 (`docker-compose` 명령은 v2로 연결된 별칭).
- **`version:` 키는 더 이상 쓰지 않는다.** Compose Specification을 따르는 v2는 `version`을 무시하고 경고만 낸다.

```
$ docker compose -f v.yaml config      # 첫 줄에 version: "3.8"을 넣은 파일
level=warning msg=".../v.yaml: the attribute `version` is obsolete, it will be ignored, please remove it to avoid potential confusion"
```

- 파일 이름: `compose.yaml`(권장), `compose.yml`, `docker-compose.yaml`, `docker-compose.yml`을 자동 탐색한다. `-f`로 직접 지정할 수 있다.

#### 2. 기본 사용법과 명령어

##### 2-1. 실험용 프로젝트

```yaml
# compose.yaml (일부)
name: t5demo
services:
  db:
    image: alpine:3.20
    command: sh -c 'sleep 8; touch /ready; sleep 3000'     # 8초 뒤에 준비되는 "느린 DB"
    healthcheck: { test: ["CMD","test","-f","/ready"], interval: 2s, retries: 10 }
    networks: [back]
  web:
    image: nginx:${TAG:-latest}
    ports: ["${WEB_PORT:-8080}:80"]
    depends_on: { db: { condition: service_healthy } }
    networks: [front, back]
  api:
    build: { context: ./api, args: { GREETING: "${GREETING:-nogreeting}" } }
    image: t5demo-api:local
    networks: [back]
networks: { front: {}, back: { internal: true } }
volumes: { webdata: {} }
```

##### 2-2. 생성부터 정리까지

```
$ docker compose up -d
 Volume "t5demo_webdata"  Created
 Container t5demo-api-1   Created
 Container t5demo-db-1    Created
 Container t5demo-web-1   Created
 Container t5demo-db-1    Started
 Container t5demo-api-1   Started
 Container t5demo-db-1    Waiting
 Container t5demo-db-1    Healthy        ← db가 healthy가 될 때까지 web은 기다림
 Container t5demo-web-1   Starting
 Container t5demo-web-1   Started
(up 소요 21초: 이미지 빌드 + db 8초 대기 포함)

$ docker compose ps
NAME           SERVICE   STATUS                   PORTS
t5demo-api-1   api       Up 8 seconds
t5demo-db-1    db        Up 8 seconds (healthy)
t5demo-web-1   web       Up Less than a second    0.0.0.0:18090->80/tcp
```

**자동으로 만들어진 리소스와 이름 규칙**

```
컨테이너 : <프로젝트>-<서비스>-<번호>      t5demo-web-1
네트워크 : <프로젝트>_<네트워크>           t5demo_front, t5demo_back (bridge)
볼륨     : <프로젝트>_<볼륨>              t5demo_webdata
이미지   : image: 이름을 주면 그 이름, 없으면 <프로젝트>-<서비스>
```

- **프로젝트 이름**은 `name:` → `-p` 옵션 → `COMPOSE_PROJECT_NAME` → 디렉터리 이름 순으로 정해진다 (이 노트에서는 `name:`과 `-p`만 확인: `docker compose -p other config` → `name: other`). 프로젝트 이름이 같으면 같은 프로젝트로 취급하므로 서로 다른 디렉터리의 같은 이름 프로젝트가 충돌할 수 있다.
- Compose는 컨테이너에 **레이블**을 붙여 자신이 만든 것을 추적한다.

```
com.docker.compose.project = t5demo
com.docker.compose.service = web
com.docker.compose.container-number = 1
com.docker.compose.config-hash = c96ce47a...      # 설정 해시: 설정이 바뀌면 달라져 재생성의 판단 기준
com.docker.compose.depends_on = db:service_healthy:false
com.docker.compose.project.working_dir / config_files / version=2.34.0 / oneoff=False
```

##### 2-3. 자주 쓰는 명령어

| 명령 | 설명 | 실측 |
|---|---|---|
| `up [-d]` | 생성/시작 (이미 있으면 필요한 것만 갱신) | 변경 없으면 `Running` 그대로, 바뀐 서비스만 `Recreated` |
| `up --build` | 이미지 재빌드 후 실행 | 아래 4절 |
| `down` | 컨테이너, 네트워크 삭제 (**볼륨은 유지**) | `-v`로 볼륨까지 삭제 |
| `ps [-a]` | 프로젝트의 컨테이너 | |
| `logs [-f] [--tail N] [service]` | 로그 | `api-1 | api-fromdotenv` 형식으로 서비스 접두사 |
| `exec [-T] svc cmd` | 실행 중 컨테이너에서 명령 | `-T`는 TTY 미할당(스크립트용) |
| `run [--rm] svc cmd` | **일회성 컨테이너**를 새로 생성해 실행 | 이름 `<프로젝트>-<서비스>-run-xxxx` |
| `stop/start/restart/pause` | 컨테이너 수명 | |
| `top`, `images`, `ls` | 프로세스, 이미지, 실행 중인 프로젝트 목록 | |
| `config` | 합쳐지고 변수가 치환된 최종 설정 출력 | 검증용으로 매우 유용 |
| `build`, `pull`, `push` | 이미지 작업 | |
| `cp`, `port`, `events`, `watch` | 기타 | (실행하지 않음) |

- `docker compose ls`는 실행 중인 프로젝트와 설정 파일 경로를 보여줬다 (`t5demo running(3) /…/compose.yaml`).
- `docker compose run --rm -T db echo from-run`은 `db`의 설정(네트워크, 볼륨 등)으로 컨테이너를 따로 하나 만들어 실행하고 `--rm`으로 정리했다 (`from-run` 출력, 컨테이너 0개 남음). `run`은 기본적으로 **서비스의 포트 게시를 하지 않으며** `depends_on`의 의존 서비스는 같이 시작한다.

##### 2-4. 변경 시 재생성 규칙

```
$ docker compose up -d                              # 변경 없음
 Container t5demo-db-1   Running
 Container t5demo-web-1  Running

$ WEB_PORT=18091 docker compose up -d               # 포트 설정 변경 → web만 재생성
 Container t5demo-web-1  Recreated
 Container t5demo-db-1   Waiting / Healthy           # db는 건드리지 않음
 Container t5demo-web-1  Started
$ curl localhost:18091   → 200
```

`up`은 **선언된 상태와 현재 상태를 비교**(config-hash 레이블)해서 달라진 서비스만 재생성한다. `--force-recreate`로 전부 재생성, `--no-recreate`로 기존 유지(실제로 `--scale api=3 --no-recreate`는 사용했다)가 가능하다.

##### 2-5. `down`의 범위

```
$ docker compose down
 Container t5demo-web-1 Removed / t5demo-api-1 Removed / t5demo-db-1 Removed
 Network t5demo_front Removed / t5demo_back Removed
$ docker volume ls --filter name=t5demo  → t5demo_webdata     # 볼륨은 남음
$ docker compose down -v                  → Volume t5demo_webdata Removed
```

- 기본 `down`은 **볼륨을 지우지 않는다** (데이터 보호). `-v`는 데이터 삭제이므로 주의한다.
- **프로파일 서비스를 `run`으로 실행**하면 `t5demo_default` 네트워크가 생기는데, 프로파일 없이 `down`하면 이 네트워크가 남았다. `--profile batch down`으로 지워졌다. 프로파일을 쓰는 서비스는 `down` 때도 같은 프로파일을 줘야 완전히 정리된다.
- 정의에서 삭제된 서비스의 컨테이너는 orphan으로 남으며 `down --remove-orphans`로 정리한다 (옵션을 사용해 정리했지만 orphan 경고 자체는 이번 실험에서 재현하지 않았다).

#### 3. YAML 주요 옵션

##### 3-1. 서비스 옵션 총정리

| 옵션 | 설명 | `docker run` 대응 |
|---|---|---|
| `image` | 사용할 이미지 (`build`와 같이 쓰면 빌드 결과의 이름) | 이미지 |
| `build` | 빌드 설정 (`context`, `dockerfile`, `args`, `target`) | `docker build` |
| `command` / `entrypoint` | 명령 / 엔트리포인트 덮어쓰기 | 이미지 뒤 인자 / `--entrypoint` |
| `ports` | 호스트:컨테이너 포트 게시 | `-p` |
| `expose` | 컨테이너 포트만 선언(게시 안 함) | `EXPOSE` |
| `environment` / `env_file` | 환경변수 | `-e` / `--env-file` |
| `volumes` | 볼륨/바인드 마운트 | `-v` |
| `networks` | 연결할 네트워크 (+`aliases`, `ipv4_address`) | `--network` |
| `depends_on` | 시작 순서/조건 | (없음) |
| `healthcheck` | 헬스체크 | `--health-*` |
| `restart` | 재시작 정책 | `--restart` |
| `deploy.resources.limits` | CPU/메모리 제한 | `--cpus`, `-m` |
| `profiles` | 프로파일을 줄 때만 시작 | (없음) |
| `labels`, `user`, `working_dir`, `hostname`, `extra_hosts`, `cap_add`, `container_name` 등 | | `-l`, `-u`, `-w`, `-h`, `--add-host`, `--cap-add`, `--name` |
| 최상위 `networks`, `volumes`, `configs`, `secrets` | 공용 리소스 정의 | |
| 최상위 `name`, `x-*`(확장 필드) | 프로젝트 이름, 재사용용 앵커 | |

##### 3-2. 실험으로 확인한 동작

**`ports`**

```
ports: ["${WEB_PORT:-8080}:80"]   →  docker compose config:  published: "18090", target: 80, protocol: tcp
$ curl localhost:18090            →  <h1>hello compose</h1>
```

- 호스트 포트를 고정하면 **같은 서비스를 `--scale`로 늘릴 수 없다.**

```
$ docker compose up -d --scale web=2
Error response from daemon: ... Bind for 0.0.0.0:18090 failed: port is already allocated
```

  스케일이 필요한 서비스는 호스트 포트를 생략(`"80"`)하거나 앞단에 로드밸런서를 둔다.
- `--scale api=3`처럼 포트가 없는 서비스는 `t5demo-api-1/2/3`으로 늘어났다. 컨테이너 이름은 `-<번호>`로 구분되므로 `container_name`을 고정하면 스케일이 불가능하다.

**`volumes`: 이름 있는 볼륨과 바인드 마운트**

```yaml
volumes:
  - webdata:/data                     # 최상위 volumes에 선언된 named volume → t5demo_webdata
  - ./html:/usr/share/nginx/html:ro   # 상대 경로 = bind mount (compose 파일 기준), :ro 읽기 전용
```
```
$ docker exec t5demo-web-1 sh -c 'touch /usr/share/nginx/html/x'
touch: /usr/share/nginx/html/x: Read-only file system
```

**`networks`: 서비스 이름으로 통신, 네트워크 분리, internal**

```
$ docker exec t5demo-web-1 getent hosts db api
172.19.0.3  db        172.19.0.2  api          ← 서비스 이름이 DNS로 해석됨
$ docker exec t5demo-web-1 cat /etc/resolv.conf | grep nameserver   → nameserver 127.0.0.11   (내장 DNS)
$ docker inspect t5demo-web-1 (네트워크)
t5demo_back:172.19.0.4(t5demo-web-1 web)  t5demo_front:172.18.0.2(t5demo-web-1 web)   # 두 네트워크에 연결, alias = 서비스 이름
```

- `networks`를 지정하지 않으면 모든 서비스가 프로젝트의 `default` 네트워크에 붙는다. 위 예제처럼 지정하면 `web`만 `front`와 `back` 양쪽에 붙어 **`db`는 `front` 쪽에서 보이지 않는다.**
- `internal: true`인 네트워크는 **외부와 통신할 수 없다.**

```
$ docker exec t5demo-db-1  wget http://example.com  → BLOCKED      # back(internal)에만 있음
$ docker exec t5demo-web-1 wget http://example.com  → OK           # front(일반 bridge)에도 연결됨
```

  DB 같은 내부 서비스를 internal 네트워크에만 두면 외부 유출/접근을 막을 수 있다.
- 서비스 이름 해석은 **같은 네트워크 안에서만** 된다 (2장에서 확인한 사용자 정의 bridge 동작과 같음).

**`depends_on`: 시작 순서 vs 준비 상태**

| 형태 | 의미 |
|---|---|
| `depends_on: [db]` (= `condition: service_started`) | db 컨테이너가 **시작**되면 바로 다음 시작. 준비 여부는 모름 |
| `condition: service_healthy` | db의 healthcheck가 **healthy**가 되어야 시작 |
| `condition: service_completed_successfully` | db가 정상 종료(exit 0)해야 시작 (마이그레이션 컨테이너 등. 이번 실험 미실행) |

```
# 짧은 형태: db가 6초 걸려 준비되는 서비스, app이 의존
$ docker compose up -d     → Container slowdb Started; Container app-started Started      # 곧바로 둘 다 시작
$ docker compose logs
app-started-at 1792505650
db-ready-at   1792505656                 # app이 db 준비보다 6초 먼저 시작됨

# service_healthy 조건: 위 프로젝트에서 web이 db Waiting → Healthy 이후에 Started (up 21초)
```

- `depends_on`의 기본 동작은 **"컨테이너가 시작됨"** 까지만 보장한다. DB 연결 가능 여부를 보장하지 않으므로 `healthcheck` + `service_healthy`를 쓰거나 앱에서 재시도한다.
- `docker compose config`에서 `required: true`가 기본값으로 붙었다 (v2.20+에서 `required: false`로 선택적 의존 가능, 이번에 미실행).

**`healthcheck`**: 2장에서 본 `--health-*`와 같다. `test`는 `["CMD", ...]`(exec) 또는 `["CMD-SHELL", "..."]`(쉘), `interval`, `timeout`, `retries`, `start_period`.

**`restart`**: `unless-stopped`가 `docker inspect`의 `RestartPolicy`에 그대로 반영됐다 (`no`, `always`, `on-failure`, `unless-stopped`).

**`deploy.resources`**: 스웜용 키처럼 보이지만 Compose v2는 단독 실행에서도 적용한다.

```yaml
deploy: { resources: { limits: { cpus: "0.5", memory: 64M } } }
$ docker inspect t5demo-web-1 --format 'mem={{.HostConfig.Memory}} nano={{.HostConfig.NanoCpus}}'
mem=67108864 nano=500000000        # 64MiB, 0.5 CPU — docker run -m 64m --cpus 0.5 와 같음
```

**`profiles`**: 프로파일이 붙은 서비스는 기본 `up`에서 **시작되지 않는다.**

```yaml
batch: { image: alpine:3.20, command: echo batch-ran, profiles: [batch] }
$ docker compose up -d                          → batch는 시작되지 않음 (ps -a: api db web)
$ docker compose --profile batch run --rm batch → batch-ran
```

디버깅 도구, 일회성 배치, 마이그레이션 같은 서비스에 쓴다. `--profile` 또는 `COMPOSE_PROFILES`로 활성화한다.

**`build`와 `image`**

```yaml
api:
  build: { context: ./api, args: { GREETING: "${GREETING:-nogreeting}" } }
  image: t5demo-api:local
```
```
$ docker logs t5demo-api-1   → api-fromdotenv        # .env의 GREETING이 빌드 인자로 들어가 ENV가 됨
```

- `image`와 `build`를 같이 쓰면 **빌드 결과에 그 이름을 붙인다.**
- `up`은 이미지가 이미 있으면 **다시 빌드하지 않는다.** Dockerfile을 수정해도 `up -d`만 하면 컨테이너는 그대로다.

```
$ (Dockerfile의 echo api- → echo api2- 로 수정)
$ docker compose up -d           → t5demo-api-1 Running        # 변화 없음, logs: api-fromdotenv
$ docker compose up -d --build   → Recreated 후 시작           # logs: api2-fromdotenv
```

  소스를 바꾼 뒤에는 `up --build`나 `docker compose build`가 필요하다.

**YAML 앵커와 확장 필드**

```yaml
x-common: &common          # x- 로 시작하는 최상위 키는 Compose가 무시하는 사용자 정의 영역
  restart: unless-stopped
  labels: { team: demo }
services:
  db:  { <<: *common, image: ... }     # <<: 로 병합
  web: { <<: *common, image: ... }
```

`docker compose config`에서 `db`와 `web` 모두 `restart: unless-stopped`와 `labels.team=demo`가 펼쳐져 나왔다. 반복되는 설정을 줄이는 방법이다.

#### 4. 변수와 환경 파일, 설정 합치기 (override)

##### 4-1. 변수 치환 `${VAR}`

```
image: nginx:${TAG:-latest}      →  config 출력: image: nginx:alpine       (.env에 TAG=alpine)
${WEB_PORT:-8080}                →  "18090"
${GREETING:-nogreeting}          →  "fromdotenv"
```

| 문법 | 의미 |
|---|---|
| `${VAR}` | 값 (없으면 빈 문자열, 경고) |
| `${VAR:-default}` | 없거나 비어 있으면 default |
| `${VAR:?error}` | 없으면 에러로 중단 |

**우선순위**: 셸 환경 변수 > `--env-file` > 프로젝트 디렉터리의 `.env`

```
.env: GREETING=fromdotenv
$ docker compose config | grep GREETING                      →  GREETING: fromdotenv
$ GREETING=fromshell docker compose config | grep GREETING   →  GREETING: fromshell    (셸이 우선)
$ docker compose --env-file alt.env config | grep GREETING   →  GREETING: viaflag      (--env-file이 .env를 대체)
```

##### 4-2. `.env`와 `env_file`, `environment`는 서로 다르다

| | 용도 | 컨테이너 환경변수로 전달? |
|---|---|---|
| `.env` (프로젝트 루트) | **compose 파일 안의 `${}` 치환**에 쓰는 값 | **아니오** |
| `env_file: [app.env]` | 컨테이너 환경변수로 로드 | 예 |
| `environment:` | 컨테이너 환경변수 직접 지정 | 예 |

```
.env     : SECRET_ONLY_IN_DOTENV=zzz
app.env  : FROM_ENVFILE=yes  /  OVERRIDE_ME=envfile
compose  : environment: { OVERRIDE_ME: environment, PLAIN: value }

$ docker exec t5demo-web-1 sh -c 'echo FROM_ENVFILE=$FROM_ENVFILE OVERRIDE_ME=$OVERRIDE_ME PLAIN=$PLAIN SECRET_ONLY_IN_DOTENV=[$SECRET_ONLY_IN_DOTENV]'
FROM_ENVFILE=yes OVERRIDE_ME=environment PLAIN=value SECRET_ONLY_IN_DOTENV=[]
```

- `.env`의 값은 컨테이너에 전달되지 않는다 (`SECRET_ONLY_IN_DOTENV`가 비어 있음).
- `env_file`과 `environment`에 같은 키가 있으면 **`environment`가 우선**한다 (`OVERRIDE_ME=environment`).
- 컨테이너 환경변수는 `docker inspect`에 평문으로 보이므로 민감 값은 시크릿을 쓴다 (3장).

##### 4-3. 여러 파일 합치기

```
$ cat compose.override.yaml          # compose.yaml과 같은 디렉터리에 있으면 -f 없이도 자동 병합
services:
  web:
    ports: ["18092:80"]
    environment: { PLAIN: overridden, EXTRA: added }

$ docker compose config  (web 부분)
  PLAIN: overridden      EXTRA: added                     # 매핑: 키 단위로 덮어쓰기/추가
  ports: published "18090" + published "18092"             # 리스트(ports): 덮어쓰지 않고 합쳐짐(추가)

$ docker compose -f compose.yaml -f prod.yaml config | grep 'image: nginx'
    image: nginx:stable-alpine                             # 단일 값: 뒤의 파일이 이김
```

| 필드 종류 | 합치는 방식 |
|---|---|
| 스칼라 (`image`, `restart` 등) | 뒤 파일이 덮어씀 |
| 매핑 (`environment`, `labels`) | 키 단위 병합 |
| 리스트 (`ports`, `volumes`, `expose`) | **이어 붙임** (중복 제거) |
| `command`, `entrypoint` | 덮어씀 |

- 개발용(`compose.override.yaml`)과 운영용(`-f compose.yaml -f prod.yaml`)을 나누는 전형적인 방법이다.
- 합쳐진 결과는 항상 `docker compose config`로 확인한다. 리스트가 합쳐져서 포트가 두 개 열리는 것처럼 의도와 다른 결과가 나오기 쉽다.

#### 5. 도커 엔진, containerd, runc의 관계

##### 5-1. 계층 구조

```
docker CLI / docker compose
      │ REST API (unix socket)
      ▼
   dockerd  (Docker Engine)     이미지 빌드/관리, 네트워크, 볼륨, 로그, 컨테이너 API
      │ gRPC (containerd.sock)
      ▼
   containerd                   컨테이너 수명주기, 이미지 저장/전송, 스냅샷, (CRI 지원)
      │ ttrpc
      ▼
   containerd-shim-runc-v2      컨테이너 1개당 1프로세스. 컨테이너의 부모, stdio/종료코드 관리
      │ exec
      ▼
   runc                         OCI 런타임. namespace, cgroup 설정 후 컨테이너 프로세스를 실행하고 **종료**
      ▼
   컨테이너 프로세스 (PID 1)
```

| 구성요소 | 한 줄 설명 | 비유 |
|---|---|---|
| **dockerd** | 사용자 대면 API + 이미지 빌드, 네트워크, 볼륨 등 "도커 기능" 전체 | 매니저/접수 창구 |
| **containerd** | 컨테이너 **런타임 관리 데몬** (실행/정지, 이미지 pull/push, 스냅샷) | 현장 감독 |
| **shim** | containerd와 컨테이너 사이의 얇은 대리자. 데몬이 재시작돼도 컨테이너를 유지 | 현장 반장 |
| **runc** | 커널 기능으로 컨테이너를 **만들고 시작**하는 CLI (저수준 런타임, OCI 구현체) | 시공 도구 |

- 역사: 초기 도커는 단일 데몬이 모든 것을 했다. 이후 **runc**(실행기, libcontainer)와 **containerd**(런타임 관리)를 분리해 각각 독립 프로젝트(OCI, CNCF)가 되었다. 그래서 쿠버네티스는 도커 엔진 없이 **containerd를 직접** 쓸 수 있다.
- **OCI** 표준: Image Spec(이미지 포맷), Runtime Spec(`config.json` + `rootfs` 번들), Distribution Spec(레지스트리). runc는 Runtime Spec의 참조 구현이다.

##### 5-2. 실제 프로세스 트리 확인

`sleep 300` 컨테이너를 만들고 VM에서 프로세스 계보를 거슬러 올라갔다.

```
$ (VM에서) 프로세스 계보
app pid=78536   sleep 300                                          ← 컨테이너 프로세스
 └ 78514        /usr/bin/containerd-shim-runc-v2 -namespace moby -id a7d97c62fae1... -address /run/containerd/containerd.sock
    └ 1         /init
```

- **컨테이너 프로세스의 부모는 `runc`가 아니라 shim**이다. runc는 컨테이너를 시작한 뒤 종료해서 상주하지 않는다. shim의 자식은 `sleep 300` 하나뿐이었다.
- shim의 인자: `-namespace moby`(containerd 네임스페이스. 도커가 쓰는 이름), `-id <컨테이너 ID>`, `-address .../containerd.sock`.
- shim은 `PPID=1`(VM의 init)이다. dockerd/containerd의 자식이 아니라서 **데몬이 죽어도 컨테이너가 같이 죽지 않는** 구조다 (`live-restore`와 연결. 데몬 중단 실험은 하지 않았다).

##### 5-3. OCI 번들: dockerd/containerd가 runc에게 넘기는 것

shim 프로세스의 작업 디렉터리가 곧 컨테이너의 OCI 번들 디렉터리다.

```
/run/desktop-containerd/daemon/io.containerd.runtime.v2.task/moby/<컨테이너ID>/
├── config.json       ← OCI Runtime Spec: 실행할 명령, 환경변수, capability, namespace, cgroup, 마운트 …
├── rootfs/           ← 컨테이너의 루트 파일시스템 (overlayfs 마운트 지점)
├── init.pid          ← 컨테이너 PID 1의 호스트(VM) PID
├── log / log.json    ← shim 로그
├── options.json, address, runtime, shim-binary-path, work
```

```
config.json (일부)
"process": { "args": ["sleep","300"], "env": ["PATH=...","HOSTNAME=a7d97c62fae1"], "cwd": "/",
             "capabilities": { "bounding": ["CAP_CHOWN","CAP_DAC_OVERRIDE", ... ] } ... }
"namespaces": [ mount, network, uts, pid, ipc, cgroup ]       ← user namespace 없음 (1장에서 확인한 사실과 일치)
```

즉 `docker run -m 64m --cap-drop ...`의 옵션은 dockerd가 **OCI `config.json`으로 변환**하고, runc는 도커를 전혀 모른 채 이 파일만 읽고 컨테이너를 만든다. 1장·2장에서 본 cgroup 값, capability 집합이 그 결과다.

##### 5-4. runc 단독 실행 (도커 없이)

컨테이너 rootfs를 `docker export`로 만들고, **dockerd와 containerd 없이 runc만으로** 실행했다 (runc 1.1.14, 특권 컨테이너 안에서 실행).

```
$ docker export <alpine 컨테이너> > rootfs.tar ; tar xf rootfs.tar -C /bundle/rootfs
$ cd /bundle && runc spec        # 기본 config.json 생성 → 실행 명령만 수정
$ runc run demo1
hello-from-runc pid=1 host=runc        ← 컨테이너 안에서 PID 1, hostname=runc (spec 기본값)
0::/

$ runc run -d demo2 ; runc list
ID     PID  STATUS   BUNDLE   CREATED
demo2  61   running  /bundle  ...
$ ppid of container proc: 1             ← shim 없이 PID 1에 입양됨
$ pid namespace: 컨테이너 pid:[4026533769] ≠ 호출자 pid:[4026533634]
$ runc kill demo2 KILL ; runc list → demo2 PID 0 stopped ; runc delete demo2
```

기본 `runc spec`의 namespaces: `pid, network, ipc, uts, mount, cgroup` — Docker 컨테이너와 같은 구성이다.

- **이미지 pull, 네트워크 연결(veth/bridge), 볼륨, 포트 매핑, 로그 수집은 runc가 하지 않는다.** 그 일은 dockerd/containerd/CNI가 한다. 이 실험의 컨테이너에는 네트워크 인터페이스 설정도 없었다 (`runc spec`의 기본은 새 net ns).
- 데몬과 shim 없이 `run -d`로 띄운 컨테이너는 부모가 `PID 1`이라 **감시자가 없다.** shim이 맡는 역할(stdio 유지, 종료 코드 수집, 데몬 재시작에도 생존)이 이것이다.

##### 5-5. 계층별 책임 정리

| 작업 | 담당 |
|---|---|
| `docker build`, Dockerfile 파싱 | dockerd (BuildKit) |
| 이미지 저장/pull/push, 레이어 스냅샷(overlayfs) | containerd (dockerd 경유) |
| 네트워크(bridge, veth, iptables), 볼륨, 로그 드라이버 | dockerd (libnetwork) |
| 컨테이너 시작/정지 호출 | dockerd → containerd |
| 컨테이너의 부모 프로세스, stdio, 종료 코드, 데몬 재시작 시 생존 | containerd-shim |
| namespace/cgroup/capability/seccomp 적용 후 `exec` | **runc** |
| 실제 격리/자원 제한 강제 | **리눅스 커널** (1장) |

- 같은 Docker Desktop VM에서 `ps`로 `kubelet`과 `cri-dockerd`도 보였다 (`cri-dockerd --docker-endpoint unix:///run/guest-services/cri-dockerd-proxy.sock ...`). Docker Desktop에 켜 둔 쿠버네티스가 **kubelet → cri-dockerd → dockerd → containerd → runc** 경로를 쓴다는 뜻이다. 쿠버네티스(CRI)는 이 중 dockerd를 건너뛰고 containerd를 직접 호출할 수도 있다 (이후 장에서 연결).
- 런타임이 runc가 아닌 대체 구현으로 바뀔 수 있는 이유도 이 분리 덕분이다 (`docker info`: `Runtimes: io.containerd.runc.v2 runc`, `Default Runtime: runc`. gVisor `runsc`나 Kata 같은 런타임을 같은 방식으로 붙인다. 이번에 실행은 하지 않았다).

#### 6. 확인하지 못한 부분

- **Compose 옵션**: `extends`, `include`, `configs`/`secrets`(Compose 레벨), `develop.watch`, `docker compose watch`, `depends_on`의 `service_completed_successfully`/`required: false`/`restart: true`, `healthcheck`의 `start_period`, `build.target`, `cache_from`, `platform`, `extra_hosts`, `ipv4_address`는 실행하지 않았다. 표의 설명은 일반 설명이다.
- **`restart` 동작**: `restart: unless-stopped`가 `inspect`에 반영되는 것만 확인했다. 컨테이너를 강제 종료했을 때 되살아나는 실험은 `api` 서비스에 `restart`를 주지 않은 채로 해서 의미 있는 결과가 없었다 (재실험하지 않았다).
- **`run`의 포트 게시 여부**(`--service-ports` 없이는 게시하지 않는다는 설명)와 `depends_on` 서비스를 함께 시작하는 동작은 일반 설명이며, 이번에 `run`으로 확인한 것은 `--rm`으로 일회성 컨테이너가 만들어지고 정리되는 것까지다.
- **프로젝트 이름 결정 순서**: `name:`과 `-p`만 확인했고 `COMPOSE_PROJECT_NAME`, 디렉터리 이름 기반 기본값은 실행하지 않았다.
- **orphan 경고**: 서비스를 정의에서 지운 뒤 `up`/`down`할 때의 경고와 `--remove-orphans` 효과는 재현하지 않았다.
- **`docker compose stop`의 종료 지연**: 실험 중 `stop api`가 `Exited (137)`로 끝났다 (`sleep`이 PID 1이라 SIGTERM을 무시한 것으로 보이며 2장 결과와 일치). 소요 시간은 측정하지 않았다.
- **Compose와 스웜의 관계**: `docker stack deploy`로 같은 파일을 쓸 때 `build`가 무시되고 `deploy` 키가 어떻게 적용되는지는 실행하지 않았다. `deploy.resources`가 단독 Compose에서도 적용되는 것은 위 `inspect`로 확인했다.
- **엔진 계층**: 데몬(dockerd, containerd)을 중지했을 때 컨테이너가 살아남는지, `live-restore` 효과, `containerd`의 `ctr` 같은 도구로 직접 조회하는 것은 하지 않았다 (VM에는 `ctr`이 없었다). `runc --root ... list`로 Docker가 만든 컨테이너를 조회하려 했으나 VM 쪽 state 디렉터리를 찾지 못해 확인하지 못했다.
- **runc 단독 실험**은 특권 컨테이너 안에서 runc 1.1.14로 했고, VM의 runc는 1.2.5다 (버전이 다르다). 번들 구조 관찰은 VM의 실제 컨테이너에서 한 것이다.
- **gVisor/Kata 같은 대체 런타임**, rootless 도커는 실행하지 않았다.
- 책 본문의 구체적인 예제·그림은 반영하지 않았다 (요청받은 토픽 + 실측 중심).
