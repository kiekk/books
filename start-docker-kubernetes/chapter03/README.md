### 3장. 도커 스웜

> 정리 범위: 스웜 / 스웜 클래식 / 스웜 모드 개념과 동작, 매니저를 홀수 개로 두는 이유(Raft), secret과 config, `docker_gwbridge`와 `ingress` 네트워크, 노드 availability
>
> 실측 환경: Docker Desktop(Engine 28.0.4) 위에 Docker-in-Docker(`docker:28-dind`, Engine 28.5.2) 컨테이너 7개를 만들어 클러스터를 구성했다. 같은 사용자 정의 브리지 네트워크(`172.40.0.0/24`)에 매니저 3(m1~m3) + 워커 2(w1, w2)로 스웜 모드를 구성했고, 스웜 클래식은 별도의 엔진 2개(c1, c2)로 구성했다. Docker Desktop 자체에는 `swarm init`을 하지 않았다(호스트의 기존 컨테이너 보호). 실험이 끝난 뒤 컨테이너, 네트워크, 이미지, 익명 볼륨을 모두 삭제했다.
>
> 표기: `$` 명령과 출력은 위 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.

#### 목차

1. 스웜 / 스웜 클래식 / 스웜 모드
2. 스웜 모드의 구성과 동작
3. 매니저는 왜 홀수 개를 추천하는가 (Raft)
4. secret과 config
5. `docker_gwbridge`와 `ingress` 네트워크
6. 노드 availability
7. 확인하지 못한 부분

---

#### 1. 스웜 / 스웜 클래식 / 스웜 모드

"도커 스웜"이라는 이름은 **서로 다른 두 제품**을 가리킨다.

| 구분 | 스웜 클래식 (Docker Swarm, standalone) | 스웜 모드 (Swarm mode) |
|---|---|---|
| 시기 | Docker 1.12 이전 (2014~) | **Docker 1.12 (2016)부터** 엔진에 내장 |
| 설치 | 별도 프로그램/컨테이너 `swarm` 이미지 | 엔진 자체 기능 (`docker swarm init`) |
| 상태 저장 | 외부 KV 스토어(Consul, etcd, ZooKeeper) 또는 정적 노드 목록 | **내장 Raft 저장소** (외부 의존 없음) |
| 사용 방식 | 기존 `docker run`을 클러스터에 보냄 (`DOCKER_HOST=tcp://manager:4000`) | **서비스(service)** 를 선언하면 스웜이 태스크로 스케줄링하고 유지 |
| 자기 치유 | 없음 (컨테이너를 배치만 함) | 있음 (원하는 상태 reconcile) |
| 보안 | 별도 TLS 구성 | 노드 mTLS 자동 구성, 인증서 자동 갱신 |
| secret/config, 롤링 업데이트, 라우팅 메시 | 없음 | 있음 |
| 상태 | **deprecated, 유지보수 종료** | 현재 사용되는 방식 (Docker Engine 기능) |

> 책에서 "도커 스웜"이라고 하면 보통 **스웜 모드**를 가리킨다. 스웜 클래식은 역사적 배경이다.

##### 1-1. 스웜 클래식 동작 확인

공식 `swarm:1.2.9` 이미지(2020년 빌드, amd64 전용이라 Rosetta로 실행)로 외부 KV 없이 정적 노드 목록(`nodes://`)으로 구성했다.

```
$ docker run -d --name classic -p 14000:4000 swarm:1.2.9 manage -H tcp://0.0.0.0:4000 \
    nodes://172.40.0.5:2375,172.40.0.7:2375
level=info msg="Registered Engine c2 at 172.40.0.7:2375"
level=info msg="Registered Engine c1 at 172.40.0.5:2375"

$ docker -H tcp://classic:4000 info
 Server Version: swarm/1.2.9
 Role: primary          Strategy: spread
 Filters: health, port, containerslots, dependency, affinity, constraint, whitelist
 Nodes: 2
  c1: 172.40.0.5:2375   c2: 172.40.0.7:2375

$ for i in 1 2 3 4; do docker -H tcp://classic:4000 run -d --name k$i alpine:3.20 sleep 100; done
$ docker -H tcp://classic:4000 ps --format '{{.Names}}'
c1/k4  c2/k3  c1/k2  c2/k1                      # 컨테이너 이름 앞에 노드 이름이 붙음
$ (엔진 직접 조회)  c1: k4 k2   c2: k3 k1       # spread 전략으로 2:2 분산
```

- 스웜 클래식은 **엔진 앞에 선 프록시/스케줄러**다. 표준 Docker API를 흉내 내며, `docker run`을 받아 노드(엔진)에 분산한다. 전략은 `spread`(기본), `binpack`, `random`.
- 각 엔진은 평범한 단독 엔진이다 (`docker info`의 Swarm 상태 `inactive`).
- 스웜 모드 기능은 없다.

```
$ docker -H tcp://classic:4000 service ls    → Error response from daemon: 404 page not found
$ docker -H tcp://classic:4000 secret ls     → Error response from daemon: 404 page not found
$ docker -H tcp://classic:4000 network create -d overlay ov1
Error ...: This node is not a swarm manager. Use "docker swarm init" or "docker swarm join" ...
```

