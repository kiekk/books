### CHAPTER 2 레디스 시작하기

레디스를 **처음 띄울 때 알아야 하는 설정값들**을 영역별로 정리. `redis.conf`를 열면 수백 줄이지만, **실무에서 자주 건드리는 것은 20~30개 정도**. 기본값 + 권장값을 함께 기록.

#### 목차

- [1부. redis.conf 구조와 설정 방법](#1부-redisconf-구조와-설정-방법)
- [2부. 네트워크 및 커넥션 설정](#2부-네트워크-및-커넥션-설정)
- [3부. 일반 설정](#3부-일반-설정)
- [4부. 메모리 관리](#4부-메모리-관리)
- [5부. 영속성 — RDB (Snapshot)](#5부-영속성--rdb-snapshot)
- [6부. 영속성 — AOF](#6부-영속성--aof)
- [7부. 복제 (Replication)](#7부-복제-replication)
- [8부. 보안](#8부-보안)
- [9부. Lazy Freeing (비동기 삭제)](#9부-lazy-freeing-비동기-삭제)
- [10부. Slow Log](#10부-slow-log)
- [11부. 고급 — 인코딩/임계값](#11부-고급--인코딩임계값)
- [12부. I/O Threads](#12부-io-threads)
- [13부. CPU Affinity](#13부-cpu-affinity)
- [14부. 운영 체크리스트](#14부-운영-체크리스트)

---

## 1부. redis.conf 구조와 설정 방법

### 설정 적용 방식 3가지

| 방식 | 특징 | 사용처 |
|---|---|---|
| **redis.conf 파일** | 재시작 반영 | 영구 설정 |
| **커맨드라인 인자** | 재시작 시 적용 | `redis-server --port 6380` |
| **CONFIG SET 명령** | **런타임 즉시 반영** (일부 제한) | 운영 중 튜닝 |

**예**:
```
# 런타임 변경
CONFIG SET maxmemory 2gb
CONFIG SET maxmemory-policy allkeys-lru

# 조회
CONFIG GET maxmemory
CONFIG GET "max*"            # 와일드카드

# 변경 사항 파일에 저장
CONFIG REWRITE
```

**주의**: `CONFIG REWRITE` 하지 않으면 **재시작 시 롤백**. 운영에서 설정 변경 후 반드시 수행.

### 설정 파일 포함 (include)

```
# 공통 설정 포함
include /etc/redis/common.conf
```

- 멀티 인스턴스 운영 시 공통 설정 분리

---

## 2부. 네트워크 및 커넥션 설정

| 설정 | 기본값 | 설명 |
|---|---|---|
| **bind** | `127.0.0.1 -::1` | 수신 IP. 외부 접근은 `0.0.0.0` 또는 특정 IP |
| **port** | `6379` | 수신 포트 |
| **protected-mode** | `yes` | bind 미설정 + 패스워드 없음일 때 **로컬만 허용** |
| **timeout** | `0` | 유휴 커넥션 자동 종료(초). 0 = 비활성 |
| **tcp-keepalive** | `300` | TCP keepalive(초). 끊긴 커넥션 감지 |
| **tcp-backlog** | `511` | SYN backlog. 동시 접속 폭주 시 OS `somaxconn`도 함께 상향 |
| **unixsocket** | 없음 | Unix 도메인 소켓 사용 (예: `/tmp/redis.sock`) |
| **unixsocketperm** | `0` | 소켓 파일 권한 |

### bind 함정

- **기본 `127.0.0.1`** — 외부에서 접근 안 됨
- 외부 접근 열려면 `bind 0.0.0.0 -::1` + **반드시 패스워드/ACL 설정**
- `protected-mode yes`가 **인증 없는 외부 접근을 차단**해줌 — 끄는 건 비권장

### tcp-backlog와 OS 설정

레디스 설정만으로는 부족. OS 커널 파라미터도 함께:

```bash
# /etc/sysctl.conf
net.core.somaxconn = 65535
sysctl -p
```

---

## 3부. 일반 설정

| 설정 | 기본값 | 설명 |
|---|---|---|
| **daemonize** | `no` | 데몬 모드. systemd 사용 시 `no` 유지 권장 |
| **supervised** | `no` | `systemd`/`upstart`/`auto` — 시스템 통합 |
| **pidfile** | `/var/run/redis_6379.pid` | PID 파일 경로 |
| **loglevel** | `notice` | `debug` / `verbose` / `notice` / `warning` |
| **logfile** | `""` | 빈 문자열 = stdout, 파일 경로 지정 가능 |
| **databases** | `16` | 논리적 DB 수 (`SELECT 0~15`) |
| **always-show-logo** | `no` | 시작 로고 출력 |
| **set-proc-title** | `yes` | `ps` 명령에 역할 표시 |

### databases (논리적 DB)

- 레디스는 **DB 번호(0~15)로 네임스페이스 분리** 가능 (`SELECT 1`)
- **클러스터 모드에서는 DB 0만 사용 가능** → **멀티 DB 사용 지양 권장**
- 서비스별 분리는 **인스턴스 분리**가 올바른 방향

---

## 4부. 메모리 관리

### 핵심 설정

| 설정 | 기본값 | 설명 |
|---|---|---|
| **maxmemory** | `0` (무제한) | 최대 사용 메모리. 예: `2gb`, `8gb` |
| **maxmemory-policy** | `noeviction` | 메모리 꽉 찼을 때 **어떤 키를 지울지** |
| **maxmemory-samples** | `5` | LRU/LFU 샘플링 수 (높을수록 정확, 느림) |
| **maxmemory-eviction-tenacity** | `10` | eviction 공격성 (0~100) |

### maxmemory-policy 8가지

| 정책 | 동작 |
|---|---|
| **noeviction** (기본) | 메모리 초과 시 쓰기 명령 **거부** — 캐시로 쓸 땐 **매우 위험** |
| **allkeys-lru** | **모든 키 중 LRU** 제거 — 캐시에 적합 |
| **allkeys-lfu** | **모든 키 중 LFU** 제거 (Redis 4.0+) — 접근 빈도 반영 |
| **allkeys-random** | 모든 키 랜덤 제거 |
| **volatile-lru** | **TTL 있는 키 중 LRU** 제거 |
| **volatile-lfu** | TTL 있는 키 중 LFU 제거 |
| **volatile-random** | TTL 있는 키 랜덤 제거 |
| **volatile-ttl** | **TTL이 가장 적게 남은** 키 제거 |

**선택 가이드**:
- **순수 캐시** → `allkeys-lru` (가장 흔함)
- 접근 빈도 중요 → `allkeys-lfu`
- 세션+캐시 혼용 → `volatile-lru` (TTL 걸린 캐시만 지움)
- **절대 지우면 안 됨** → `noeviction` + 반드시 모니터링

### maxmemory 설정의 함정

- **설정 안 하면 OS 메모리 다 쓰다 OOM Kill**
- **swap 사용은 최악** — μs → ms로 지연 폭증
- 권장: **물리 메모리의 60~75%** 로 설정 (fork, 레플리카 버퍼 여유)
- `used_memory_rss` (OS 레벨) vs `used_memory` (레디스 집계) 차이 모니터링

### Fragmentation (조각화)

| 설정 | 기본값 | 설명 |
|---|---|---|
| **activedefrag** | `no` | 활성 조각 모음 (Redis 4.0+) |
| **active-defrag-ignore-bytes** | `100mb` | 임계값 |
| **active-defrag-threshold-lower** | `10` | 조각화 10%부터 시작 |
| **active-defrag-threshold-upper** | `100` | 조각화 100%에서 최대 공격 |

→ `mem_fragmentation_ratio`가 1.5 이상이면 `activedefrag yes` 고려.

---

## 5부. 영속성 — RDB (Snapshot)

### 개념

> 특정 시점의 **메모리 전체를 바이너리 파일로** 디스크에 저장. 재시작 시 로드하여 복구.

### 설정

| 설정 | 기본값 | 설명 |
|---|---|---|
| **save** | `3600 1 300 100 60 10000` | 자동 스냅샷 조건 |
| **stop-writes-on-bgsave-error** | `yes` | BGSAVE 실패 시 쓰기 거부 (안정성 우선) |
| **rdbcompression** | `yes` | LZF 압축 |
| **rdbchecksum** | `yes` | CRC64 체크섬 |
| **dbfilename** | `dump.rdb` | RDB 파일명 |
| **dir** | `./` | RDB/AOF 저장 디렉토리 |

### save 지시자 해석

`save 3600 1 300 100 60 10000`:
- **3600초(1시간) 동안 1번 이상 변경** 시 저장
- **300초(5분) 동안 100번 이상 변경** 시 저장
- **60초(1분) 동안 10000번 이상 변경** 시 저장

→ **변경량에 따라 자동 트리거**. 변경 없으면 스냅샷 안 함.

### 수동 트리거

```
SAVE       # 포그라운드 (운영 중 X)
BGSAVE     # fork로 백그라운드 (권장)
```

### RDB 장단점

**장점**:
- 백업/복원 간단 (단일 파일)
- 로드 빠름
- fork 후 자식 프로세스가 작성 → 메인 스레드 영향 최소

**단점**:
- **마지막 스냅샷 이후 데이터 유실**
- 큰 메모리 인스턴스는 fork가 지연 유발 가능 (Copy-on-Write)

---

## 6부. 영속성 — AOF

### 개념

> **모든 쓰기 명령을 로그 파일에 추가**. 재시작 시 명령어 재실행으로 복구.

### 설정

| 설정 | 기본값 | 설명 |
|---|---|---|
| **appendonly** | `no` | AOF 활성화 (운영은 `yes` 권장) |
| **appendfilename** | `"appendonly.aof"` | AOF 파일명 |
| **appendfsync** | `everysec` | fsync 주기 (`always`/`everysec`/`no`) |
| **no-appendfsync-on-rewrite** | `no` | Rewrite 중 fsync 금지 (지연 완화) |
| **auto-aof-rewrite-percentage** | `100` | 이전 Rewrite 대비 100% 커지면 자동 Rewrite |
| **auto-aof-rewrite-min-size** | `64mb` | 최소 크기 임계 |
| **aof-load-truncated** | `yes` | 끝이 잘린 AOF도 복구 시도 |
| **aof-use-rdb-preamble** | `yes` | AOF 파일 앞부분을 **RDB 포맷**으로 (하이브리드, 빠른 로딩) |

### appendfsync 3가지

| 모드 | 동작 | 유실 가능성 | 성능 |
|---|---|---|---|
| **always** | 명령마다 fsync | 거의 0 | **매우 느림** |
| **everysec** (기본) | 1초마다 fsync (백그라운드 스레드) | 최대 1초 | 좋음, **권장** |
| **no** | OS에 맡김 (보통 30초) | 수초~수십초 | 가장 빠름, 위험 |

### AOF Rewrite

- AOF 파일이 커지면 **동일 결과를 만드는 최소 명령어로 압축** 재작성
- `BGREWRITEAOF` 수동 또는 `auto-aof-rewrite-percentage`로 자동
- Rewrite도 **fork로 백그라운드** 수행

### RDB vs AOF 선택

| 요구사항 | 선택 |
|---|---|
| 최대 성능, 데이터 유실 OK | 영속화 꺼둠 or RDB only |
| 1초 이내 유실 허용 | AOF `everysec` (**가장 흔함**) |
| 거의 0 유실 | AOF `always` (성능 저하 감수) |
| 백업 간편 | RDB |
| **운영 권장** | **RDB + AOF 조합** (`aof-use-rdb-preamble yes`) |

---

## 7부. 복제 (Replication)

### 설정

| 설정 | 기본값 | 설명 |
|---|---|---|
| **replicaof** (구 `slaveof`) | 없음 | 마스터 주소 (예: `replicaof 10.0.0.5 6379`) |
| **masterauth** | 없음 | 마스터 비밀번호 (마스터에 패스워드 있을 때) |
| **masteruser** | 없음 | ACL 사용자 (Redis 6+) |
| **replica-read-only** | `yes` | 복제본은 읽기 전용 |
| **replica-serve-stale-data** | `yes` | 마스터와 끊긴 상태에서도 응답 (오래된 데이터) |
| **replica-priority** | `100` | 센티널 승격 우선순위 (0 = 승격 불가) |
| **repl-ping-replica-period** | `10` | 마스터 → 레플리카 PING 주기 |
| **repl-timeout** | `60` | 복제 연결 타임아웃 |
| **repl-disable-tcp-nodelay** | `no` | 복제 트래픽 TCP delay |
| **repl-backlog-size** | `1mb` | 재접속 시 부분 재동기화 버퍼 — **커지면 유리** |
| **repl-backlog-ttl** | `3600` | 레플리카 끊긴 후 backlog 유지 시간 |

### repl-backlog-size 중요

- 레플리카 재접속 시 **이 버퍼로 부분 재동기화(PSYNC)** 수행
- **작으면** → 끊긴 사이 쓰기가 넘치면 **전체 재동기화**(큰 비용)
- **쓰기 많으면 수백 MB로 상향**

### 복제 체크
```
INFO replication
# role:master
# connected_slaves:2
# slave0:ip=...,state=online,offset=12345,lag=0
```

---

## 8부. 보안

### 설정

| 설정 | 기본값 | 설명 |
|---|---|---|
| **requirepass** | 없음 | 레거시 패스워드 (Redis 5 이하) |
| **user** (ACL, Redis 6+) | `default on nopass ~* +@all` | ACL 사용자 정의 |
| **aclfile** | 없음 | ACL 외부 파일 |
| **rename-command** | 없음 | 위험 명령 리네임 (`rename-command FLUSHALL ""`) |
| **tls-port** | `0` | TLS 포트 |
| **tls-cert-file** | 없음 | TLS 인증서 |

### ACL (Redis 6+)

> **사용자별 권한 분리**. 명령어 패턴, 키 패턴, 채널 패턴을 제한.

```
# redis.conf 또는 CLI
ACL SETUSER alice on >alicePass ~cache:* +get +set +del
ACL SETUSER readonly on >readPass ~* +@read -@write
ACL LIST
```

- `>password` — 비밀번호
- `~pattern` — 키 패턴
- `+command` / `-command` — 명령 허용/거부
- `+@category` — 카테고리 (`@read`, `@write`, `@admin`, `@dangerous` 등)

### 위험 명령 비활성화

```
rename-command FLUSHALL ""
rename-command KEYS ""
rename-command CONFIG ""
rename-command DEBUG ""
```

→ 운영 환경에서 **실수나 공격 방지**.

---

## 9부. Lazy Freeing (비동기 삭제)

Redis 4.0+ — **큰 자료 구조 삭제를 백그라운드 스레드로** 보내 메인 블록 방지.

| 설정 | 기본값 | 설명 |
|---|---|---|
| **lazyfree-lazy-eviction** | `no` | Eviction 비동기 |
| **lazyfree-lazy-expire** | `no` | TTL 만료 삭제 비동기 |
| **lazyfree-lazy-server-del** | `no` | SET 덮어쓰기 시 기존 값 비동기 삭제 |
| **lazyfree-lazy-user-del** | `no` | DEL 명령 자체 비동기 (UNLINK처럼) |
| **lazyfree-lazy-user-flush** | `no` | FLUSHDB/FLUSHALL 비동기 |
| **replica-lazy-flush** | `no` | 레플리카 전체 동기화 시 기존 데이터 비동기 flush |

**운영 권장**: **모두 `yes`로 켜기** (큰 키 삭제 지연 방지)

---

## 10부. Slow Log

| 설정 | 기본값 | 설명 |
|---|---|---|
| **slowlog-log-slower-than** | `10000` | 10,000 μs(10 ms) 이상 걸린 명령 기록 |
| **slowlog-max-len** | `128` | 저장할 최근 명령 수 |

### 사용

```
SLOWLOG GET 10         # 최근 10개 조회
SLOWLOG LEN            # 저장된 수
SLOWLOG RESET          # 초기화
```

**실무 세팅**:
```
# 50 ms 이상, 1000개 보관
CONFIG SET slowlog-log-slower-than 50000
CONFIG SET slowlog-max-len 1000
```

---

## 11부. 고급 — 인코딩/임계값

레디스는 자료 구조별로 **내부 인코딩을 자동 전환**해 메모리 효율을 최적화. 임계값 넘으면 **밀집 인코딩 → 일반 인코딩** 전환.

| 설정 | 기본값 | 설명 |
|---|---|---|
| **hash-max-listpack-entries** | `128` | Hash 원소 수 임계 |
| **hash-max-listpack-value** | `64` | Hash 값 바이트 임계 |
| **list-max-listpack-size** | `-2` | List 노드 크기(음수 = 바이트, 양수 = 원소 수) |
| **list-compress-depth** | `0` | 양 끝 N개는 압축 안 함 |
| **set-max-intset-entries** | `512` | 정수 Set 임계 |
| **set-max-listpack-entries** | `128` | Set listpack 임계 (Redis 7.2+) |
| **zset-max-listpack-entries** | `128` | Sorted Set 원소 수 |
| **zset-max-listpack-value** | `64` | Sorted Set 값 바이트 |
| **hll-sparse-max-bytes** | `3000` | HyperLogLog sparse → dense 전환 |
| **stream-node-max-bytes** | `4096` | Stream 노드 바이트 |
| **stream-node-max-entries** | `100` | Stream 노드 엔트리 수 |

### 내부 인코딩 확인

```
OBJECT ENCODING mykey
# listpack / quicklist / ziplist / hashtable / intset / skiplist 등
```

### 왜 중요한가

- 작은 Hash는 **listpack**으로 **메모리 ~90% 절약**
- 임계 넘으면 **hashtable**로 전환 — 메모리 ↑, 하지만 **O(1) 보장**
- 임계값을 **의도적으로 크게** 잡으면 메모리 효율 ↑, **하지만 조회 O(N)** 리스크

---

## 12부. I/O Threads

Redis 6.0+. 네트워크 I/O를 멀티스레드로.

| 설정 | 기본값 | 설명 |
|---|---|---|
| **io-threads** | `1` | I/O 스레드 수 (1 = 비활성) |
| **io-threads-do-reads** | `no` | 읽기도 멀티스레드 |

### 권장

- **코어 수의 절반 ~ 전체** (예: 8코어 → 4~8)
- 명령 처리 자체는 **여전히 메인 스레드** → 명령어 원자성 유지
- **고부하 환경에서만** 효과 (낮은 부하는 오히려 오버헤드)

---

## 13부. CPU Affinity

Redis 6.0+. 특정 CPU 코어에 **스레드 바인딩** → 캐시 효율 ↑

| 설정 | 기본값 | 설명 |
|---|---|---|
| **server_cpulist** | 없음 | 메인 스레드 CPU |
| **bio_cpulist** | 없음 | 백그라운드 I/O 스레드 CPU |
| **aof_rewrite_cpulist** | 없음 | AOF Rewrite 자식 CPU |
| **bgsave_cpulist** | 없음 | BGSAVE 자식 CPU |

**예**:
```
server_cpulist 0-3
bio_cpulist 4,5
aof_rewrite_cpulist 6
bgsave_cpulist 7
```

→ 고성능 환경에서 **NUMA 최적화**에 사용.

---

## 14부. 운영 체크리스트

### 필수 변경 (기본값 그대로 쓰면 안 되는 것)

- [ ] **`maxmemory` 설정** — 미설정 시 OOM Kill
- [ ] **`maxmemory-policy allkeys-lru`** — 캐시 용도 (기본 `noeviction`은 쓰기 거부)
- [ ] **`bind` 적절히 설정** — 기본 `127.0.0.1`만 (외부 접근 필요 시)
- [ ] **`requirepass` 또는 ACL 설정** — 외부 접근 열면 반드시
- [ ] **`appendonly yes`** + **`appendfsync everysec`** — 영속성 필요한 경우
- [ ] **위험 명령 리네임** (`FLUSHALL`, `KEYS`, `CONFIG`)
- [ ] **Lazy freeing 전부 `yes`** — 큰 키 삭제 지연 방지
- [ ] **`repl-backlog-size`** 상향 — 쓰기 많은 환경
- [ ] **OS `somaxconn`** 상향 — `tcp-backlog`와 함께
- [ ] **vm.overcommit_memory = 1** — RDB/AOF fork 안정성 (리눅스 커널)
- [ ] **Transparent Huge Pages (THP) 비활성화** — fork 지연 유발

### OS 설정 (리눅스)

```bash
# /etc/sysctl.conf
vm.overcommit_memory = 1
net.core.somaxconn = 65535

# /etc/rc.local (THP 비활성화)
echo never > /sys/kernel/mm/transparent_hugepage/enabled
```

### 모니터링 지표

```
INFO memory         # used_memory, mem_fragmentation_ratio
INFO stats          # instantaneous_ops_per_sec, rejected_connections
INFO replication    # role, connected_slaves, lag
INFO persistence    # rdb_last_save_time, aof_last_rewrite_time_sec
INFO clients        # connected_clients, blocked_clients
LATENCY LATEST      # 각종 지연 포인트
MEMORY STATS        # 메모리 상세
```

---

### 요약

- **설정은 3가지 방식**: `redis.conf` / `--cmdline` / `CONFIG SET` (런타임). 운영 변경 후 `CONFIG REWRITE` 필수
- **네트워크**: `bind`, `port`, `tcp-backlog`, `tcp-keepalive`. `protected-mode`는 켜두기
- **메모리**: `maxmemory` 설정 필수, `maxmemory-policy allkeys-lru`가 캐시 표준
- **RDB**: `save` 지시자로 자동 스냅샷, fork 기반 백그라운드. 빠른 로딩/간단 백업
- **AOF**: `appendonly yes` + `appendfsync everysec`이 표준. **RDB+AOF 조합** 권장
- **복제**: `replicaof`, `repl-backlog-size`가 재동기화 성능 결정
- **보안**: ACL(Redis 6+), 위험 명령 rename, TLS
- **Lazy Freeing**: 전부 `yes`로 켜기 — 큰 키 삭제 지연 방지
- **Slow Log**: `slowlog-log-slower-than 50ms`로 실무 세팅
- **인코딩**: 작은 자료 구조는 listpack으로 메모리 ~90% 절약
- **I/O Threads**: 코어 많은 환경에서 네트워크 병목 완화 (명령 실행은 여전히 단일)
- **운영 필수 OS 설정**: `vm.overcommit_memory=1`, `somaxconn` 상향, THP 비활성화
