### CHAPTER 13 레디스 운영하기

Redis 는 **싱글 스레드 + 인메모리** 구조라 문제가 생기면 **특정 지표가 먼저 신호를 준다.** 운영의 핵심은 ① **어떤 지표를 어떤 기준으로 볼지**(대시보드·알람) ② **어떤 설정/명령이 장애·성능 저하를 부르는지 사전에 아는 것** 이다. 이 장은 이 두 가지를 정리한다.

> 로컬 Redis 7.2.6(macOS, 키 약 100만 개·메모리 약 120MB)에서 **INFO 필드명과 위험 명령/설정의 영향을 직접 측정**했다(`✅ 검증`). **Grafana/Prometheus/`redis_exporter` 는 이 환경에서 실행하지 못했다**(Docker 데몬 미기동). 따라서 **PromQL/메트릭명/대시보드 ID 는 `redis_exporter` 문서 기준**이며 각 메트릭이 대응하는 **원천 `INFO` 필드는 ✅ 로컬 확인**했다. 임계값은 **일반적 권장 출발점**이며 워크로드에 맞게 조정해야 한다.

#### 목차

- [1부. 모니터링 구성](#1부-모니터링-구성)
- [2부. 지표(패널) 우선순위 — 필수 / 권장 / 참고](#2부-지표패널-우선순위--필수--권장--참고)
- [3부. 추천 대시보드 구성과 알람 규칙](#3부-추천-대시보드-구성과-알람-규칙)
- [4부. 장애·성능 저하를 유발하는 설정과 명령어](#4부-장애성능-저하를-유발하는-설정과-명령어)
- [5부. 증상 → 지표 → 원인 → 조치 플레이북](#5부-증상--지표--원인--조치-플레이북)

---

## 1부. 모니터링 구성

```
 [Redis] ──INFO/SLOWLOG/LATENCY/CLUSTER INFO──▶ [redis_exporter :9121] ──/metrics──▶ [Prometheus] ──▶ [Grafana]
                                                                                          └──▶ [Alertmanager → Slack/PagerDuty]
```

| 구성 요소 | 역할 |
|---|---|
| **redis_exporter** (oliver006) | Redis 에 접속해 `INFO`, `CLUSTER INFO`, `SLOWLOG`, `LATENCY`, key 크기 등을 **Prometheus 메트릭으로 변환**(문서 기준) |
| **Prometheus** | 15초 등 주기로 스크레이프, 시계열 저장, 알람 규칙 평가 |
| **Grafana** | 대시보드 시각화(변수 `instance`/`job` 으로 다중 인스턴스 전환) |
| **(보완) 외부 프로브** | `redis-cli --latency`/`PING` 왕복을 **클라이언트 관점**으로 측정 — exporter 는 서버 내부 값만 보여 **네트워크/큐잉 지연을 놓침** |

### 원천 데이터 — `INFO` 섹션 ✅ 확인 (7.2.6)

| 섹션 | 주요 필드(확인된 이름) |
|---|---|
| `server` | `uptime_in_seconds`, `redis_version`, `io_threads_active` |
| `clients` | `connected_clients`, `blocked_clients`, `maxclients`, `client_recent_max_input_buffer`, `client_recent_max_output_buffer`, `tracking_clients` |
| `memory` | `used_memory`, `used_memory_rss`, `used_memory_peak`, `maxmemory`, `mem_fragmentation_ratio`, `allocator_frag_ratio`, `mem_clients_normal`, `mem_clients_slaves`, `mem_replication_backlog`, `mem_not_counted_for_evict`, `mem_aof_buffer`, `used_memory_overhead`, `used_memory_dataset` |
| `persistence` | `rdb_last_bgsave_status`, `rdb_changes_since_last_save`, `rdb_last_save_time`, `rdb_last_bgsave_time_sec`, `aof_last_write_status`, `aof_last_bgrewrite_status`, `aof_rewrites_consecutive_failures`, `current_cow_size`, `loading` |
| `stats` | `total_commands_processed`, `instantaneous_ops_per_sec`, `total_connections_received`, `rejected_connections`, `keyspace_hits/misses`, `expired_keys`, `evicted_keys`, `evicted_clients`, `total_eviction_exceeded_time`, `latest_fork_usec`, `sync_full`, `sync_partial_ok`, `sync_partial_err`, `total_error_replies`, `instantaneous_eventloop_duration_usec`, `acl_access_denied_*`, `total_net_input/output_bytes` |
| `replication` | `role`, `connected_slaves`, `master_repl_offset`, `repl_backlog_size`, (레플리카) `master_link_status`, `master_last_io_seconds_ago`, `slave_repl_offset`, `slaveN:...lag=` |
| `cpu` | `used_cpu_sys`, `used_cpu_user`, `used_cpu_sys_children`, `used_cpu_user_children` |
| `commandstats` | `cmdstat_<cmd>:calls, usec, usec_per_call, rejected_calls, failed_calls` ✅ |
| `latencystats` | `latency_percentiles_usec_<cmd>:p50,p99,p99.9` ✅ (`latency-tracking yes` 기본, 퍼센타일 `50 99 99.9`) |
| `keyspace` | `db0:keys=,expires=,avg_ttl=` ✅ |
| 별도 | `SLOWLOG LEN/GET`, `LATENCY LATEST/HISTORY`, `CLUSTER INFO` |

> **누적 카운터(`*_total`, `total_*`) 는 `rate()` 로 보고, 게이지(`used_memory` 등) 는 그대로** 본다. 재시작하면 카운터가 0 으로 리셋되므로 `rate()` 가 자동 처리.

---

## 2부. 지표(패널) 우선순위 — 필수 / 권장 / 참고

> 메트릭명은 `redis_exporter` 기준(문서 기준). 각 패널 옆 `INFO` 필드는 ✅ 로컬 확인.

### 🔴 필수 패널 (장애를 직접 알려주는 Golden Signals — 알람 연결 대상)

| # | 패널 | PromQL(예시) | 원천 INFO | 임계 / 해석 |
|---|---|---|---|---|
| 1 | **가용성 (Up / Role / Uptime)** | `redis_up`, `redis_instance_info{role}`, `redis_uptime_in_seconds` | — / `role` / `uptime_in_seconds` | `redis_up==0` 즉시 알람. **uptime 급감 = 재시작/failover**. role 변화 = 승격 감지 |
| 2 | **메모리 사용률** | `redis_memory_used_bytes / redis_memory_max_bytes` | `used_memory`, `maxmemory` | **80% 경고 / 90% 위험**. `maxmemory=0`(무제한)이면 **호스트 메모리 대비**로 계산 |
| 3 | **Eviction / OOM 속도** | `rate(redis_evicted_keys_total[5m])` | `evicted_keys` | **0 이 아니면 데이터가 지워지는 중**(캐시는 정상일 수 있음, **세션/영구 데이터면 즉시 장애**). `noeviction` 이면 **쓰기 OOM 에러**(✅ 4부) |
| 4 | **명령 처리량 (OPS)** | `rate(redis_commands_processed_total[1m])` | `total_commands_processed` / `instantaneous_ops_per_sec` | 급증/급감을 베이스라인과 비교(트래픽 이상, 재시도 폭주, 장애) |
| 5 | **지연 (Latency)** | 명령별: `rate(redis_commands_duration_seconds_total[1m]) / rate(redis_commands_total[1m])` ; (가능 시) 퍼센타일 | `commandstats usec_per_call`, `latencystats p99` | **평균은 평소 수십 µs**. **p99 가 수 ms 이상**이면 블로킹 명령/fork/CPU 포화 의심. 평균만 보면 **꼬리 지연을 놓침** → 퍼센타일 |
| 6 | **연결 수 & 거부** | `redis_connected_clients / redis_config_maxclients`, `rate(redis_rejected_connections_total[5m])` | `connected_clients`, `maxclients`, `rejected_connections` | 사용률 **80%↑ 경고**, `rejected>0` 즉시(✅ `max number of clients reached`) |
| 7 | **Blocked clients** | `redis_blocked_clients` | `blocked_clients` | `BLPOP` 등 **의도된 블로킹 외 지속 증가**는 큐 소비 지연/데드락 신호 |
| 8 | **복제 상태·지연** | `redis_connected_slaves`, `redis_master_repl_offset - redis_slave_repl_offset`, `redis_master_link_up` | `connected_slaves`, `master_repl_offset`, `slave_repl_offset`, `master_link_status` | 레플리카 수 감소, **link down**, **offset 격차 지속 증가**(복제 지연 → failover 시 유실) |
| 9 | **영속성 상태** | `redis_rdb_last_bgsave_status`, `redis_aof_last_write_status`, `time() - redis_rdb_last_save_timestamp_seconds` | `rdb_last_bgsave_status`, `aof_last_write_status`, `rdb_last_save_time` | **status != ok 즉시 알람**(✅ MISCONF 으로 쓰기 중단), 마지막 저장이 너무 오래됨 |
| 10 | **CPU** | `rate(redis_cpu_user_seconds_total[1m]) + rate(redis_cpu_sys_seconds_total[1m])` | `used_cpu_user/sys` | **Redis 는 명령 실행이 1 코어** → **코어 1개 100% 가까이**(≈ 0.8~1.0)면 **포화**. 호스트 전체 CPU% 로는 못 잡음 |
| 11 | **Slowlog** | `redis_slowlog_length`, `rate(...)` | `SLOWLOG LEN` | 증가율 상승 = 느린 명령 유입. `slowlog get` 으로 원인 명령 확인(✅ 4부에서 `keys`/`del` 이 기록됨) |

### 🟡 보면 좋은 패널 (원인 분석·용량 계획)

| # | 패널 | 메트릭 / INFO | 용도 |
|---|---|---|---|
| 12 | **캐시 히트율** | `rate(keyspace_hits)/(rate(hits)+rate(misses))` ✅ | **캐시용일 때 필수에 가까움**. 하락 = TTL 만료 집중, eviction, 키 설계 문제 |
| 13 | **메모리 단편화** | `mem_fragmentation_ratio` (`redis_mem_fragmentation_ratio`) ✅ | **1.0~1.5 정상**, **>1.5 단편화**(RSS 낭비), **<1 이면 스왑 사용 의심(위험)**. `allocator_frag_ratio` 병행 |
| 14 | **RSS vs used_memory** | `used_memory_rss`, `used_memory_peak` ✅ | 실제 OS 메모리 대비. fork 시 **COW 로 RSS 급증** 가능 |
| 15 | **Fork 시간 / COW** | `latest_fork_usec` ✅(로컬 약 2.5ms/121MB), `current_cow_size`, `rdb_last_cow_size` | **fork 가 길면 그 동안 메인 스레드 정지**. 대용량·THP 켜짐에서 수백 ms~수 초 |
| 16 | **Key 수 / Expire 수** | `redis_db_keys`, `redis_db_keys_expiring` ✅ `db0:keys=,expires=` | 키 증가 추세, TTL 없는 키 비율(영구 누적 위험) |
| 17 | **Expired / Stale** | `rate(expired_keys)`, `expired_stale_perc` ✅ | 만료 키 처리 부하(5장 active expire), TTL 몰림 감지 |
| 18 | **명령별 호출·소요시간(Top N)** | `redis_commands_total{cmd}`, `redis_commands_duration_seconds_total{cmd}` | **어떤 명령이 CPU 를 쓰는지**(예: `KEYS`, `SORT`, `LRANGE` 급증) — 가장 유용한 원인 분석 패널 |
| 19 | **네트워크 I/O** | `rate(redis_net_input_bytes_total)`, `rate(redis_net_output_bytes_total)` ✅ | 대역폭 포화, **big value 응답** 증가 감지 |
| 20 | **클라이언트 버퍼** | `client_recent_max_output_buffer`, `client_recent_max_input_buffer`, `mem_clients_normal`, `evicted_clients` ✅ | **느린 클라이언트** 가 메모리를 쌓는지(12장) |
| 21 | **복제 Full/Partial sync** | `rate(sync_full)`, `sync_partial_ok`, `sync_partial_err` ✅ | **full sync 반복 = backlog 부족/네트워크 불안정**(8장) |
| 22 | **에러 응답** | `rate(total_error_replies)`, `commandstats failed_calls/rejected_calls` ✅ | 앱 오류, ACL 거부(`acl_access_denied_*`), OOM 거부 |
| 23 | **Replication backlog** | `repl_backlog_size`, `mem_replication_backlog` ✅ | 레플리카가 끊겼다 복귀할 때 partial sync 가능 여부 |
| 24 | **Eventloop 지연** | `instantaneous_eventloop_duration_usec` ✅(7.0+) | 이벤트 루프 1회 평균 소요 — **블로킹 명령/CPU 포화의 서버 내부 신호** |

### 🟢 참고 패널 (상황별)

| 패널 | 언제 |
|---|---|
| **Cluster**: `redis_cluster_state`, `cluster_slots_ok/fail/pfail`, `cluster_known_nodes`, `cluster_current_epoch` | **클러스터 운영 시 필수**(10장). `state != ok` 즉시 알람, `slots_fail>0` |
| **Sentinel**: `redis_sentinel_masters`, `sentinel_master_status`, `sentinel_master_ok_slaves`, `sentinel_master_sentinels`, `sentinel_tilt` | 센티널 운영 시 (`sentinel_master_status != ok`, 센티널 수 < quorum) |
| **Pub/Sub**: `pubsub_channels`, `pubsub_patterns`, `pubsubshard_channels` ✅ | 메시징 용도 |
| **Client-side caching**: `tracking_total_keys`, `tracking_clients` ✅ | Tracking 사용 시 (12장) |
| **Lazyfree/Defrag**: `lazyfree_pending_objects`, `active_defrag_running`, `total_active_defrag_time` ✅ | `UNLINK`/`activedefrag` 사용 시 |
| **ACL**: `acl_access_denied_auth/cmd/key/channel` ✅ | 보안 이벤트(무차별 대입 `auth` 급증) |
| **Key 크기 분포**(`--check-keys`/`check-single-keys`) | **big key 탐지**. 비용이 커서 **샘플링/주기 낮게** |
| **Loading**: `loading`, `async_loading` ✅ | 재시작 후 데이터 로딩 중(`LOADING` 에러) |
| **시스템 메트릭**(node_exporter): 메모리/스왑, `vm.overcommit`, `net.core.somaxconn`, 디스크 IO, 네트워크 | Redis 지표 외 **OS 병목 교차 확인** |

### 지표 간 해석 팁 (대시보드 상관관계)

| 관찰 | 의심 |
|---|---|
| OPS ↓ + 지연 ↑ + CPU(코어 1) ~100% | **느린 명령/ big key / 파이프라인 폭주**. Top N 명령 패널로 확인 |
| 지연 스파이크가 **주기적**(분/시간 단위) | **RDB/AOF rewrite 의 fork**, `save` 주기, 만료 키 몰림 |
| 메모리 ↑ + evicted ↑ + 히트율 ↓ | **용량 부족**: 샤딩/증설/TTL·정책 조정 |
| 메모리 ↑ + 키 수 불변 | **클라이언트 버퍼/복제 버퍼/단편화** (`mem_clients_*`, frag ratio) |
| connected_clients ↑ + blocked ↑ | 소비자 지연, 커넥션 누수/풀 과다 |
| 복제 offset 격차 ↑ + sync_full ↑ | **레플리카 네트워크/느린 레플리카/ backlog 부족** |
| `rdb_last_bgsave_status:err` | 디스크 용량/권한 → **MISCONF 로 쓰기 중단**(✅ 4부) |

---

## 3부. 추천 대시보드 구성과 알람 규칙

### 3-1. 추천 레이아웃 (Row 단위, 위에서 아래로 중요도 순)

| Row | 패널 | 비고 |
|---|---|---|
| **0. 상단 Stat 요약** | Up, Role, Uptime, 메모리 사용률(게이지), 연결 사용률, 히트율, Evicted/s, 복제 레플리카 수 | **한눈에 정상/비정상** 판단(색상 임계) |
| **1. 트래픽·지연** | OPS, 명령별 평균 소요시간 Top N, (퍼센타일 p99), Slowlog 증가율, Eventloop 지연 | 필수 4·5·11, 권장 18·24 |
| **2. 메모리** | used vs maxmemory(+rss, peak), 단편화 비율, Evicted/Expired, 키 수/TTL 키 수, 클라이언트·복제 버퍼 메모리 | 필수 2·3, 권장 13·14·16·17·20 |
| **3. 연결·네트워크** | connected/blocked/rejected, 네트워크 in/out, 클라이언트 버퍼 최대치 | 필수 6·7, 권장 19·20 |
| **4. 복제·가용성** | link status, offset 격차, full/partial sync, backlog, failover 이력(role 변화·uptime) | 필수 8, 권장 21·23 |
| **5. 영속성** | RDB/AOF 상태, 마지막 저장 경과, fork 시간, COW 크기, AOF rewrite 상태 | 필수 9, 권장 15 |
| **6. 클러스터/센티널** (해당 시) | cluster_state, slots_fail, known_nodes / sentinel 상태 | 참고 |
| **7. 보안·에러** | 에러 응답율, ACL 거부, 거부된 연결 | 권장 22 |
| **8. 호스트(OS)** | CPU/메모리/스왑/디스크/네트워크 | 교차 확인 |

**운영 팁**: 변수 `instance` 다중 선택, **배포/장애 이벤트를 Annotation** 으로 표시, 패널 임계값 색(녹/황/적), **Top N 패널은 `topk(5, ...)`**, 장기 추세는 **다운샘플/`rate` 구간 5m 이상**.

### 3-2. 알람 규칙 (Prometheus Alertmanager 예시 — 출발점)

| 심각도 | 조건(예시) | 지속 | 의미 |
|---|---|---|---|
| **P1 즉시** | `redis_up == 0` | 1m | 인스턴스 다운 |
| P1 | `redis_cluster_state == 0` / `redis_connected_slaves < 기대치`(마스터) / `redis_master_link_up == 0`(레플리카) | 1~2m | 클러스터/복제 붕괴 |
| P1 | `rdb_last_bgsave_status != ok` 또는 `aof_last_write_status != ok` | 즉시 | **쓰기 중단(MISCONF) / 영속성 실패** |
| P1 | `memory_used / maxmemory > 0.95` | 5m | OOM/대량 eviction 임박 |
| P2 경고 | `memory_used / maxmemory > 0.8` | 10m | 용량 계획 |
| P2 | `rate(evicted_keys[5m]) > 0` (**세션/영구 데이터 인스턴스**) | 5m | 데이터 유실 중 |
| P2 | `rate(rejected_connections[5m]) > 0` | 1m | 연결 한도 초과 |
| P2 | `connected_clients / maxclients > 0.8` | 5m | 연결 풀 과다 |
| P2 | 코어 1개 사용률 > 0.8 | 5m | 싱글 스레드 포화 |
| P2 | `mem_fragmentation_ratio > 1.5` 또는 `< 1` | 15m | 단편화 / 스왑 |
| P2 | p99 지연 > 임계 (예: 5ms) / slowlog 증가율 급증 | 5m | 지연 악화 |
| P3 정보 | `uptime < 300` (재시작/failover 감지), 히트율 하락, `sync_full` 증가 | — | 이상 징후 |

### 3-3. 공개 대시보드 (참고)

- Grafana 공식 라이브러리의 **"Redis Dashboard for Prometheus Redis Exporter 1.x"** 가 가장 널리 사용되고(대시보드 **ID 763**), `redis_exporter` 저장소에도 예제 대시보드가 포함된다 — **ID/내용은 문서 기억 기준이므로 임포트 전 최신 여부 확인** 필요. 그대로 쓰기보다 **위 3-1 레이아웃에 맞춰 필수 패널을 앞쪽에 재배치**하고 **불필요한 패널을 줄이는 것**을 권장한다.
- AWS ElastiCache 는 CloudWatch 지표명이 다르다(`CPUUtilization`, `EngineCPUUtilization`, `DatabaseMemoryUsagePercentage`, `Evictions`, `CurrConnections`, `ReplicationLag`, `CacheHitRate` 등). **`EngineCPUUtilization`(Redis 스레드 CPU)** 이 싱글 스레드 포화를 보는 핵심 지표(문서 기준).

---

## 4부. 장애·성능 저하를 유발하는 설정과 명령어

> 아래 수치는 **로컬 맥(키 100만·약 120MB)** 측정값으로, **상대적 크기감**을 보기 위한 것이다. 실제 서버(리눅스, 더 큰 데이터)에서는 **더 길어진다.** 블로킹 시간은 **"다른 모든 클라이언트의 `PING` 이 그만큼 막힌 시간"** 이다(`max PING`).

### 4-1. 메인 스레드를 막는 명령어 ✅ 검증

| 명령 | 측정 결과 | 왜 위험한가 | 대안 |
|---|---|---|---|
| **`KEYS *`** | **204ms** (키 100만) / 같은 시간 동안 다른 클라이언트 `PING` **최대 90ms 지연** | O(N) 전체 순회, 서버 정지 | **`SCAN`** (커서 기반 점진 순회) |
| **`DEL bigkey`** (50만 필드 해시) | **147ms**, `PING` 최대 **137ms** | 큰 키 삭제가 **동기 해제** | **`UNLINK`**: **9ms**, `PING` 최대 **1ms** ✅ (백그라운드 해제) |
| **`FLUSHALL`** (키 100만) | **462ms**, `PING` 최대 **447ms** | 전체 동기 삭제 | **`FLUSHALL ASYNC`**: **20ms**, `PING` 최대 **1ms** ✅. `lazyfree-lazy-user-flush yes` 로 기본 비동기화 가능 |
| **`SAVE`** | **278ms**, `PING` 최대 **269ms** | **동기 RDB 저장 — 완료까지 서비스 정지** | **`BGSAVE`**(fork) |
| **`LRANGE biglist 0 -1`** (50만 요소) | **98ms**, `PING` 최대 39ms | 거대 응답 생성 + 전송(네트워크/클라이언트 버퍼도 부담) | 범위 조회(`LRANGE 0 99`), 페이지네이션, 큰 리스트 분할 |
| **`SORT biglist ALPHA`** | **128ms**, `PING` 최대 110ms | O(N+M·logM), 임시 결과 메모리 | 정렬 결과는 Sorted Set 으로 유지, `SORT` 지양 |
| **`LINDEX biglist 250000`** | **12ms** | **O(N)** (quicklist 순회) — 3장에서 정리한 인덱스 접근 비용 | 인덱스 접근이 잦으면 Sorted Set / 다른 구조 |
| **Lua 무한/장시간 스크립트** | 실행 중 다른 명령은 **`BUSY Redis is busy running a script. You can only call SCRIPT KILL or SHUTDOWN NOSAVE.`**, `SCRIPT KILL` → `OK` ✅ | 스크립트 전체가 **단일 원자 실행** | 스크립트는 짧게, 루프 금지. `busy-reply-threshold`(=`lua-time-limit`, 기본 **5000ms** ✅) 후에야 `SCRIPT KILL` 가능 |
| `MONITOR` | 20 클라이언트 `SET` 부하에서 **약 123k → 113k ops/s (약 8% 감소)** ✅ (출력을 `/dev/null` 로 버린 **최선의 경우**) | 모든 명령을 복사해 전송, 출력 소비가 느리면 **output buffer 폭증**(12장) | 운영 중 장시간 사용 금지, 필요 시 짧게. **(공식 문서는 최대 50% 이상 저하 가능이라 경고 — 문서 기준, 내 환경에선 8%)** |
| `SMEMBERS`/`HGETALL`/`SUNION`/`SINTER` (큰 컬렉션) | 컬렉션 크기에 비례 (이번 테스트의 `HGETALL` 은 대상 키가 이미 삭제되어 **측정 불가**) | O(N) + 응답 크기 | `SSCAN`/`HSCAN`/`SRANDMEMBER`, 컬렉션 크기 제한 |
| `DEBUG SLEEP` / `DEBUG RELOAD` | 지정 시간 전체 정지 | 서버 정지 | 운영 환경에서 `enable-debug-command no`(기본, 11장) |
| `CLIENT PAUSE` | 쓰기(또는 전체) 정지 | 의도적 정지, 해제 누락 시 장애 | 시간 지정, failover 작업에만 |
| `MIGRATE`/`RESTORE` (큰 키) | 키 크기에 비례해 블로킹(10장) | big key | 사전 분할 |
| `EXPIRE` 를 **동일 시각에 대량** 지정 | 같은 시점 대량 만료 → active expire 가 CPU 점유, 캐시 avalanche(5장) | | TTL 에 **랜덤 지터** |
| `WAIT` | 레플리카 ACK 까지 **호출 클라이언트 블로킹** | 레플리카 지연 시 앱 지연 | 타임아웃 지정 |

### 4-2. 메모리·eviction 설정

| 설정 | 위험 | 확인 |
|---|---|---|
| **`maxmemory` 미설정(0)** | **호스트 메모리를 끝까지 사용 → OOM Killer 가 Redis 를 종료** / 스왑 발생 시 극심한 지연 | `maxmemory` 를 **호스트 메모리의 약 60~75%**(fork COW·버퍼 여유 확보)로 |
| **`maxmemory-policy noeviction`**(기본) + 용량 도달 | **쓰기 전부 거부** ✅ 검증: `maxmemory 5mb`에서 20만 건 입력 중 **175,005건 실패 `OOM command not allowed when used memory > 'maxmemory'.`**, 읽기·삭제는 정상 | 캐시용이면 `allkeys-lru/lfu`. `allkeys-lru` 로 바꾸자 쓰기 성공 + **`evicted_keys` 증가** ✅ |
| **`volatile-*` 정책인데 TTL 키가 없음** | evict 할 키가 없어 `noeviction` 처럼 OOM | TTL 설정 점검(5장) |
| **`maxmemory` 가 너무 근접** | fork(BGSAVE/AOF rewrite) 시 **COW 로 RSS 가 순간 2배 근접** → OOM | 여유 메모리 확보, `used_memory` 80% 이하 유지 |
| **`activedefrag yes`** | CPU 소모(단편화 해소와 트레이드오프) | `mem_fragmentation_ratio` > 1.5 일 때만 검토 |
| **Transparent Huge Pages(THP) 켜짐** | fork 시 **COW 단위가 2MB** → 메모리·지연 급증 (Redis 시작 시 경고 로그 출력) | `echo never > /sys/kernel/mm/transparent_hugepage/enabled` (2장) |
| **`vm.overcommit_memory` ≠ 1** | fork 실패 → **BGSAVE/AOF rewrite 실패** | `vm.overcommit_memory=1` (2장) |
| **스왑 활성** | 메모리 일부가 디스크로 → **수십~수백 ms 지연**, `mem_fragmentation_ratio < 1` | 스왑 끄기/최소화 |
| `lazyfree-lazy-*` 미설정 | `DEL`/만료/eviction 이 **동기 해제**되어 큰 키에서 블로킹 | `lazyfree-lazy-eviction/expire/server-del/user-del yes` 검토(2장) |

### 4-3. 영속성(RDB/AOF) 설정

| 설정 | 위험 | 확인 |
|---|---|---|
| **`appendfsync always`** | 모든 쓰기마다 fsync → **처리량 급락** ✅ 검증(단일 클라이언트 `SET` 1000건): **everysec 약 11,800 req/s vs always 약 313 req/s (약 1/37)**, p50 **0.07ms → 3.07ms** | 일반적으로 **`everysec`**(최대 1초 유실). 금융급 내구성 필요 시에만 always + 배치/파이프라인 |
| `appendfsync everysec` | 디스크가 느리면 **fsync 가 메인 스레드를 지연**(`no-appendfsync-on-rewrite` 와 연계) | 디스크 IO 모니터링 |
| `no-appendfsync-on-rewrite no` | AOF rewrite 중 fsync 경합 → 지연 | `yes` 시 rewrite 중 유실 가능성↑ (트레이드오프) |
| **`save` 주기가 너무 촘촘** (예: `save 1 1`) | 쓰기마다 BGSAVE **fork 반복** → 지연·메모리 | 용량/쓰기량에 맞는 주기 |
| **`stop-writes-on-bgsave-error yes`**(기본) + RDB 저장 실패 | ✅ 검증: 디렉터리 쓰기 불가 상태에서 `save 1 1` → 저장 실패 후 `rdb_last_bgsave_status:err`, **쓰기 명령이 `MISCONF Redis is configured to save RDB snapshots, but it's currently unable to persist to disk...` 로 전부 거부**(읽기는 정상). `no` 로 바꾸면 쓰기 재개 | **디스크 용량/권한 모니터링 필수**. `no` 는 영속성 실패를 방치하므로 신중 |
| **fork 가 오래 걸리는 대용량 인스턴스** | `latest_fork_usec` 가 길면 **fork 동안 서비스 정지**(✅ 로컬 2.5ms/121MB — 리눅스 대용량은 수십~수백 ms/GB 문서 기준) | 인스턴스를 **수~수십 GB 이하로 샤딩**, 레플리카에서 백업 |
| **AOF 가 계속 커짐**(`auto-aof-rewrite-*` 부적절) | 디스크 고갈, 재시작 로딩 시간 증가 | `auto-aof-rewrite-percentage/min-size` 점검, 멀티파트 AOF(7장) |
| 디스크 가득 | AOF 쓰기 실패(`aof_last_write_status:err`) | 디스크 사용률 알람 |

### 4-4. 복제·클러스터·센티널 설정

| 설정 | 위험 |
|---|---|
| **`repl-backlog-size` 너무 작음**(기본 1mb) | 레플리카 순단 시 **partial sync 불가 → full sync(fork+전송) 반복** (`sync_full` ↑) |
| **레플리카 `client-output-buffer-limit replica` 초과** | 복제 연결 끊김 → full sync 루프(12장) |
| `repl-diskless-sync-delay`/`repl-diskless-load` | 설정에 따라 **로딩 중 서비스 불가/메모리 2배**(8장) |
| **`min-replicas-to-write` 설정 + 레플리카 부족** | **마스터가 쓰기를 거부**(의도된 보호이나 가용성 저하) |
| 레플리카 `replica-serve-stale-data yes`(기본) | 동기화 중에도 오래된 데이터를 응답 |
| **센티널 `down-after-milliseconds` 너무 짧음** | 일시 지연에도 **오탐 failover** (9장) |
| **센티널 2대 / quorum 오설정** | failover 불능·split-brain (9장) |
| **클러스터 `cluster-node-timeout` 너무 짧음** | GC/fork 지연에도 **오탐 failover(flapping)** ✅ 10장(3초 설정 시 8초 정지 → failover) |
| **`cluster-require-full-coverage yes`**(기본) | **슬롯 하나만 비어도 클러스터 전체 쓰기 중단**(✅ 10장) |
| **클러스터 big key + `MIGRATE`/리샤딩** | 블로킹 → PFAIL(10장) |

### 4-5. 클라이언트·네트워크·OS

| 설정 | 위험 |
|---|---|
| **`client-output-buffer-limit normal 0 0 0`**(기본 무제한) | 느린 클라이언트가 메모리를 무한히 쌓음 ✅(12장: hard limit 로 종료 확인). `normal 256mb 128mb 60` 등 설정 |
| **`maxmemory-clients 0`**(기본 비활성) | 클라이언트 총 메모리 무제한. `10%` 등 설정(12장) |
| **`maxclients`** 대비 풀 크기 합계 초과 | `ERR max number of clients reached` ✅(12장) |
| `timeout 0` + 방화벽/LB idle timeout | 죽은 연결 누적, 클라이언트 좀비 연결 |
| `tcp-backlog` 511 > OS `somaxconn` | 연결 폭주 시 SYN drop(Redis 시작 경고) |
| `hz` 를 과도하게 높임 | **idle CPU 증가**(만료 처리 빈도↑). 기본 10, `dynamic-hz yes` |
| `slowlog-log-slower-than`/`latency-monitor-threshold` 미설정 | **원인 분석 불가**(관측성 부재) — 권장: `slowlog-log-slower-than 10000`(10ms), `latency-monitor-threshold 100`, `slowlog-max-len 128+` |
| `rename-command`/ACL 미적용 | 위험 명령 노출(11장) |
| 자원 한도 `ulimit -n` 낮음 | `maxclients` 자동 하향, 연결 거부 |

### 4-6. 설정 변경 전 점검 원칙

1. **`CONFIG GET`/`INFO` 로 현재값 확인 → `CONFIG SET`(런타임) 으로 시험 → `CONFIG REWRITE` 로 영구화** (재시작 시 원복 방지). 단 **protected config(`dir`, `dbfilename`...)** 는 런타임 변경 불가(11장)
2. **운영 중 위험 명령은 `ACL` 로 앱 계정에서 차단**(`-@dangerous`, 11장), **`SCAN`/`UNLINK`/`ASYNC`** 로 대체
3. **big key 사전 탐지**: `redis-cli --bigkeys`/`--memkeys`, `MEMORY USAGE`, exporter `check-keys`(샘플링)
4. 변경은 **레플리카 → 마스터** 순서, **롤링**, 변경 후 **지연/메모리 패널 관찰**
5. **관측성 선행**: slowlog/latency-monitor/commandstats 를 먼저 켜 **전후 비교 가능**하게

---

## 5부. 증상 → 지표 → 원인 → 조치 플레이북

| 증상 | 먼저 볼 패널 | 흔한 원인 | 조치 |
|---|---|---|---|
| **응답이 느려짐(지연 급증)** | 지연/Top N 명령/Slowlog/CPU(코어1)/Eventloop | `KEYS`, big key `DEL`, `SORT`, 거대 `LRANGE`, Lua 장시간, fork | `SLOWLOG GET`, `LATENCY DOCTOR`, 원인 명령 차단, `UNLINK/SCAN` 전환 |
| **주기적 지연 스파이크** | fork 시간/RDB·AOF 상태/만료 | BGSAVE/rewrite fork, 만료 몰림, THP | fork 시간 확인, THP off, 샤딩, `save` 조정, TTL 지터 |
| **쓰기 실패 `OOM`** | 메모리 사용률/Evicted/정책 | `noeviction`, 용량 초과 | 증설/샤딩/정책 변경/키 정리 |
| **쓰기 실패 `MISCONF`** | RDB/AOF 상태/디스크 | 디스크 풀/권한/fork 실패 | 디스크 확보, `vm.overcommit_memory=1`, 원인 해결 후 `BGSAVE` |
| **연결 거부** | 연결 수/rejected | 풀 과다/누수 | 풀 상한, `CLIENT LIST` 로 idle 연결 정리, `maxclients`/`ulimit` |
| **메모리 증가(키 수 불변)** | 클라이언트·복제 버퍼, 단편화 | 느린 클라이언트, replica 버퍼, MONITOR | `CLIENT LIST` omem, limit 설정, `maxmemory-clients` |
| **캐시 히트율 하락** | 히트율/Evicted/Expired | 메모리 부족 eviction, TTL 몰림, 키 설계 변경 | 용량 증설, TTL 분산, 캐시 워밍 |
| **복제 지연/ full sync 반복** | offset 격차/sync_full/backlog | backlog 작음, 레플리카 느림, 네트워크 | `repl-backlog-size` 상향, replica 버퍼, 레플리카 성능 점검 |
| **failover 후 데이터 이상** | role/uptime/offset | 비동기 복제 유실, 오탐 failover | `WAIT`/`min-replicas`, 타임아웃 튜닝 |
| **재시작 후 오래 걸림** | loading/AOF 크기 | 큰 AOF/RDB | rewrite, 샤딩, 레플리카 활용 |

---

### ⚠️ 확인하지 못한 부분 (정직 기록)

**로컬에서 직접 확인한 것**: `INFO` 섹션별 필드명(메트릭 원천), `commandstats`/`latencystats`/`keyspace` 형식, `KEYS`·`DEL` vs `UNLINK`·`FLUSHALL` vs `ASYNC`·`SAVE`·`LRANGE 0 -1`·`SORT`·`LINDEX` 의 블로킹 시간(`PING` 최대 지연으로 측정), Lua `BUSY`/`SCRIPT KILL`, `lua-time-limit`/`busy-reply-threshold` 5000ms, `noeviction` OOM 에러와 `allkeys-lru` 전환, `stop-writes-on-bgsave-error` → `MISCONF`, `appendfsync always` vs `everysec` 처리량, `MONITOR` 부하 영향, `latest_fork_usec`.

**실행/재현하지 못한 것(문서·지식 기준)**:
- **Grafana / Prometheus / `redis_exporter` 실제 구동**: Docker 데몬이 꺼져 있어 못 띄움. **메트릭명(`redis_*`), PromQL, 알람 규칙, 대시보드 ID(763)** 는 문서/기억 기준이므로 **적용 전 `curl :9121/metrics` 로 실제 이름을 확인**할 것
- **임계값**(80%/90%, p99 5ms, 단편화 1.5, 코어 0.8 등)은 일반 권장 출발점이며 워크로드별 조정 필요
- **THP, `vm.overcommit_memory`, 스왑, `somaxconn`**: 맥 환경이라 리눅스 동작 미재현 (2장 내용과 문서 기준)
- **fork 소요 시간**: 로컬 121MB 에서 2.5ms 만 확인. **대용량(수 GB~)·THP 켜짐**의 수십~수백 ms 는 문서 기준
- **`MONITOR` 영향**: 측정은 출력을 `/dev/null` 로 버리는 **최선의 경우**(약 8%). 실제 운영 소비자가 느리면 훨씬 커진다(문서는 50%+ 경고)
- **`HGETALL` 대형 해시**: 대상 키가 이미 삭제된 상태에서 측정해 **유효하지 않아 제외**
- **측정 환경**: macOS 단일 호스트, 키 100만/120MB, 단일 시도 위주 → **절대값이 아니라 상대 비교용**. 리눅스·실서버 데이터량에서는 달라짐
- **AWS ElastiCache CloudWatch 지표**는 문서 기억 기준이며 확인하지 않음
- 센티널/클러스터 exporter 지표명(`redis_sentinel_*`, `redis_cluster_*`)은 문서 기준
- 책에서 다루는 **구체 대시보드/설정값**(예: 책이 제시하는 특정 패널 구성)과 다를 수 있음 — **책 내용 확인 필요**