- 이 환경에서 `swarm manage` 도움말 출력은 템플릿 패닉(`panic: template: help:18`)이 났다. 오래된 이미지 버그로 보이며 본 실험과는 무관하다.
- (참고) 이 구성은 매니저가 단일 컨테이너(HA 없음)다. 클래식의 HA 구성(`--replication`, 외부 KV)은 실행하지 않았다.

#### 2. 스웜 모드의 구성과 동작

##### 2-1. 구성 요소

```
          ┌─────────── 매니저 노드 (Raft 그룹) ───────────┐
 docker  →│ m1 (Leader) ◀── Raft 복제 ──▶ m2 ◀──▶ m3      │
 CLI      │  API / 스케줄러 / 오케스트레이터 / 디스패처 / CA   │
          └────────────────────┬─────────────────────────┘
                               │ 태스크 할당 (gRPC, mTLS)
                  ┌────────────┴────────────┐
                  ▼                         ▼
              워커 w1                      워커 w2   (매니저도 태스크를 실행함)
```

| 개념 | 설명 |
|---|---|
| **노드(node)** | 스웜에 참여한 엔진. 역할은 manager / worker |
| **매니저** | 클러스터 상태(Raft)를 관리, API 제공, 스케줄링. 기본적으로 태스크도 실행 |
| **워커** | 태스크만 실행 |
| **서비스(service)** | 원하는 상태의 선언 (이미지, 복제 수, 포트 등) |
| **태스크(task)** | 서비스의 실행 단위 = 컨테이너 1개 + 스케줄 정보. 태스크는 노드 간에 **이동하지 않고** 죽으면 새 태스크가 생긴다 |
| **모드** | `replicated`(복제 수 지정, 기본) / `global`(모든 노드에 1개씩) |

##### 2-2. 클러스터 구성

```
$ docker swarm init --advertise-addr 172.40.0.2           (m1)
Swarm initialized: current node (0jphp11z...) is now a manager.
  docker swarm join --token SWMTKN-1-1jbek5b6...-8bvjcylw... 172.40.0.2:2377

$ docker swarm join-token -q manager / worker             # 토큰이 서로 다름
$ docker swarm join --token <manager 토큰> 172.40.0.2:2377   (m2, m3)
$ docker swarm join --token <worker 토큰>  172.40.0.2:2377   (w1, w2)

$ docker node ls
ID                          HOSTNAME  STATUS  AVAILABILITY  MANAGER STATUS  ENGINE VERSION
0jphp11z... *               m1        Ready   Active        Leader          28.5.2
syaeq7e1...                 m2        Ready   Active        Reachable       28.5.2
km49z7sx...                 m3        Ready   Active        Reachable       28.5.2
i7tnh9ho...                 w1        Ready   Active                        28.5.2
pyd3ovep...                 w2        Ready   Active                        28.5.2
```

- **조인 토큰** `SWMTKN-1-<루트 CA 해시>-<역할별 비밀>`: 앞부분(루트 CA 지문)으로 조인하는 노드가 올바른 클러스터인지 검증하고, 뒷부분이 역할(manager/worker)을 결정한다.
- 필요한 포트(실제 매니저에서 listen 확인): **2377/tcp**(클러스터 관리, 매니저), **7946/tcp+udp**(노드 간 gossip/디스커버리), **4789/udp**(오버레이 VXLAN 데이터). 방화벽에서 열어야 한다.
- 서비스 생성과 태스크 분배:

```
$ docker service create --name web --replicas 2 -p 8080:80 nginx:alpine
$ docker service ls
ID            NAME  MODE        REPLICAS  IMAGE         PORTS
x62ro3o3ocgy  web   replicated  2/2       nginx:alpine  *:8080->80/tcp
$ docker service ps web --format '{{.Name}} {{.Node}} {{.CurrentState}}'
web.1 m2 Running     web.2 m1 Running
```

- `service create`는 **원하는 상태(desired state)** 를 Raft에 기록하고, 오케스트레이터가 실제 상태와 차이를 계속 맞춘다. 노드가 죽으면 태스크를 다른 노드에 다시 만든다 (6절에서 확인).
- 스웜 설정값 확인 (`docker info --format '{{json .Swarm}}'`):

```
Raft:       SnapshotInterval=10000 KeepOldSnapshots=0 LogEntriesForSlowFollowers=500 ElectionTick=10 HeartbeatTick=1
Dispatcher: HeartbeatPeriod=5s        CAConfig: NodeCertExpiry=90일 (7776000000000000ns)
```

- Raft 데이터는 매니저의 `/var/lib/docker/swarm/raft/` 아래 `wal-v3-encrypted`, `snap-v3-encrypted` 디렉터리에 있다 (디렉터리 이름에 `encrypted`가 들어간다. 4절 참고).

#### 3. 매니저는 왜 홀수 개를 추천하는가 (Raft)

##### 3-1. Raft 합의 알고리즘 요약

스웜 매니저들은 클러스터 상태(서비스, 노드, secret, config 등)를 **Raft**로 복제한다. 모든 매니저가 같은 로그를 가져야 하므로 합의가 필요하다.

