### CHAPTER 11 보안

Redis 는 **"신뢰할 수 있는 내부 네트워크에서 쓰는 빠른 인메모리 저장소"** 로 설계되었고, 기본 철학은 **보안보다 성능·단순함** 이다. 인터넷에 노출된 Redis 가 **데이터 삭제(`FLUSHALL`), 랜섬, 악성 코드 실행 경로(`CONFIG SET dir` + `SAVE`)** 로 악용되는 사고가 반복되어 왔기 때문에, 현재는 **단계적 방어** 를 권장한다.

```
 ① 네트워크 차단   : bind / 방화벽·보안그룹 / VPC / (필요시 TLS)
 ② 최후 방어선      : protected mode  (인증 없이 외부 공개되는 사고 방지)
 ③ 인증             : requirepass(=default 유저) / ACL 유저별 비밀번호
 ④ 명령어 제어      : ACL(권장) > rename-command(레거시) ; 위험 명령은 enable-*-command 로 기본 차단
 ⑤ 감사/운영        : ACL LOG, 최소 권한, 계정 분리(앱/운영/복제)
```

> 이 장의 동작은 **로컬 Redis 7.2.6** 에서 직접 확인했다. 확인한 부분은 `✅ 검증`, 문서/지식 기준은 `(문서 기준)` 으로 표시했다. (외부 접속 테스트는 로컬 LAN IP 로 수행)

#### 목차