| 개념 | 설명 |
|---|---|
| **역할** | Leader(1명) / Follower / Candidate. 스웜의 `Leader`, `Reachable`이 Leader와 Follower에 해당 |
| **term** | 선거 번호(논리 시계). 새 선거마다 증가. 오래된 term의 리더는 인정받지 못한다 |
| **리더 선출** | Follower가 일정 시간(**election timeout**, 무작위) 동안 리더의 heartbeat를 못 받으면 Candidate가 되어 term을 올리고 투표를 요청. **과반수** 표를 얻으면 리더 |
| **로그 복제** | 쓰기는 **리더만** 받는다. 리더가 로그 엔트리를 Follower에 복제하고 **과반수가 저장**하면 커밋 → 적용 |
| **쿼럼(quorum)** | 과반수 = `⌊N/2⌋ + 1`. 선거와 커밋 모두 쿼럼이 필요 |
| 무작위 timeout | 여러 Follower가 동시에 Candidate가 되어 표가 갈리는 상황(split vote)을 줄임 |

- 스웜에서 `docker service create` 같은 **쓰기는 리더 매니저가 처리**한다. Follower 매니저에 요청해도 리더로 전달된다. 워커는 클러스터 상태를 볼 수도 바꿀 수도 없다 (`This node is not a swarm manager`).

##### 3-2. 쿼럼과 장애 허용

| 매니저 수 N | 쿼럼 | 허용되는 동시 장애 수 | 비고 |
|---|---|---|---|
| 1 | 1 | 0 | 단일 장애점 |
| 2 | 2 | **0** | 1대만 죽어도 쿼럼 상실 → 1대보다 나쁨 |
| **3** | 2 | **1** | |
| 4 | 3 | **1** | 3대와 허용치가 같고 노드만 하나 더 |
| **5** | 3 | **2** | |
| 6 | 4 | 2 | 5대와 같은데 비용만 증가 |
| **7** | 4 | **3** | 권장 상한 (그 이상은 복제 비용이 커짐) |

**홀수를 쓰는 이유**: 짝수 개로 늘려도 허용 가능한 장애 수는 늘지 않고(3→4: 1 그대로), 쿼럼은 오히려 커진다. 노드만 늘고 가용성 이득이 없다.
또 네트워크가 **절반씩 분리되는 파티션**에서 짝수는 `2:2`처럼 어느 쪽도 과반수가 아니어서 **양쪽 모두 리더를 못 뽑고** 전체가 멈춘다. 홀수면 한쪽은 반드시 과반수이므로 그쪽이 계속 동작하고, 반대쪽은 쓰기를 못 하므로 **split-brain(리더가 둘이 되어 서로 다른 상태를 만드는 것)이 생기지 않는다.**

##### 3-3. 직접 확인

**① 리더 장애 → 재선출** (매니저 3개, 리더 m1을 `docker kill`)

```
$ docker kill m1
$ (m2에서 docker node ls를 재시도하며 Leader가 보일 때까지 측정)
recovered after 11s
m1 Unknown Unreachable
m2 Unknown Reachable
m3 Unknown Leader                      # m3가 새 리더. 이 순간 모든 노드 STATUS가 Unknown
```

- 11초 후 새 리더가 선출됐다. Raft 설정이 `ElectionTick=10, HeartbeatTick=1`이므로 1틱을 1초로 보면 election timeout 10틱과 맞아떨어진다 (틱 길이를 설정에서 확인하지는 않았으므로 추정).
- 선출 직후 `docker node ls`의 **STATUS가 전부 `Unknown`** 으로 보였다. 새 리더가 아직 노드 heartbeat를 받기 전이라 상태를 모르는 구간이다. 곧 `Ready`로 돌아온다.
- 매니저 로그: `became candidate at term 10` → `became candidate at term 13` → `became leader at term 13` → `elected leader ... at term 13`. term은 선거마다 올라가며, 이 실험에서 여러 번의 선거(장애와 복구를 반복)로 13까지 올랐다.

**② 3대 중 1대 장애: 계속 쓰기 가능**

```
$ docker service scale web=3        (m1이 죽어 있는 상태)
verify: Service web converged        # 2/3 쿼럼으로 정상 처리
```

**③ 3대 중 2대 장애: 쿼럼 상실**

리더 m3을 추가로 죽이고(남은 매니저 m2 1대) 시도:

```
$ docker node ls
Error response from daemon: rpc error: code = DeadlineExceeded desc = context deadline exceeded
$ docker service ls
Error ...: The swarm does not have a leader. It's possible that too few managers are online.
           Make sure more than half of the managers are online.
$ docker service scale web=1  → 같은 에러
$ docker service create --name x nginx:alpine → 같은 에러
```

- 조회와 변경이 **모두 불가능**하다 (읽기도 리더를 거친다).
- 반면 **이미 떠 있는 컨테이너와 라우팅 메시는 계속 동작**했다.

```
$ (워커 w1에서) wget -S http://127.0.0.1:8080  → HTTP/1.1 200 OK     # 서비스 트래픽은 정상
$ docker ps (m2에서)                           → web.1, api-rr.1 ... 컨테이너 유지
$ (워커 w1에서) docker node ls → "This node is not a swarm manager..."
$ docker info (w1)  → swarm=active control=false
```

- 즉 쿼럼 상실은 **컨트롤 플레인 장애**이고 **데이터 플레인(이미 실행 중인 서비스)** 은 계속 동작한다. 다만 이 상태에서는 장애 노드의 태스크를 재스케줄링할 수 없다.
- 죽은 매니저를 다시 `docker start`하자 쿼럼이 복구되어 `node ls`와 서비스 조회가 다시 동작했다. 이 실험에서는 복구 직후 각 서비스의 레플리카가 잠시 `2/3`, `1/3`로 보였다가 재조정되어 모두 `3/3`으로 돌아왔다.
- (이 실험에서 m3의 dind 컨테이너가 한 번 비정상 종료한 뒤 raft 로그를 재생하며 `Down` → `Ready`로 돌아오는 데 수 분이 걸렸다. dind 컨테이너 강제 종료의 영향이며 스웜 동작 자체의 특성으로 일반화하지 않는다.)

**④ 매니저 4대: 2대가 죽으면 쿼럼 상실 (3대와 같은 허용치)**

```
$ docker node promote w1            # 매니저 4개 (m1 m2 m3 w1)
$ docker kill m1 m3                 # 4개 중 2개 장애 (쿼럼 3 필요, 생존 2)
$ docker node ls (m2에서)  → Error ... DeadlineExceeded
```

매니저가 3개일 때는 1대 장애까지 허용이었다. 4개로 늘려도 **2대가 죽으면 쿼럼 상실**이다. 노드를 하나 더 둬도 장애 허용이 늘지 않았다는 것을 확인했다.
이후 `docker node demote w1`으로 되돌렸다 (`raft-members=3`).

##### 3-4. 운영 가이드

- 매니저는 **3, 5, 7개**. 보통 3개면 충분하고 가용영역을 걸쳐 배치한다. 7개 초과는 권장되지 않는다 (리더가 모든 팔로워에 복제해야 하므로 쓰기 지연 증가).
- 매니저를 **워커 전용으로 비우고 싶으면** `docker node update --availability drain <매니저>` (6절).
- 매니저를 하나 더 늘리거나 줄이는 중에 장애가 겹치지 않도록 한 번에 하나씩 변경한다.
- 쿼럼을 완전히 잃으면 `docker swarm init --force-new-cluster`로 남은 한 매니저에서 클러스터를 재구성하는 복구 절차가 있다 (이 실험에서는 실행하지 않았다).
- Raft 데이터(`/var/lib/docker/swarm`)는 백업 대상이다 (4-5절 autolock과 함께 고려).

#### 4. secret과 config

##### 4-1. 개념 비교

| | secret | config |
|---|---|---|
| 목적 | **민감 정보** (비밀번호, 인증서 키, API 키) | 민감하지 않은 설정 파일 (nginx.conf 등) |
| 저장 | Raft에 저장 (Raft는 디스크에 암호화되어 저장), 전송은 mTLS | 동일하게 Raft에 저장 |
| 컨테이너 안 | `/run/secrets/<이름>` 파일 (**tmpfs**, 기본 `0444`) | 기본 `/<이름>` 파일, `target`으로 경로 지정 |
| 크기 제한 | 512,000바이트 미만 | 이번 실험에서 1MB도 생성됨 (아래) |
| 변경 | **불변** (update 없음, 새로 만들고 교체) | 불변 (동일) |
| 할당 단위 | 서비스 (`--secret`) | 서비스 (`--config`) |

공통: 스웜 **서비스에만** 쓸 수 있고, 서비스의 태스크를 실행하는 노드에만 전달된다.

##### 4-2. secret 동작 확인

```
$ printf 's3cr3t-pw' | docker secret create db_pw -
$ docker secret ls --format '{{.Name}} {{.ID}}'      → db_pw wv4v9rx7c3t6gw7aivtc7mcfr
$ docker secret inspect db_pw --format '{{json .Spec}}'
{"Name":"db_pw","Labels":{}}                          # 값은 나오지 않는다 (inspect 전체 출력에 's3cr3t' 0건)
```

서비스에 연결하고 컨테이너 안을 확인:

```
$ docker service create --name sapp --secret db_pw --replicas 2 --constraint node.role==worker alpine:3.20 sleep 3000
(task on w1) $ ls -l /run/secrets/
-r--r--r--  1 root root  9  db_pw
$ cat /run/secrets/db_pw           → s3cr3t-pw
$ mount | grep secrets             → tmpfs on /run/secrets/db_pw type tmpfs (ro,relatime)
$ docker inspect <task container> --format '{{json .Mounts}}'  → []     # 일반 마운트 목록에는 안 보임
```

- 컨테이너에서는 **읽기 전용 tmpfs 파일**로 보인다. 디스크에 쓰이지 않으므로 컨테이너가 사라지면 같이 사라진다.
- 환경변수(`-e`)와 달리 `docker inspect`의 `Config.Env`에 노출되지 않는다.

**배포 범위와 불변성**

```
$ docker service create --name sw1 --secret only_w1 --constraint node.hostname==w1 ...
m1 has task container: no   m2: no   w1: yes   w2: no          # 태스크가 가는 노드에만 컨테이너가 생김

$ echo x | docker secret create db_pw -      → AlreadyExists: secret db_pw already exists
$ docker secret update db_pw                 → docker: unknown command: docker secret update
$ docker secret rm db_pw                     → InvalidArgument: secret 'db_pw' is in use by the following service: sapp
```

- 같은 이름으로 다시 만들 수 없고 `update`도 없다. 사용 중인 secret은 삭제되지 않는다.

**옵션: 대상 이름, 권한, 소유자**

```
$ docker service create --name sapp2 --secret source=db_pw_v2,target=pw,uid=1000,mode=0400 ...
(container) $ ls -ln /run/secrets/  →  -r--------  1 1000 0  2  pw
```