- [1부. bind — 어느 인터페이스로 받을 것인가](#1부-bind--어느-인터페이스로-받을-것인가)
- [2부. protected mode](#2부-protected-mode)
- [3부. 인증 — requirepass 와 default 유저](#3부-인증--requirepass-와-default-유저)
- [4부. 커맨드 제어 — rename-command 와 enable-*-command](#4부-커맨드-제어--rename-command-와-enable--command)
- [5부. ACL](#5부-acl)
- [6부. dangerous 카테고리 — 유형별 위험 명령어](#6부-dangerous-카테고리--유형별-위험-명령어)
- [7부. dangerous 가 아니지만 위험한 것들](#7부-dangerous-가-아니지만-위험한-것들)
- [8부. 보안 체크리스트](#8부-보안-체크리스트)

---

## 1부. bind — 어느 인터페이스로 받을 것인가

### 개념

`bind` 는 Redis 가 **클라이언트 연결을 받아들일 서버 측 네트워크 인터페이스(로컬 IP 주소)** 를 지정한다. **"접속을 허용할 클라이언트 IP"가 아니라 "내 서버의 어느 NIC(주소)에서 listen 할지"** 이다 (클라이언트 IP 제한은 방화벽/보안그룹/ACL 의 영역).

```
bind 127.0.0.1 -::1           # redis.conf 기본 예시: IPv4/IPv6 loopback 만
bind 10.0.1.5 127.0.0.1       # 내부망 NIC + loopback
bind * -::*                   # 모든 IPv4 + 모든 IPv6   ← 설정 없이 기동한 서버의 기본값 ✅ 확인
bind 0.0.0.0                  # 모든 IPv4 인터페이스
```

### 특징

| 항목 | 설명 |
|---|---|
| **여러 주소** | 공백으로 여러 개 지정 가능 |
| **`-` 접두사** | 해당 주소 **bind 실패를 무시**(`-::1` : IPv6 가 없는 호스트에서도 기동). 접두사 없이 실패하면 **서버 기동 실패** |
| **기본값** | 설정 파일 없이 기동하면 `* -::*` ✅ 검증(`CONFIG GET bind`). 배포 `redis.conf` 는 `bind 127.0.0.1 -::1` 로 시작하는 것이 일반적(7.0+ 문서 기준) |
| **`bind` 에 loopback 만** | **외부 접속 자체가 불가** (`Connection refused`) ✅ 검증: `bind 127.0.0.1` 서버에 LAN IP 로 접속 → `Could not connect ... Connection refused` |
| **`CONFIG SET bind`** | 런타임 변경 가능(7.0+). 변경 시 소켓을 다시 열어 **기존 연결 유지, 새 listen 주소 반영** |
| **`bind-source-addr`** | 서버가 **나가는 연결(복제, MIGRATE)** 의 **출발지 IP** 지정(멀티 NIC) |
| **`port 0`** | TCP 포트를 열지 않음 → `unixsocket` 만 사용(동일 호스트 앱·사이드카) |
| **`tls-port`** | TLS 전용 포트(일반 `port 0` 과 함께 쓰면 평문 차단) |

### 운영 팁

- **가장 단순하고 강력한 방어는 "내 서버가 외부에 listen 하지 않는 것"** — 앱이 같은 호스트면 `bind 127.0.0.1` 또는 unix socket
- 원격 접속이 필요하면 **프라이빗 서브넷의 NIC IP** 만 `bind` 하고, **보안그룹으로 앱 서버 IP/SG 만 허용**
- `bind 0.0.0.0` 은 **"모든 NIC 에서 받는다"** 일 뿐이며 **공인 IP 가 붙은 NIC 가 있으면 인터넷에 열린다** → 반드시 방화벽/보안그룹과 함께
- **Docker**: 컨테이너 안에서는 보통 `bind 0.0.0.0` 이 필요하고(포트 매핑 대상 NIC 이 컨테이너 eth0) **호스트에서 `-p 6379:6379` 는 모든 호스트 인터페이스에 공개**한다(Docker 는 iptables 로 방화벽을 우회). **`-p 127.0.0.1:6379:6379`** 로 제한 (문서/경험 기준)
- 클러스터는 **데이터 포트와 버스 포트 모두** 노드 간 접근이 가능해야 하므로 `bind` 는 **노드 간 통신 NIC** 를 포함해야 함(10장)

---

## 2부. protected mode

### 개념

**"비밀번호도 없고(= default 유저에 인증 없음) 주소 제한도 없이 외부에 열린 Redis" 를 막는 마지막 안전장치.**

```
protected-mode yes   (기본값 ✅ 확인)
```

발동 조건(문서 기준) — 아래가 **모두** 참이면 **loopback(127.0.0.1, ::1, unix socket) 이외의 클라이언트 연결 시 명령을 거부**:
1. `protected-mode yes`
2. **default 유저에 비밀번호가 없음**(`requirepass` 미설정, `nopass`)
3. (과거 문서) `bind` 를 명시하지 않았거나 모든 인터페이스 대상

### 동작 ✅ 검증 (LAN IP 192.168.0.27 에서 접속)

| 서버 설정 | 접속 | 결과 |
|---|---|---|
| 기본(`bind * -::*`, protected-mode yes, 비번 없음) | **loopback** | `PONG` |
| 〃 | **LAN IP** | **`DENIED Redis is running in protected mode ...`** |
| `bind 0.0.0.0 --protected-mode yes` (비번 없음) | LAN IP | `DENIED ...` (`bind` 를 열어도 막힘) |
| 위 + **`requirepass s3cret`** | LAN IP, 비번 없이 | **`NOAUTH Authentication required.`** (protected mode 는 풀림, 대신 인증 필요) |
| 〃 | LAN IP, `-a s3cret` | `PONG` |
| `protected-mode no` + 비번 없음 | LAN IP | **`PONG` — 완전 개방(위험)** |

- **에러 메시지** 자체가 해결책 4가지를 안내한다: ① loopback 에서 `CONFIG SET protected-mode no` ② 설정 파일 수정 후 재시작 ③ `--protected-mode no` 로 기동 ④ **default 유저에 비밀번호 설정(권장)**
- **`DENIED` 와 `NOAUTH` 의 차이**: `DENIED` = protected mode 가 연결 차단. `NOAUTH` = 인증 필요(protected mode 와 무관)

### 주의 (흔한 오해/사고)

| 오해 | 실제 |
|---|---|
| "protected mode 가 있으니 안전하다" | **`protected-mode no` 로 바꾸는 순간 무방비.** 개발 중 연결 안 돼서 `no` 로 바꾸고 **그대로 운영에 배포**하는 것이 가장 흔한 사고 |
| "`bind 0.0.0.0` 이면 열려 있는 것" | protected mode 가 `yes` 이고 비번이 없으면 **외부는 여전히 `DENIED`** |
| "비밀번호를 걸었으니 protected mode 는 의미 없다" | 비번이 있으면 protected mode 는 **해제된 것으로 취급**(인증이 대신 방어) |
| **프록시/Docker/NAT 뒤** | Redis 입장에서 클라이언트 IP 가 **프록시/브리지 IP** → loopback 이 아니므로 `DENIED`. 이를 피하려 `no` 로 바꾸기 쉬움 → **비번 설정이 정답** |
| 센티널/클러스터 | 센티널에도 `protected-mode` 가 있다. 센티널 노드 간 통신이 막히면 **비번/ACL 설정** 으로 해결, 무작정 `no` 금지 |

---

## 3부. 인증 — requirepass 와 default 유저

- **`requirepass <pw>`** = **`default` 유저의 비밀번호 설정**(Redis 6.0+ 내부적으로 동일) ✅ 검증: `CONFIG SET requirepass pw` 후 `ACL GETUSER default` 에 `passwords <sha256>` 가 생김
- 비밀번호는 **SHA-256 해시로 저장**(`ACL LIST` 에 `#해시`) ✅ 확인. 단 **`redis.conf` 의 `requirepass` 줄과 `CONFIG GET requirepass`, `MONITOR`, 슬로우로그, 클라이언트 로그, `AUTH` 명령 인자는 평문** → 노출 경로 관리 필요
- `AUTH <pw>` (default 유저) / `AUTH <user> <pw>` (6.0+)
- `redis-cli -a <pw>` 는 `Warning: Using a password with '-a' ... can be insecure` 경고 → **`REDISCLI_AUTH` 환경변수** 또는 `--askpass`(7.x)
- 비밀번호는 **길고 무작위**로: `ACL GENPASS [bits]` ✅ 검증 (기본 256 bit → 64 hex, `128` → 32 hex) — Redis 는 **초당 수십만 회 시도(brute force)가 가능**하므로 짧은 비번은 위험
- **복제/센티널/클러스터 인증**: `masterauth`(+ `masteruser` 6.0+), 센티널 `sentinel auth-pass` / `auth-user`, 클러스터 노드 간 동일 계정 필요 → **계정 변경 시 전 노드 일괄 적용**

---

## 4부. 커맨드 제어 — rename-command 와 enable-*-command

### 4-1. `rename-command` (레거시 방식, 설정 파일 전용)

```
rename-command FLUSHALL ""              # 완전 비활성화
rename-command CONFIG   CFG_x9a_secret   # 추측하기 어려운 이름으로 변경
```

✅ 검증 (서버 기동 시 `--rename-command FLUSHALL ""`, `CONFIG CFG_x9a`, `KEYS KEYS_adm`):
```
> FLUSHALL        → ERR unknown command 'flushall', with args beginning with:
> CONFIG GET port → ERR unknown command 'config'...
> CFG_X9A GET port → 7103                     (바꾼 이름으로는 동작)
> KEYS *          → ERR unknown command 'keys'...
> KEYS_ADM *      → a
```

| 장점 | 단점/한계 |
|---|---|
| 간단, **ACL 이전(6.0 미만)** 에도 사용 가능 | **전역 적용**: "앱은 못 쓰고 운영자만 쓰게" 같은 **사용자별 구분 불가** |
| 이름 비밀을 아는 사람만 사용 가능 | **런타임 변경 불가**(재시작 필요) |
| | **AOF / 복제 호환성**: 마스터에서 이름이 바뀐 명령이 AOF·복제 스트림에 **바뀐 이름으로 기록** → 레플리카/AOF 재생 서버도 **같은 rename 설정** 필요(문서 기준) |
| | **센티널/클러스터/툴(`redis-cli --cluster`, 클라이언트 라이브러리)** 이 `CONFIG`, `CLUSTER` 등을 호출하다 **실패** |
| | `COMMAND`, `ACL CAT` 등에서 관리가 일관되지 않음, **이름을 설정 파일에 평문 보관**(= 공유 비밀) |

→ **Redis 6.0+ 에서는 `rename-command` 대신 ACL 사용을 권장.** (공식 문서도 rename-command 는 호환성 위주로 유지)

### 4-2. 위험 명령을 **기본 차단** 하는 설정 (7.0+) ✅ 검증

| 설정 | 기본값 | 의미 |
|---|---|---|
| **`enable-protected-configs`** | `no` | `CONFIG SET dir`, `dbfilename`, `appendfilename`, `logfile`, `aclfile` 등 **파일 경로 관련(PROTECTED) 설정 변경 차단**. `yes` / **`local`**(loopback 연결만 허용) |
| **`enable-debug-command`** | `no` | `DEBUG` 명령 차단. `yes`/`local` |
| **`enable-module-command`** | `no` | `MODULE LOAD/UNLOAD` 차단. `yes`/`local` |

✅ 검증 (기본값 `no` 상태, loopback 연결):
```
CONFIG SET dir /tmp          → ERR CONFIG SET failed (possibly related to argument 'dir') - can't set protected config
CONFIG SET dbfilename x.rdb  → ERR ... can't set protected config
DEBUG SLEEP 0                → ERR DEBUG command not allowed. If the enable-debug-command option is set to "local", ...
MODULE LIST                  → (빈 목록, 조회는 허용)
```
✅ `--enable-protected-configs local --enable-debug-command local --enable-module-command no`:
`CONFIG SET dir /tmp` → OK, `DEBUG SLEEP 0` → OK (loopback), `MODULE LOAD /x.so` → `ERR MODULE command not allowed...`

- **왜 중요한가**: 과거 공격은 **`CONFIG SET dir ~/.ssh` + `CONFIG SET dbfilename authorized_keys` + 키 `SET` + `SAVE`** 로 **Redis 가 SSH 공개키 파일을 쓰게** 하거나, cron 파일을 쓰게 하는 방식이었다. 7.0 의 protected-configs 가 이 경로를 **기본 차단**한다.
- `local` : "같은 호스트 콘솔에서 운영자는 쓰되 원격은 차단" — 디버깅용 절충
- 이 세 설정은 **`redis.conf` 에서만 변경 가능**(런타임 `CONFIG SET` 불가)

---

## 5부. ACL

### 5-1. 개념 (6.0+)

**사용자(User)별로 ① 비밀번호 ② 실행 가능한 명령 ③ 접근 가능한 키 ④ 접근 가능한 Pub/Sub 채널 을 제한**하는 권한 시스템. 기본 `default` 유저 = **`on nopass +@all ~* &*`** (모든 것 허용, 비번 없음) ✅ 확인.

```
> ACL GETUSER default
flags: on nopass sanitize-payload   passwords: (empty)
commands: +@all     keys: ~*     channels: &*     selectors: (empty)
```

### 5-2. 규칙 문법

| 규칙 | 의미 |
|---|---|
| `on` / `off` | 유저 활성/비활성 (`off` 유저는 인증 실패 `WRONGPASS ... or user is disabled` ✅ 검증) |
| `>pass` / `<pass` | 비밀번호 추가/제거 (여러 개 가능 → **무중단 비밀번호 교체**: 새 비번 추가 → 클라이언트 전환 → 옛 비번 제거) |
| `#<sha256>` / `!<sha256>` | 해시로 직접 등록/제거 |
| `nopass` / `resetpass` | 비밀번호 없이 허용 / 모든 비번 제거 |
| `~pattern` | **키 패턴**(glob) 허용. `~*` 전체, `allkeys` |
| `%R~pattern` / `%W~pattern` | **읽기 전용 / 쓰기 전용** 키 패턴 (7.0+, `%RW~` = `~`) |
| `&pattern` | **Pub/Sub 채널 패턴**. `allchannels`. 기본 `resetchannels` 상태에선 **모든 채널 차단**(6.2+) |
| `+cmd` / `-cmd` | 명령 허용/차단 |
| `+@cat` / `-@cat` | **카테고리** 허용/차단 (`+@all`) |
| `+cmd\|sub` | **서브커맨드** 허용 (`+config\|get`) / 첫 인자 허용(`+select\|0`) |
| `(규칙...)` | **Selector**(7.0+): 한 유저에 **독립적인 권한 묶음(OR)** 을 추가 |
| `reset` | 유저를 초기 상태(off, 모든 권한 제거)로 리셋 ✅ 검증 |
| `sanitize-payload` / `skip-sanitize-payload` | `RESTORE` 페이로드 검증 여부(기본 sanitize) |

> **규칙은 왼쪽→오른쪽 순서로 적용**된다. `-@all +@read` 와 `+@read -@all` 은 결과가 다르다. `ACL SETUSER` 후 `ACL GETUSER`/`ACL LIST` 가 **정규화된 최종 형태**를 보여준다.

### 5-3. 카테고리 ✅ 검증 (7.2.6 `ACL CAT`)

```
keyspace read write set sortedset list hash string bitmap hyperloglog geo stream pubsub
admin fast slow blocking dangerous connection transaction scripting
```
- 자료구조 카테고리(`@string`, `@hash`…), 동작 성질 카테고리(`@read`, `@write`, `@fast`, `@slow`, `@blocking`), **위험도 카테고리(`@admin`, `@dangerous`)**, 기능 카테고리(`@pubsub`, `@scripting`, `@transaction`, `@connection`, `@keyspace`)
- 개수 ✅ 확인: `@admin` 65, **`@dangerous` 75**, `@scripting` 21, `@keyspace` 34, `@connection` 38 (7.2.6 기준, 버전마다 다름)
- 한 명령은 **여러 카테고리에 속함**(예: `FLUSHALL` = `@keyspace @write @slow @dangerous`)
- `ACL CAT <category>` 로 소속 명령 조회

### 5-4. 실습 — 유저별 권한 검증 ✅ 검증

```
ACL SETUSER app on >apppw ~app:* &app:* +@read +@write +@connection -@dangerous
ACL SETUSER readonly on >ropw ~* +@read +ping +auth
ACL SETUSER ops on >opspw ~* &* +@all -@dangerous +info +config|get
ACL SETUSER nopassuser on nopass ~tmp:* +get +set
ACL SETUSER sel on >selpw %R~read:* %W~write:* +@all -@dangerous
```

| 유저 | 명령 | 결과 |
|---|---|---|
| app | `SET app:1 v` | OK |
| app | `SET other:1 v` | **`NOPERM No permissions to access a key`** |
| app | `FLUSHALL` / `KEYS *` / `CONFIG GET port` | **`NOPERM User app has no permissions to run the 'flushall' command`** |
| app | `ACL WHOAMI` | NOPERM (`@connection` 에 `acl\|whoami` 없음 — `acl` 은 `@slow`) |
| app | `PUBLISH other hi` | NOPERM — 허용 명령(`+publish`)이 없어서(채널 `&` 이전에 **명령 권한** 필요) |
| readonly | `GET` / `SET` | 정상 / **NOPERM** |
| ops | `CONFIG GET port` / `CONFIG SET maxmemory 1mb` / `FLUSHALL` | 허용 / **NOPERM**(`config\|set` 은 `@dangerous` 소속이라 `-@dangerous` 로 차단) / NOPERM |
| pub(`&news:*`, `+publish`) | `PUBLISH news:1 x` / `PUBLISH other x` | 정상 / **`NOPERM No permissions to access a channel`** |
| sel(`%R~read:*`, `%W~write:*`) | `GET read:a` / `SET read:a` / `SET write:a` / `GET write:a` | 정상 / **NOPERM** / OK / **NOPERM** (R/W 분리 동작) |
| nopassuser | 아무 비번 + `SET tmp:1` | **OK**(`nopass` 는 어떤 비번도 허용) |
| offuser | `AUTH` | `WRONGPASS ... or user is disabled` |

### 5-5. 운영에 중요한 동작 ✅ 검증

| 동작 | 결과 |
|---|---|
| **`ACL SETUSER` 로 권한 제거** | **이미 연결된 세션에도 즉시 반영**(`ops2` 가 `flushall` 반복 실행 중 `-flushall` → 이후 호출 `NOPERM`) |
| **`ACL DELUSER`** | 해당 유저의 **기존 연결이 끊김**(`Error: Server closed the connection`) |
| **`ACL DRYRUN <user> <cmd...>`** (7.0+) | 실행 없이 **권한 여부 확인**: `ACL DRYRUN app flushall` → `User app has no permissions to run the 'flushall' command`, `ACL DRYRUN app set other v` → `...no permissions to access the 'other' key` |
| **`ACL LOG`** | 거부 이력(`reason`: command/key/channel/auth, `object`, `username`, `client-info`, `entry-id`). `acllog-max-len`(기본 128) ✅ 확인 → **보안 감사/권한 설계 디버깅**의 핵심 |
| **`ACL WHOAMI`** | 현재 유저 확인 |
| **`ACL GENPASS`** | 무작위 비번 생성 |
| **스크립트(EVAL) 내부 호출도 키 권한 검사** | `~app:*` 유저의 `EVAL "redis.call('set','other:2','x')"` → **`ERR ACL failure in script: No permissions to access a key`** ✅ 검증 (6.2/7.0+ 동작) |

### 5-6. ACL 영속화 — 두 방식 중 하나만

| 방식 | 설정 | 특징 |
|---|---|---|
| **`aclfile`** | `aclfile /etc/redis/users.acl` | **`ACL SAVE`(파일에 저장) / `ACL LOAD`(파일에서 재로드)** 사용 가능. 유저는 **파일에만** 정의(redis.conf 의 `user ...` 와 **혼용 불가**) |
| **redis.conf 에 `user ...` 줄** | `user app on >pw ~app:* +@read` | `ACL SAVE/LOAD` **사용 불가**, `CONFIG REWRITE` 로 저장 |

✅ 검증:
- `aclfile` 없이 `ACL SAVE` / `ACL LOAD` → `ERR This Redis instance is not configured to use an ACL file...`
- `aclfile` 로 기동(`user default off`, admin, repl 정의) → **default 가 `off` 이므로 익명 접속은 `NOAUTH`**, `admin` 으로 인증해야 사용 가능
- `ACL SETUSER newu ...` → `ACL SAVE` 후 파일에 **`newu` 가 반영**(비밀번호는 `#<sha256>` 로 기록, 파일 안의 주석/포맷은 재작성되며 **사라질 수 있음**). 이후 `ACL SETUSER temp on` 만 하고 `ACL LOAD` → **저장 안 한 `temp` 는 사라짐**(`ACL USERS`: admin default newu repl)
- **인증 정책**: `user default off` 로 **익명 접속을 끄고** 전용 계정만 쓰는 것이 가장 안전. 단 **`default` 를 끄면 `AUTH <pw>`(유저 없이) 형태의 레거시 클라이언트/툴이 실패**하므로 사전 점검

### 5-7. 복제/클러스터/센티널 계정 ✅ 검증

최소 권한 복제 계정:
```
user repl on >replpw +psync +replconf +ping       (users.acl)
레플리카:  masteruser repl / masterauth replpw
```
- 이 권한만으로 **레플리카가 `master_link_status:up`**, 전체 동기화 완료(`sync: Finished with success`) ✅ 검증. 같은 계정으로 `GET`/`INFO` 는 **NOPERM**(데이터 접근 불가) → **"복제 전용" 계정이 가능**
- 반대로 **`masteruser` 없이 default 가 off** 이면 레플리카가 `Unexpected reply to PSYNC from master: -NOAUTH Authentication required.` 로 **복제 실패**(`master_link_status:down`) ✅ 검증
- 센티널: `sentinel auth-user/auth-pass`, 클러스터: 모든 노드가 같은 계정 정의 필요(`cluster nodes` 간 버스는 별개 프로토콜, 클라이언트 측 인증은 노드별)

### 5-8. 흔한 함정 ✅ 검증 포함

| 함정 | 설명 |
|---|---|
| **`+@write` 에 `FLUSHALL`·`FLUSHDB` 가 포함됨** | `+eval +@read +@write ~app:*` 유저(`scr`)가 **스크립트 안에서 `redis.call('flushall')` 실행 성공** ✅ 검증. 키 패턴(`~app:*`)은 **`FLUSHALL` 을 막지 못한다**(키 인자가 없는 명령) → **`-@dangerous` 를 함께** 지정해야 한다 |
| **`-@all` 부터 시작** | 새 유저의 기본값은 **명령 없음**. 허용 목록(allow-list) 방식으로 설계 |
| **`requirepass` 와 ACL 혼용** | `requirepass` 는 **default 유저 비번** 설정일 뿐. default 가 `+@all ~*` 이면 **비번만 알면 전부 가능** |
| **`@admin` 만 막고 `@dangerous` 를 놓침** | `@dangerous` 에는 `@admin` 이 아닌 명령(`KEYS`, `FLUSHALL`, `FLUSHDB`, `SORT`, `SWAPDB`, `RESTORE`, `MIGRATE`, `INFO`) 이 있다 ✅ 확인 |
| **클라이언트 라이브러리/툴이 쓰는 명령** | 앱 계정에 `CLIENT SETNAME/SETINFO`(`@connection`), `HELLO`, `PING`, `SELECT`, 클러스터 클라이언트의 `CLUSTER SLOTS/SHARDS` 가 필요할 수 있음 → `ACL LOG` 로 거부 항목 확인 후 보완 |
| **Pub/Sub `&`** | 6.2+ 기본 `resetchannels` → **`&` 를 안 주면 SUBSCRIBE/PUBLISH 가 막힘** |
| **ACL 변경은 즉시 반영** | 운영 중 실수로 권한을 줄이면 **즉시 장애**. `ACL DRYRUN` 으로 사전 확인 |

---

## 6부. dangerous 카테고리 — 유형별 위험 명령어

> ✅ 검증: **7.2.6 `ACL CAT dangerous` = 75개**(서브커맨드 포함). 아래는 이를 **위험 유형별**로 분류한 것. 버전에 따라 목록이 달라지므로 **`ACL CAT dangerous` 를 직접 확인**할 것.

### 6-1. 데이터 삭제·덮어쓰기 (가용성/데이터 손실)

| 명령 | 위험 |
|---|---|
| **`FLUSHALL [ASYNC]`** | **전 DB 데이터 삭제**. `ASYNC` 는 백그라운드 삭제일 뿐 **삭제 자체는 동일** |
| **`FLUSHDB [ASYNC]`** | 현재 DB 전체 삭제 |
| **`SWAPDB a b`** | 두 DB 의 **내용을 맞바꿈** → 앱이 엉뚱한 데이터를 보게 됨 |
| **`RESTORE` / `RESTORE-ASKING`** | 직렬화된 값으로 **키를 덮어쓰기**(`REPLACE`). 7.0+ `sanitize-payload` 가 변조된 페이로드를 검증하지만 **원본 키 덮어쓰기 위험** 은 그대로 |
| **`MIGRATE`** | **다른 서버로 키를 이동**(기본 원본 삭제) — 데이터 유출/삭제 + **블로킹**(대형 키) |
| **`DEBUG`**(하위 다수) | `DEBUG RELOAD`, `DEBUG SLEEP`, `DEBUG SEGFAULT`, `DEBUG POPULATE` 등 **서버를 멈추거나 죽임** |

### 6-2. 서버 설정·종료·영속화 제어 (서버 장악/DoS)

| 명령 | 위험 |
|---|---|
| **`CONFIG SET`** | 거의 모든 설정 변경 — `dir`/`dbfilename`(임의 파일 쓰기, 7.0+ protected), `requirepass`, `maxmemory`, `bind`, `appendonly` 등 |
| `CONFIG GET` / `CONFIG REWRITE` / `CONFIG RESETSTAT` | 설정(경로·비밀번호 일부) 조회 / **설정 파일 덮어쓰기** / 통계 초기화 |
| **`SHUTDOWN [NOSAVE]`** | **서버 종료**(`NOSAVE` 는 데이터 유실) |
| **`SAVE`** | **동기 RDB 저장 — 메인 스레드 블로킹**(전 요청 중단) |
| `BGSAVE` / `BGREWRITEAOF` | `fork` 부하·메모리 급증(COW), 디스크 쓰기 |
| `LASTSAVE` | 마지막 저장 시각 조회(정보 노출 수준) |
| **`DEBUG`**, **`MODULE LOAD/LOADEX/UNLOAD/LIST`** | 임의 **공유 라이브러리 로드 = 임의 코드 실행**. 7.0+ `enable-*-command` 로 기본 차단 |
| `FAILOVER` | 마스터→레플리카 전환 트리거 |

### 6-3. 복제·클러스터 토폴로지 조작 (구성 파괴/데이터 탈취)

| 명령 | 위험 |
|---|---|
| **`REPLICAOF` / `SLAVEOF`** | **다른 서버의 레플리카로 전환** → 내 데이터가 **공격자 서버 데이터로 대체**되거나, 반대로 공격자 서버를 마스터로 삼아 **악성 데이터/모듈** 이식(과거 침해 사례) |
| `SYNC` / `PSYNC` / `REPLCONF` | 복제 프로토콜 — 임의 클라이언트가 **전체 데이터 덤프를 받아가는 경로**(복제 계정에만 허용) ✅ 5-7 |
| `ROLE` | 복제 토폴로지 노출 |
| **`CLUSTER` 하위(약 20개)**: `ADDSLOTS/DELSLOTS/ADDSLOTSRANGE/DELSLOTSRANGE`, `SETSLOT`, `MEET`, `FORGET`, `RESET`, `FAILOVER`, `REPLICATE`, `SET-CONFIG-EPOCH`, `BUMPEPOCH`, `FLUSHSLOTS`, `SAVECONFIG`, `COUNT-FAILURE-REPORTS`, `REPLICAS/SLAVES` | **슬롯 소유권/노드 구성 변경** → 클러스터 분할, 슬롯 소실(10장) |

### 6-4. 대량 조회·고비용 연산 (성능 DoS)

| 명령 | 위험 |
|---|---|
| **`KEYS pattern`** | O(N) 전체 키 순회 → **메인 스레드 블로킹**(3장). 대안 `SCAN` |
| **`SORT` / `SORT_RO`** | O(N+M·log M), `BY`/`GET` 으로 **외부 키 참조**(`BY other:*`) → 느리고 **다른 키 데이터 노출** 가능 |
| `INFO` | 서버 상세 정보(메모리, 경로, 설정, 복제) **정보 노출** + 대량 섹션은 부하 |
| `MONITOR` | **모든 명령을 실시간 노출**(비밀번호, 민감 데이터 포함) + 처리량 크게 감소 |
| `SLOWLOG GET/LEN/RESET` | 느린 쿼리 로그(인자에 **민감 데이터**) 조회/삭제(증거 인멸) |
| `LATENCY` 하위(`DOCTOR/GRAPH/HISTOGRAM/HISTORY/LATEST/RESET`) | 지연 정보 노출/초기화 |
| `PFDEBUG` / `PFSELFTEST` | HyperLogLog 내부 디버그/자체 테스트 |

### 6-5. 클라이언트·권한 관리

| 명령 | 위험 |
|---|---|
| `CLIENT KILL` / `CLIENT LIST` / `CLIENT PAUSE` / `CLIENT UNPAUSE` / `CLIENT UNBLOCK` / `CLIENT NO-EVICT` | **다른 클라이언트 연결 강제 종료**, **전 쓰기 중단(PAUSE)**, 접속 IP·명령·유저 노출(`CLIENT LIST`) |
| **`ACL SETUSER/DELUSER/LOAD/SAVE`** | **권한 변경(권한 상승)**, 유저 삭제, 파일 덮어쓰기 |
| `ACL GETUSER/LIST/USERS/LOG/DRYRUN` | 권한 구조·거부 이력 **노출** |

### 6-6. 한눈에 보는 요약 표

| 유형 | 대표 명령 | 영향 | 권장 |
|---|---|---|---|
| 삭제/덮어쓰기 | FLUSHALL, FLUSHDB, SWAPDB, RESTORE, MIGRATE | 데이터 손실 | 앱 계정 **차단**, 운영 계정도 신중 |
| 서버 제어 | CONFIG SET, SHUTDOWN, SAVE, DEBUG, MODULE | 장악/중단 | 운영자만, `enable-*` 로 기본 차단 |
| 토폴로지 | REPLICAOF/SLAVEOF, CLUSTER *, FAILOVER | 구성 파괴/탈취 | 운영자만 |
| 고비용/노출 | KEYS, SORT, INFO, MONITOR, SLOWLOG | 성능/정보 노출 | 앱에선 `SCAN`, 모니터링 계정에 `info`·`slowlog` 만 허용 |
| 클라이언트/권한 | CLIENT KILL/PAUSE, ACL * | 서비스 중단/권한 상승 | 운영자만 |

---

## 7부. dangerous 가 아니지만 위험한 것들

`ACL CAT dangerous` 에 **없지만** 운영상 주의가 필요한 명령(7.2.6 기준, 직접 확인한 목록 외 항목은 문서 기준):

| 명령 | 이유 |
|---|---|
| **`EVAL` / `EVALSHA` / `FUNCTION`** | `@scripting`. **Lua 가 서버를 블로킹**(`lua-time-limit` 5초 후에도 다른 명령은 `BUSY`), 스크립트 내부 호출도 ACL 은 적용(✅ 5-5)되지만 **허용된 명령이 `@write` 면 `FLUSHALL` 도 가능**(5-8). 과거 Lua 샌드박스 탈출 취약점(CVE) 사례 → **버전 최신 유지** |
| `SCRIPT FLUSH/KILL`, `FUNCTION FLUSH/DELETE/RESTORE` | 스크립트/함수 삭제, 실행 중 스크립트 종료 |
| **`DEL`/`UNLINK` 의 대상 큰 키** | `dangerous` 는 아니지만 **big key `DEL` 은 블로킹**(3장). 대안 `UNLINK` |
| `OBJECT`, `MEMORY USAGE/DOCTOR` | 내부 정보(인코딩, 메모리) 노출 — 낮은 위험 |
| `EXPIRE`/`PERSIST`/`RENAME` | 키 삭제·덮어쓰기 가능(`@write`), 키 패턴으로 제어 |
| `WAIT`, `BLPOP` 등 **blocking** | 클라이언트 점유(연결 고갈) |
| `HELLO` / `AUTH` / `CLIENT SETNAME` | 인증 시도(브루트포스 경로) — 실패 횟수 제한 필요(방화벽/fail2ban 류는 Redis 외부) |
| **`SUBSCRIBE`/`PSUBSCRIBE *`** | 모든 채널 감청 → **채널 `&` 패턴** 으로 제한 |

---

## 8부. 보안 체크리스트

- [ ] **`bind`**: 프라이빗 NIC/loopback 만, 불필요한 `0.0.0.0` 금지. Docker 는 `-p 127.0.0.1:6379:6379`
- [ ] **방화벽/보안그룹**: 6379(+클러스터 16379, 센티널 26379)를 **앱 SG/내부 CIDR 만** 허용. **인터넷 개방 금지**
- [ ] **`protected-mode yes` 유지**. 연결 안 된다고 `no` 로 바꾸지 말고 **비번/ACL** 로 해결
- [ ] **`default` 유저**: 비밀번호 설정 또는 `user default off` + 전용 계정
- [ ] **ACL**: 앱/운영/모니터링/복제 **계정 분리**, **최소 권한(`-@all` 부터 허용 목록)**, **앱 계정에 `-@dangerous`**, 키(`~`)·채널(`&`) 패턴 제한
- [ ] **`ACL DRYRUN` 으로 사전 확인, `ACL LOG` 로 거부 이력 모니터링**
- [ ] **`aclfile` 사용** + 파일 권한 `600`, 설정 변경은 `ACL SAVE` 로 영속화
- [ ] **`enable-protected-configs/debug-command/module-command` 는 기본(`no`) 유지**, 필요 시 `local`
- [ ] **`rename-command`** 는 ACL 로 대체 가능하면 쓰지 않음(복제/AOF/센티널 호환)
- [ ] **비밀번호 보관**: 설정 파일 권한 관리, **`MONITOR`/슬로우로그/로그에 평문 노출** 주의, `redis-cli -a` 대신 `REDISCLI_AUTH`
- [ ] **복제/센티널/클러스터**: 모든 노드에 **동일 계정/비번**(`masteruser/masterauth`, `sentinel auth-*`) 일괄 적용
- [ ] **TLS**(`tls-port`, `tls-cert-file`, `tls-key-file`, `tls-ca-cert-file`, `tls-replication`, `tls-cluster`): **외부/타 VPC 통신, 규정 준수 환경** 에서 사용. 성능 오버헤드 있음
- [ ] **Redis 버전 최신 유지**(Lua 샌드박스, 취약점 패치)
- [ ] **`dir`/데이터 파일 권한**, Redis 를 **root 가 아닌 전용 계정**으로 실행

---

### ⚠️ 확인하지 못한 부분 (정직 기록)

**로컬에서 직접 확인한 것**: `bind` 기본값·loopback-only 거부, protected mode `DENIED`/`NOAUTH`/`no` 개방 동작, `rename-command`(삭제·개명), `enable-protected-configs/debug-command/module-command` 기본 차단과 `local`, `ACL CAT` 카테고리·`dangerous` 75개와 `admin`과의 차집합, ACL 유저별 명령·키·채널·selector R/W 권한, 스크립트 내 키 권한 검사, `+@write` 로 스크립트 `FLUSHALL` 가능, `ACL DRYRUN`/`LOG`/`DELUSER`(연결 종료)/`SETUSER`(즉시 반영)/`GENPASS`, `aclfile` + `ACL SAVE/LOAD`, 복제 전용 계정(성공/실패 모두).

**문서/지식 기준이며 재현하지 않은 것**:
- **Docker/클라우드 환경의 `-p` 공개, iptables 우회, 프록시 뒤 `DENIED` 시나리오**
- 과거 공격 사례(`CONFIG SET dir` 를 이용한 SSH 키/cron 파일 쓰기, `REPLICAOF` 를 이용한 악성 모듈 이식)의 **실제 재현은 하지 않음**(방어 설명 목적)
- **`rename-command` 와 AOF/복제의 호환성 문제**: 설정 가능한 이름이 AOF/복제 스트림에 기록된다는 설명은 문서 기준(복제 구성 후 검증하지 않음)
- **7.0 이전 protected mode 세부 조건**, `redis.conf` 배포본의 `bind` 기본 문구, `CONFIG SET bind` 의 연결 유지 동작
- **TLS 설정 및 성능 영향**, `--askpass`, `sentinel auth-user`, 클러스터 노드 간 인증 세부는 실행해보지 않음
- **Lua 샌드박스 CVE 사례**와 `lua-time-limit` 동작은 일반 지식 기준
- `dangerous` 카테고리 구성 외 **7부 표**(dangerous 아닌 위험 명령)는 일부만 확인(`ACL CAT dangerous` 목록에 없음을 확인), 나머지는 문서 기준
- 클라이언트 라이브러리가 요구하는 최소 명령 목록(`HELLO`, `CLIENT SETINFO` 등)은 라이브러리·버전별로 다름