**로테이션 (무중단 교체 패턴)**

```
$ docker secret create db_pw_v2 ...                 # 새 이름으로 생성
$ docker service update --secret-rm db_pw --secret-add source=db_pw_v2,target=db_pw sapp
verify: Service sapp converged
(container) $ cat /run/secrets/db_pw   → v2       # 앱은 같은 경로 /run/secrets/db_pw를 그대로 읽는다
```

- secret은 불변이라 **새 이름으로 만들고 `target`을 기존 경로로 맞춰** 교체한다. 이 `service update`는 태스크를 롤링 방식으로 교체한다 (앱이 재시작된다).

**크기 제한**

```
$ docker secret create ... (400,000 B)   → 성공
$ docker secret create ... (520,000 B)   → InvalidArgument: secret data must be larger than 0 and less than 512000 bytes
$ docker secret create ... (600,000 B)   → 동일 에러
```

##### 4-3. config 동작 확인

```
$ docker config create ngx_v1 nginx.conf      # 'return 200 "v1\n"' 하는 nginx 설정
$ docker config inspect ngx_v1 --format '{{json .Spec.Name}} data-b64-len={{len .Spec.Data}}'
"ngx_v1" data-b64-len=56                      # secret과 달리 inspect에 데이터가 포함됨 (base64)

$ docker service create --name cfgapp --config source=ngx_v1,target=/etc/nginx/conf.d/default.conf -p 8090:80 nginx:alpine
(container) $ ls -l /etc/nginx/conf.d/default.conf  → -r--r--r-- 1 root root 56
$ mount | grep default.conf     → tmpfs on /etc/nginx/conf.d/default.conf type tmpfs (ro,relatime)
$ wget -qO- http://127.0.0.1:8090 (아무 노드)   → v1
```

- **config의 `inspect`는 내용을 보여준다.** 비밀이 아니므로 secret과 달리 조회가 가능하다. 그래서 비밀번호를 config에 넣으면 안 된다.
- 이 환경(Engine 28.5.2)에서는 config도 `tmpfs`로 마운트됐다 (예전 문서에는 컨테이너 파일시스템에 쓰인다고 설명된 적이 있다).
- 대상 경로를 생략하면 **루트의 `/<이름>`** 에 마운트된다 (`--config plain` → `/plain`, `-r--r--r--`).

**업데이트(로테이션)**

```
$ docker config create ngx_v2 nginx2.conf        # 'v2' 반환
$ docker service update --config-rm ngx_v1 --config-add source=ngx_v2,target=/etc/nginx/conf.d/default.conf cfgapp
$ wget -qO- http://127.0.0.1:8090                → v2        # 롤링 교체 후 반영
$ docker config rm ngx_v2   → InvalidArgument: config 'ngx_v2' is in use by the following service: cfgapp
```

config도 불변이라 새로 만들어 교체한다. 설정을 바꿀 때마다 이미지를 다시 빌드하지 않아도 되는 것이 config의 장점이다.

**템플릿**

```
$ docker config create --template-driver golang tpl - <<< 'svc={{.Service.Name}} node={{.Node.Hostname}}'
(container) $ cat /tpl     → svc=tplapp node=
```

`{{.Service.Name}}`은 치환됐지만 `{{.Node.Hostname}}`은 빈 값이었다 (원인 확인 못 함).

**크기**: 이 환경에서 600,000 B와 1,000,000 B config가 모두 생성됐다. secret은 512,000 B 미만이라는 에러가 났다 (config의 상한은 확인하지 못했다. 문서에는 둘 다 500KB라는 설명이 있어 불일치로 남긴다).

##### 4-4. 보안 모델과 autolock

- secret과 config는 **Raft 로그에 저장**된다. Raft 디렉터리는 `wal-v3-encrypted`, `snap-v3-encrypted`라는 이름처럼 **암호화되어 디스크에 저장**되고, 그래서 `/var/lib/docker/swarm` 전체에서 평문 `s3cr3t-pw`를 `grep`했을 때 0건이었다 (값 자체가 암호화됐다는 확인이 아니라 평문이 없다는 확인).
- 이 암호화 키는 기본적으로 **매니저 디스크의 `/var/lib/docker/swarm/certificates`에 같이 저장**된다. 디스크 전체를 가져가면 키도 같이 가져간다. 이를 보완하는 것이 **autolock**이다.

```
$ docker swarm update --autolock=true
To unlock a swarm manager after it restarts, run the `docker swarm unlock` command ...
    SWMKEY-1-gDlIc4Yb... (이 잠금 키는 클러스터 밖에서 안전하게 보관해야 한다)

$ docker restart m1                      # 매니저 재시작
$ docker info --format 'swarm={{.Swarm.LocalNodeState}}'   → swarm=locked
$ docker node ls (m1에서)
Error ...: Swarm is encrypted and needs to be unlocked before it can be used. Please use "docker swarm unlock" to unlock it.
$ docker node ls (m2에서)  →  m1 Down Unreachable            # 잠긴 매니저는 클러스터에서 Down으로 보임

$ echo $KEY | docker swarm unlock         → 이후 swarm=active, 다시 Ready
$ docker swarm unlock-key --rotate        → 새 키 발급 (SWMKEY-1-06qsg...)
$ docker swarm update --autolock=false
```

- autolock을 켜면 **매니저가 재시작될 때 사람이 키를 입력해야** 클러스터에 복귀한다. 디스크가 탈취돼도 키 없이는 Raft 데이터를 못 연다.
- 대가: 재시작한 매니저가 잠겨 있는 동안 **쿼럼에 기여하지 못한다**. 매니저 3대 중 재시작 두 대가 잠기면 쿼럼을 잃는다. 자동 재시작이 필요한 환경에서는 운영 절차를 같이 설계해야 한다.

#### 5. `docker_gwbridge`와 `ingress` 네트워크

##### 5-1. 개요

스웜 노드에는 `init`/`join` 직후(서비스를 만들기 전에도) 다음 네트워크가 만들어져 있었다.

```
$ docker network ls --format '{{.Name}} {{.Driver}} {{.Scope}}'
bridge           bridge   local
docker_gwbridge  bridge   local      ← 스웜 전용 브리지 (노드마다 로컬)
host             host     local
ingress          overlay  swarm      ← 스웜 전체 오버레이 (라우팅 메시)
none             null     local
```

| | `ingress` | `docker_gwbridge` |
|---|---|---|
| 드라이버/범위 | overlay / **swarm** (모든 노드에 걸침) | bridge / **local** (노드별) |
| 역할 | **라우팅 메시**: 게시된 포트로 들어온 트래픽을 태스크로 분산 | 오버레이 네트워크에 연결된 컨테이너가 **노드 밖(외부)으로 나가는 통로**, ingress 진입 지점 |
| 서브넷(실측) | `10.0.0.0/24` gw `10.0.0.1`, `ingress=true`, vxlanid 4096 | `172.18.0.0/16` gw `172.18.0.1` |
| 옵션(실측) | 오버레이 | `enable_icc=false`, `enable_ip_masquerade=true`, 브리지 이름 `docker_gwbridge` |

```
$ docker network inspect ingress --format 'driver={{.Driver}} scope={{.Scope}} ingress={{.Ingress}} internal={{.Internal}} attachable={{.Attachable}} subnet=... gw=...'
driver=overlay scope=swarm ingress=true internal=false attachable=false subnet=10.0.0.0/24 gw=10.0.0.1
$ docker network inspect docker_gwbridge ...
driver=bridge scope=local subnet=172.18.0.0/16 gw=172.18.0.1 opts={"...enable_icc":"false","...enable_ip_masquerade":"true","...name":"docker_gwbridge"}
$ ip -o -4 addr (노드)   →  eth0 172.40.0.2/24, docker0 172.17.0.1/16, docker_gwbridge 172.18.0.1/16
```

##### 5-2. 컨테이너가 보는 네트워크

`-p 8080:80`으로 게시한 서비스의 태스크 컨테이너:

```
$ docker exec <task> ip -o -4 addr ; ip route
eth0  10.0.0.9/24     ← ingress 오버레이 (VXLAN)
eth1  172.18.0.3/16   ← docker_gwbridge (이 노드의 브리지)
default via 172.18.0.1 dev eth1          # 외부로 나가는 기본 경로는 gwbridge
10.0.0.0/24 dev eth0                     # ingress 대역은 eth0
```

- 오버레이 네트워크에 붙은 컨테이너는 **NIC가 2개**다. 오버레이 쪽(`eth0`)은 다른 노드의 컨테이너와 통신하고, `docker_gwbridge` 쪽(`eth1`)은 외부 인터넷으로 나가는 기본 경로(NAT, masquerade)다.
- `enable_icc=false`라 같은 gwbridge에 붙은 컨테이너끼리의 직접 통신은 막혀 있다. 컨테이너 간 통신은 오버레이로 한다.

##### 5-3. 라우팅 메시 (ingress) 동작

`web` 서비스(레플리카 2개, **m1과 m2에서만 실행**)를 `-p 8080:80`으로 게시한 뒤 **5개 노드 전부**에서 접속했다.

```
m1: HTTP/1.1 200 OK    m2: 200    m3: 200    w1: 200    w2: 200    # 태스크가 없는 m3, w1, w2에서도 응답
```

게시된 포트는 **스웜의 모든 노드에서 열리고**, 어느 노드로 요청이 들어와도 태스크가 있는 노드로 전달된다. 노드 안에서 확인한 경로:

```
요청 → 노드:8080
  ① 노드 iptables nat:  -A DOCKER-INGRESS -p tcp --dport 8080 -j DNAT --to-destination 172.18.0.2:8080
                          (172.18.0.2 = ingress_sbox 네임스페이스의 gwbridge IP)
  ② ingress_sbox 네트워크 네임스페이스 (/var/run/docker/netns/ingress_sbox):
        eth0 10.0.0.2/24 + 10.0.0.7/32 (이 서비스의 ingress VIP)    eth1 172.18.0.2/16
        mangle:  -A PREROUTING -p tcp --dport 8080 -j MARK --set-xmark 0x100
        IPVS :   FWM 256 rr  →  10.0.0.8:0 (Masq)   10.0.0.9:0 (Masq)     # 태스크 IP로 라운드 로빈
        nat  :   -A POSTROUTING -d 10.0.0.0/24 -m ipvs --ipvs -j SNAT --to-source 10.0.0.2
  ③ ingress 오버레이(VXLAN, 4789/udp)로 태스크가 있는 노드의 태스크 컨테이너(10.0.0.8, 10.0.0.9)로 전달
```

- `ingress_sbox`는 도커가 노드마다 만드는 **전용 네트워크 네임스페이스(sandbox)** 로, 안에서 IPVS가 L4 로드밸런싱을 한다 (`ipvsadm -ln`으로 확인).
- ②의 `SNAT --to-source 10.0.0.2`에서 보이듯 ingress 모드에서는 소스 주소가 변환되므로 **태스크가 원래 클라이언트 IP를 직접 볼 수 없다**. 클라이언트 IP가 필요하면 `--publish mode=host`(해당 노드의 포트만 사용, 라우팅 메시 없음)를 쓰는 방법이 있다 (이 실험에서는 실행하지 않았다).
- 라우팅 메시 덕분에 외부 로드밸런서는 **아무 노드나** 가리키면 된다.

##### 5-4. 사용자 정의 오버레이: VIP와 DNS

```
$ docker network create -d overlay --attachable t4-ov
$ docker service create --name api    --network t4-ov --replicas 3 nginx:alpine           # 기본: vip 모드
$ docker service create --name api-rr --network t4-ov --endpoint-mode dnsrr --replicas 3 nginx:alpine
$ docker service create --name cli    --network t4-ov alpine:3.20 sleep 3000

$ docker service inspect api    --format 'VIP={{json .Endpoint.VirtualIPs}}'  → [{"NetworkID":"6s5bmum...","Addr":"10.0.1.2/24"}]
$ docker service inspect api-rr --format 'VIP=...'                             → null   (mode=dnsrr)

(cli 컨테이너에서)
$ nslookup api       → 10.0.1.2                                   # 서비스 이름 = VIP (단일 주소)
$ nslookup api-rr    → 10.0.1.10, 10.0.1.9, 10.0.1.11             # dnsrr: 태스크 IP 3개를 모두 반환
$ nslookup tasks.api → 10.0.1.3, 10.0.1.5, 10.0.1.4               # tasks.<서비스>: 항상 태스크 IP 목록
$ cat /etc/resolv.conf  → nameserver 127.0.0.11, options ndots:0   # 내장 DNS
```

- **vip 모드(기본)**: 서비스 이름이 가상 IP로 해석되고 IPVS가 태스크로 분산한다. 태스크가 바뀌어도 VIP는 그대로다.
- **dnsrr**: VIP 없이 DNS가 태스크 IP를 돌아가며 반환한다. DNS 캐시 영향을 받고 ingress 라우팅 메시와 함께 쓸 수 없다.
- `--attachable`이 있으면 `docker run --network`로 독립 컨테이너도 이 오버레이에 붙을 수 있다.

#### 6. 노드 availability

##### 6-1. 상태 필드 구분

`docker node ls`에는 서로 다른 세 가지 개념이 섞여 있다.

| 필드 | 값 | 누가 정하나 | 의미 |
|---|---|---|---|
| **STATUS** | Ready / Down / Unknown | 매니저가 **heartbeat로 판단** | 노드가 살아 있는가 (연결 상태) |
| **AVAILABILITY** | **Active / Pause / Drain** | **관리자가 설정** | 스케줄러가 이 노드에 태스크를 배정해도 되는가 |
| **MANAGER STATUS** | Leader / Reachable / Unreachable | Raft | 매니저의 Raft 상태 |

##### 6-2. availability 타입

| 타입 | 새 태스크 배정 | 기존 태스크 | 용도 |
|---|---|---|---|
| **Active** (기본) | 받음 | 유지 | 정상 |
| **Pause** | **안 받음** | **유지** | 새 배정만 막기 (장애 조사, 점검 전 단계) |
| **Drain** | 안 받음 | **종료하고 다른 노드로 재스케줄** | 유지보수, 업그레이드, 노드 교체 |

```
$ docker node update --availability drain|pause|active <node>
```

##### 6-3. 실측 (replicated 서비스 `rep` 6개 + global 서비스 `glob`, 노드 5개)

```
기준:           rep: m1=1 m2=1 m3=1 w1=2 w2=1      glob: m1=1 m2=1 m3=1 w1=1 w2=1

① drain w1:
   rep: m1=2 m2=1 m3=2 w2=1                          # w1의 2개가 다른 노드(m1, m3)로 이동
   glob: m1=1 m2=1 m3=1 w2=1                         # w1의 global 태스크는 이동 없이 사라짐
   (task 이력: rep.3 w1 Shutdown, rep.4 w1 Shutdown)

② drain 중 scale rep=9:  rep: m1=2 m2=2 m3=2 w2=3   # w1에는 배정되지 않음

③ w1을 active로 복귀:     rep: m1=2 m2=2 m3=2 w2=3   # 기존 태스크는 w1으로 돌아오지 않는다 (자동 재분배 없음)
                          glob: w1=1 다시 생김        # global 서비스는 노드가 active가 되면 즉시 태스크 생성
④ scale rep=12:          rep: m1=2 m2=2 m3=2 w1=3 w2=3     # 새 태스크만 w1에 배정되어 균형

⑤ pause w2 (rep=12로 올림):
   w2는 기존 1개 유지(컨테이너 2개: rep 1 + glob 1 그대로), 새 레플리카는 m1=3 m2=3 m3=2 w1=3 으로 배정
   → pause는 기존 태스크를 건드리지 않고 새 배정만 막는다

⑥ drain m1 (매니저):
   rep: m2=3 m3=2 w1=3 w2=1,  glob에도 m1 없음
   $ docker node ls | grep m1  →  m1 Drain Reachable     # 태스크는 없지만 Raft 매니저로는 계속 참여
```

- **drain은 태스크만 비울 뿐 매니저 역할(Raft 참여)은 그대로**다. 매니저를 전용 컨트롤 플레인으로 쓰려면 drain으로 비운다.
- 태스크는 **자동으로 원래 노드로 되돌아가지 않는다.** 균형을 맞추려면 `docker service update --force <서비스>` 같은 재배포가 필요하다 (이 실험에서는 실행하지 않았다).
- 이름 그대로 drain(배수)은 "태스크를 빼내고 새 태스크를 받지 않는 상태"다.

##### 6-4. 노드 장애(Down)와의 차이

```
$ docker kill w1                    # 엔진 비정상 종료 (관리자 설정이 아니라 장애)
w1 Down after 12s                   # 매니저가 heartbeat 부재로 Down 판단
rep: m1=2 m2=3 m3=4 w2=3 (총 12 Running)      # 사라진 태스크가 다른 노드에 즉시 재생성
$ docker service ps rep → Running 12, Shutdown 5, Orphaned 4
```

- STATUS가 `Down`이 되면 스케줄러가 desired state를 맞추려고 **자동 재스케줄**한다. 관리자가 drain하는 계획된 이동과 결과는 비슷하지만 앞서 실행하던 컨테이너는 정상 종료되지 못한다.
- 노드가 돌아왔을 때 이전 태스크는 `Orphaned`로 남는 것을 확인했다.
- Dispatcher의 `HeartbeatPeriod=5s` 설정에서 12초 안에 Down이 됐다 (이 값 사이의 관계는 확인하지 않았다).

##### 6-5. 노드 제거와 역할 변경

```
$ docker node rm w1             → w1 (Down 상태라서 제거됨)
$ docker swarm leave (w2)       → Node left the swarm.
$ docker node rm w2             → FailedPrecondition: node ... is not down and can't be removed   # 매니저가 Down으로 판단하기 전
$ docker node promote w1 / demote w1   # 워커 ↔ 매니저 (3절)
$ docker node update --label-add zone=a m2
$ docker node inspect m2 --format '{{json .Spec.Labels}} role={{.Spec.Role}} avail={{.Spec.Availability}} state={{.Status.State}}'
{"zone":"a"} role=manager avail=active state=ready
$ docker service create --name pin --constraint node.labels.zone==a ...     → m2에 배치됨
```

- `swarm leave` 후에도 매니저 입장에서는 한동안 `Ready`로 보여 `node rm`이 거부된다. Down이 될 때까지 기다리거나 `--force`를 사용한다 (`--force`는 실행하지 않았다).
- 노드 레이블 + `--constraint`로 배치 위치를 제어한다.

#### 7. 확인하지 못한 부분

- **스웜 클래식의 HA/외부 KV(Consul 등) 구성, 오버레이 네트워킹, 스케줄링 전략(`binpack`, `random`)과 필터**는 실행하지 않았다. 정적 노드 목록 + spread만 확인했다. 스웜 클래식의 deprecated 시점과 "Docker 1.12 이전" 설명은 일반 지식이다.
- **롤링 업데이트(`--update-parallelism`, `--update-delay`, `--update-failure-action`, 롤백)와 헬스체크 연동**은 이번 노트에서 실험하지 않았다 (secret/config 교체에서 롤링 교체가 일어나는 것만 확인).
- **Raft 내부 설정의 의미**: `ElectionTick=10`, `HeartbeatTick=1`과 11초 재선출의 관계, 틱 길이는 추정이다. 짝수 매니저의 `2:2` 네트워크 파티션 시나리오는 직접 만들지 않았고 설명은 이론이다 (4대 중 2대 종료 실험으로 쿼럼 부족만 확인).
- **`docker swarm init --force-new-cluster`** 복구 절차는 실행하지 않았다.
- **autolock**의 **저장 키 위치와 복구**, 다른 매니저가 재시작될 때 쿼럼 영향은 설명이며 직접 시험하지 않았다.
- **Raft 암호화 확인**: `grep`으로 평문이 없다는 것만 확인했다. 어떤 키로 어떻게 암호화되는지는 확인하지 못했다.
- **config 크기 한계와 템플릿의 `{{.Node.Hostname}}`**가 빈 값인 이유는 확인하지 못했다. config가 tmpfs로 마운트되는 것은 이번 엔진 버전 기준이다.
- **`--publish mode=host`, `--endpoint-mode dnsrr`+`-p`**, 오버레이 암호화(`--opt encrypted`), 외부 로드밸런서 연동은 실행하지 않았다. 클라이언트 IP 보존 관련 설명은 SNAT 규칙에서 추정한 것이다.
- **drain 시 `--force` 재분배**, `docker node rm --force`, 스택 배포(`docker stack deploy`, 4장 컴포즈와 연결)는 실행하지 않았다.
- 이 실험은 **단일 물리 호스트의 dind 컨테이너**로 구성되어 실제 노드 간 네트워크 지연, 4789/udp 방화벽, 서로 다른 커널에서의 동작은 반영하지 못한다.
- 책 본문의 구체적인 예제·그림은 반영하지 않았다 (요청받은 토픽 + 실측 중심).
