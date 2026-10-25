### 11장. 애플리케이션 배포를 위한 고급 설정

> 정리 범위: CPU 단위(1000m)와 limits 동작 방식, CPU lifecycle과 CPU manager / requests / QoS, ResourceQuota, LimitRange / 스케줄러와 etcd(노드 필터링, 스코어링) / Pod·Node affinity / 상태 검사(livenessProbe, readinessProbe)
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), **Docker Desktop 내장 쿠버네티스 v1.32.2**(단일 노드, 12 CPU, 메모리 약 8GiB, cgroup v2)에서 리소스/QoS/쿼터/프로브를 확인했다. 노드가 하나라서 **스케줄러/어피니티/CPU manager static 정책은 별도로 만든 임시 kind 클러스터(v1.35.0, control-plane 1 + worker 2, 3개 노드, 스케줄러 로그 레벨 `--v=10`)** 에서 확인했고 실험 후 삭제했다. 이 kind 클러스터는 별도 kubeconfig 파일을 써서 PC의 kubeconfig는 건드리지 않았다.
>
> 표기: `$` 명령과 출력은 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 Docker Desktop 실험은 모두 `--context docker-desktop`을 명시했다 (`k`로 줄여 적음). 이 PC에서 돌아가는 다른 kind 클러스터는 건드리지 않았다.

```bash
k()  { kubectl --context docker-desktop "$@"; }     # Docker Desktop 클러스터
kc() { kubectl --kubeconfig kind.conf "$@"; }       # 실험용 3노드 kind 클러스터
```

#### 목차

1. CPU 단위와 limits의 동작 방식, CPU manager
2. requests
3. QoS, ResourceQuota, LimitRange
4. 스케줄러와 etcd (필터링, 스코어링)
5. Pod / Node affinity
6. 애플리케이션 상태 검사 (probe)
7. 확인하지 못한 부분

---

#### 1. CPU 단위와 limits의 동작 방식, CPU manager

##### 1-1. CPU 단위

| 표기 | 의미 |
|---|---|
| `1` (= `1000m`) | **노드의 논리 CPU(vCPU/코어) 1개**를 100% 쓸 수 있는 양 |
| `500m` (= `0.5`) | 코어 1개의 절반 (1000분의 500, m은 milli) |
| `100m` (= `0.1`) | 코어 1개의 10% |
| `2` | 코어 2개 |

- `m`은 "1000분의 1코어"이고 **1000m = 1코어**다. 코어 수가 많은 노드와 적은 노드에서 같은 `1`은 같은 양(코어 1개)이다.
- 최소 단위는 `1m`이다. `0.5m`을 쓰면 반올림된다 (`requests.cpu: "0.5m"`로 만든 Pod의 저장된 값이 `1m`).
- 메모리는 `Mi`(2의 거듭제곱, 1024 기반), `M`(1000 기반)이 다르다는 점에 주의. 이 장의 실험은 모두 `Mi`/`Gi`를 썼다.
- CPU는 **압축 가능(compressible) 자원**이다. 모자라면 속도가 느려질 뿐 프로세스가 죽지 않는다. 메모리는 **압축 불가능**이라 한도를 넘으면 프로세스가 죽는다 (1-4절).

##### 1-2. requests, limits가 cgroup 값으로 바뀌는 방식 (실측)

여러 Pod를 만들고 컨테이너 안에서 cgroup v2 파일을 읽었다.

| Pod | requests | limits | QoS | cpu.max | cpu.weight | memory.max | oom_score_adj |
|---|---|---|---|---|---|---|---|
| `guar` | 500m / 128Mi | 500m / 128Mi | Guaranteed | `50000 100000` | 20 | 134217728 | **-997** |
| `limonly` | (없음→limits와 같게) | 500m / 128Mi | Guaranteed | `50000 100000` | 20 | 134217728 | -997 |
| `burst` | 250m / 64Mi | 1 / 256Mi | Burstable | `100000 100000` | 10 | 268435456 | 992 |
| `reqonly` | 250m / 64Mi | (없음) | Burstable | `max 100000` | 10 | max | 992 |
| `frac` | 100m | 0.1 (=100m) | Burstable | `10000 100000` | 4 | max | 999 |
| `beff` | (없음) | (없음) | BestEffort | `max 100000` | 1 | max | 1000 |

| 쿠버네티스 설정 | cgroup v2 파일 | 변환 |
|---|---|---|
| `limits.cpu` | `cpu.max` = `<quota> <period>` | quota = limit(m) × 100 µs, period = 100000 µs(100ms). `500m` → `50000 100000`, `1` → `100000 100000`, limit 없음 → `max` |
| `requests.cpu` | `cpu.weight` (1~10000) | requests(m)×1024/1000 = **CPU shares** → weight로 변환. `500m`→512→20, `250m`→256→10, `100m`→약 102→4, 요청 없음→2→1 |
| `limits.memory` | `memory.max` | 바이트 그대로. 없으면 `max` |
| `requests.memory` | (cgroup에는 반영 안 됨) | `memory.low`는 모든 Pod에서 `0`이었다. 스케줄링과 QoS 판정에만 쓰인다 |
| QoS 클래스 | 부모 cgroup, `oom_score_adj` | 3절 |

도커 쪽 설정으로도 확인했다.

```
$ docker inspect -f '{{.Name}} CgroupParent={{.HostConfig.CgroupParent}} CpuShares={{.HostConfig.CpuShares}} CpuQuota={{.HostConfig.CpuQuota}} CpuPeriod={{.HostConfig.CpuPeriod}} Memory={{.HostConfig.Memory}}' <컨테이너>
guar:  CgroupParent=/kubepods/kubepods/pod<uid>              CpuShares=512 CpuQuota=50000  CpuPeriod=100000 Memory=134217728
burst: CgroupParent=/kubepods/kubepods/burstable/pod<uid>    CpuShares=256 CpuQuota=100000 CpuPeriod=100000 Memory=268435456
beff:  CgroupParent=/kubepods/kubepods/besteffort/pod<uid>   CpuShares=2   CpuQuota=0    CpuPeriod=100000 Memory=0
```

- 즉 쿠버네티스의 `requests.cpu`는 도커의 **`--cpu-shares`(상대 가중치)**, `limits.cpu`는 **`--cpu-quota/--cpu-period`(CFS 대역폭 제한)** 와 같은 메커니즘이다 (2장의 `--cpu-shares`, `--cpus`와 동일). `limits.memory`는 `--memory`다.
- **QoS 클래스에 따라 Pod의 cgroup 부모 디렉터리가 다르다.** Guaranteed는 `kubepods/pod<uid>`, Burstable은 `kubepods/burstable/pod<uid>`, BestEffort는 `kubepods/besteffort/pod<uid>` 아래에 있다 (이 환경의 cgroup 경로).
- `cpu.weight` 변환은 `weight = 1 + (shares-2)×9999/262142` 같은 식으로 계산되고 위 값(512→20, 256→10, 2→1)과 맞는다. 식 자체는 문서 지식이고 위 값들로 맞는 것만 확인했다 (`frac`의 shares 값은 직접 읽지 않았다).

##### 1-3. limits(CPU)의 동작: CFS 대역폭 제한과 쓰로틀링

`limits.cpu`는 **CFS 대역폭 제한**으로 구현된다. 100ms를 한 주기(period)로, 컨테이너(cgroup)가 주기마다 **quota만큼의 CPU 시간만** 쓸 수 있다. 다 쓰면 그 주기가 끝날 때까지 **멈춰(throttled)** 있다. `500m`이면 100ms 중 50ms를 쓰면 나머지 50ms는 멈춘다.

코어 수보다 많은 스레드를 돌렸을 때(컨테이너 안에서 `while :; do :; done`을 4개 실행, 약 10초 평균):

| Pod | limits.cpu | 측정된 평균 CPU 사용량 | `nr_periods` / `nr_throttled` / `throttled_usec` |
|---|---|---|---|
| `guar` | 500m | **505m** | 120 / 111 / 4,885,232 |
| `frac` | 100m | **104m** | 127 / 117 / 22,736,325 |
| `reqonly` | 없음 | **4001m** (4코어) | 0 / 0 / 0 |

```
$ cat /sys/fs/cgroup/cpu.stat        # 컨테이너 안에서
usage_usec ...  nr_periods 120  nr_throttled 111  throttled_usec 4885232
```

- 루프를 4개 돌렸는데도 limit이 있는 Pod는 **한도(500m, 100m)에 딱 맞게** 사용했고, limit이 없는 Pod는 4코어를 다 썼다. **requests만 있는 Pod는 CPU를 제한받지 않는다.**
- `nr_throttled / nr_periods`가 쓰로틀링 비율이다 (111/120). 쓰로틀링이 일어나면 지연 시간이 늘어나므로, 애플리케이션이 느려졌는데 CPU 사용률이 limit 근처에서 평평하면 의심해야 한다.
- 쓰로틀링은 **평균이 아니라 주기(100ms) 단위**로 일어난다. 멀티스레드 앱이 짧은 시간에 몰아 쓰면 평균 사용량이 limit보다 낮아도 쓰로틀링이 발생할 수 있다 (이 점은 문서 지식이고, 이 실험은 지속 부하 상황만 확인했다).

##### 1-4. requests(CPU)의 동작: 경합할 때만 비율로 나눈다

`requests.cpu`는 한도가 아니라 **CPU가 모자랄 때의 가중치**다. CPU 하나(코어 3)에 고정(`taskset -c 3`)한 두 Pod가 동시에 루프를 돌렸다.

| Pod | requests.cpu | cpu.weight | 경합 시 사용량 (코어 3의) | 혼자일 때 |
|---|---|---|---|---|
| `w750` | 750m | 30 | **750m** | - |
| `reqonly` | 250m | 10 | **249m** | **1000m** |

- 경합하면 **요청 비율(750:250 = 3:1)대로** 나눴고, 혼자 있을 때는 requests와 상관없이 코어를 다 썼다. requests를 낮게 잡아도 여유가 있으면 더 쓸 수 있고, 경합이 생기면 requests 비율만큼만 보장된다.
- 따라서 **requests = "보장받는 최소량", limits = "넘을 수 없는 최대량"** 이다.

**메모리는 한도를 넘으면 죽는다**

```
$ (limits.memory: 64Mi 컨테이너에서 150MB를 변수에 담기)
$ k -n t11 get pod oom
oom   OOMKilled
$ k -n t11 get pod oom -o jsonpath='{.status.containerStatuses[0].state.terminated}'
{"exitCode":137,"reason":"OOMKilled", ...}
```

- 종료 코드 **137 = 128 + SIGKILL(9)**, 사유 `OOMKilled`다. CPU는 느려지고(쓰로틀링) 메모리는 죽는다는 차이가 제한을 정하는 감각을 달라지게 한다.
- tmpfs 볼륨(`emptyDir: {medium: Memory}`)에 쓴 데이터도 컨테이너 메모리로 계산된다 (9장에서 확인).

##### 1-5. CPU lifecycle: Pod가 CPU 설정을 받는 과정

```
YAML (requests/limits)
  → API 서버 (LimitRange 기본값, ResourceQuota 검사: 3절) 
  → 스케줄러: requests 합계로 노드 선택 (4절)
  → kubelet: QoS 클래스 결정, Pod/컨테이너 cgroup 생성 (kubepods/<QoS>/pod<uid>)
  → 컨테이너 런타임: cpu.weight(requests), cpu.max(limits), memory.max, oom_score_adj 설정
  → 실행 중: 커널 CFS가 weight로 경합 분배, quota로 쓰로틀링
  → (CPU manager static이면) kubelet이 특정 CPU 코어를 컨테이너에 배타적으로 고정 (1-6절)
  → 종료/삭제: cgroup 제거, (static이면) 고정했던 CPU를 공유 풀로 반납
```

##### 1-6. CPU manager: 정책 none vs static

kubelet의 **CPU manager**가 컨테이너를 어느 CPU 코어에서 돌릴지 정한다.

| 정책 | 동작 |
|---|---|
| `none` (기본) | 아무것도 하지 않는다. 모든 컨테이너가 모든 CPU를 공유하고 CFS가 알아서 분배 (위에서 본 cpu.weight/cpu.max만 적용) |
| `static` | **Guaranteed 중 CPU 요청이 정수**(`2`, `3` 등)인 컨테이너에 **전용 코어를 배타적으로** 할당 (cpuset). 나머지는 공유 풀에서 실행 |

**Docker Desktop (정책 `none`)**

```
$ docker run ... nsenter -t 1 -m -- cat /var/lib/kubelet/cpu_manager_state
{"policyName":"none","defaultCpuSet":"","checksum":1353318690}
$ (Guaranteed Pod, requests=limits=cpu 2)
$ k -n t11 exec guar2 -- sh -c 'grep Cpus_allowed_list /proc/1/status; cat /sys/fs/cgroup/cpu.max'
Cpus_allowed_list:	0-11              ← 12코어 전부 사용 가능
200000 100000                          ← quota만 있음 (2코어 분량)
```

정수 CPU의 Guaranteed Pod인데도 **특정 코어에 고정되지 않았다.**

**kind worker2 (정책 `static`)**

kubelet 설정(`cpuManagerPolicy: static`, `reservedSystemCPUs: "0"`)을 추가하고, 정책을 바꾸면 이전 상태 파일과 충돌하므로 `cpu_manager_state`를 치운 뒤 kubelet을 재시작했다 (kind의 노드별 `KubeletConfiguration` 패치는 워커에 적용되지 않아서 노드 컨테이너 안의 `/var/lib/kubelet/config.yaml`을 직접 수정했다).

```
$ cat /var/lib/kubelet/cpu_manager_state
{"policyName":"static","defaultCpuSet":"0-11","checksum":...}
$ k get node ...     →  capacity cpu=12  allocatable cpu=11                  ← 예약한 CPU 0번이 allocatable에서 빠짐

$ (Pod 4개 실행)
이름    QoS         requests/limits          Cpus_allowed_list   cpu.max
gint    Guaranteed  cpu 2 / 2                1-2                 max 100000      ← 코어 1, 2 전용. quota 없음
gint2   Guaranteed  cpu 3 / 3                3-5                 max 100000      ← 코어 3~5 전용
gfrac   Guaranteed  cpu 500m / 500m          0,6-11              50000 100000    ← 정수가 아니라 공유 풀. quota 유지
burst   Burstable   cpu 2 / 4                0,6-11              400000 100000   ← 공유 풀

$ cat /var/lib/kubelet/cpu_manager_state
policy= static | shared pool= 0,6-11
 pod 7f2be353 {'c': '1-2'}
 pod 93769b06 {'c': '3-5'}
$ (gint 삭제 후)
shared pool= 0-2,6-11 entries= 1                                              ← 반납된 코어 1,2가 공유 풀로 돌아옴
```

- **전용 코어 할당은 "Guaranteed + 정수 CPU"일 때만** 된다 (`gint`, `gint2`). Guaranteed라도 `500m`처럼 정수가 아니면(`gfrac`) 공유 풀이다. Burstable(`burst`)도 공유 풀이다.
- 전용 코어를 받은 컨테이너는 **`cpu.max`가 `max`(quota 없음)** 다. 이미 코어를 독점하므로 쓰로틀링이 없다. 공유 풀의 Pod들은 전용 코어로 간 CPU가 빠진 `0,6-11`에서만 돈다.
- `reservedSystemCPUs`로 지정한 코어(0번)는 시스템용으로 빠지고, **allocatable이 12 → 11**로 줄었다. static 정책은 시스템 예약 CPU가 필수다.
- 용도: 지연 시간에 민감한 앱(CPU 캐시 지역성, 쓰로틀링 제거), 코어를 고정해야 하는 워크로드. 단점: 고정된 코어는 다른 Pod가 못 쓰고, 정수 CPU Guaranteed로만 요청해야 한다 (문서 지식).
- NUMA 정렬을 담당하는 topology manager, 메모리의 memory manager는 다루지 않았다.

#### 2. requests

##### 2-1. 의미와 쓰임

| 쓰이는 곳 | requests가 하는 일 |
|---|---|
| **스케줄링** | 노드의 allocatable에서 이미 배정된 Pod들의 **requests 합**을 뺀 여유를 보고 노드를 고른다 (실제 사용량이 아니다) |
| CPU 경합 | `cpu.weight`로 비율 분배 (1-4절) |
| QoS 판정 | requests/limits의 조합으로 클래스 결정 (3절) |
| 메모리 | cgroup에는 반영 안 됨. 스케줄링과 OOM 우선순위(`oom_score_adj`)에만 영향 |
| 쿼터/LimitRange | `requests.cpu`, `requests.memory` 합계 제한 (3절) |

##### 2-2. 스케줄러는 사용량이 아니라 requests로 판단한다 (실측)

```
$ k describe node docker-desktop
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  Resource           Requests     Limits
  cpu                3200m (26%)  2100m (17%)
  memory             624Mi (8%)   852Mi (11%)
```

- 노드의 `Allocated resources`는 **Pod들의 requests/limits 합계**다. 실제 CPU 사용량이 아니다. 이 노드는 12코어(12000m) 중 3200m이 요청되었고(26%), limits 합계는 100%를 넘을 수 있다("overcommitted").
- 노드보다 큰 요청은 Pending이 된다.

```
$ (requests.cpu: 13, memory: 1Gi 인 Pod)
$ k -n t11 describe pod toobig
Warning  FailedScheduling  default-scheduler  0/1 nodes are available: 1 Insufficient cpu. preemption: 0/1 nodes are available: 1 No preemption victims found for incoming pod.
```

- **limits는 스케줄링에 쓰이지 않는다.** 노드(12코어)보다 큰 `limits.cpu: 64`도 requests가 작으면 스케줄된다.

##### 2-3. requests 설정 규칙 (실측)

| 상황 | 결과 |
|---|---|
| `limits`만 지정 | **requests가 limits와 같게 자동 설정**된다 (`limonly`: requests 500m/128Mi, QoS Guaranteed) |
| `requests`만 지정 | limits 없음 (`reqonly`: Burstable, CPU 무제한) |
| 둘 다 없음 | BestEffort |
| requests > limits | API가 거부: `spec.containers[0].resources.requests: Invalid value: "2": must be less than or equal to cpu limit of 1` |
| `0.5m` 같은 1m 미만 | `1m`으로 올림 |

주의: 한쪽 자원만 정해도 **다른 자원의 requests가 limits를 따라간다.** `requests.cpu: 100m`만 쓰고 `limits: {cpu: 64, memory: 64Gi}`를 준 Pod를 만들었더니 **memory requests가 limits와 같은 64Gi로 설정되어 `Insufficient memory`로 Pending**이 되었다 (`describe`: `0/1 nodes are available: 1 Insufficient memory`). limits만 크게 적었다가 스케줄이 안 되는 실수가 생길 수 있다.

- 이 환경에는 metrics-server가 없어서 `k top`이 `error: Metrics API not available`이었다. 실제 사용량 확인은 이 장에서 하지 않았다 (cgroup 파일로 직접 측정했다).

##### 2-4. requests/limits 설정 지침

| 권고 | 이유 |
|---|---|
| requests는 평소 사용량 기준 | 너무 크면 노드가 낭비, 너무 작으면 경합 시 밀림/한 노드에 과밀 배치 |
| 메모리는 requests ≈ limits | 메모리는 압축 불가라 한도 초과 = OOMKilled. 노드 메모리 압박 시에도 Guaranteed가 안전 (3절) |
| CPU limits는 신중하게 | 쓰로틀링으로 지연이 늘어난다 (1-3절). requests만 두고 limit을 생략하는 선택도 있다 |
| 한쪽만 적지 않기 | limits만 적으면 requests가 따라가서 의도와 다를 수 있다 (2-3절) |

#### 3. QoS, ResourceQuota, LimitRange

##### 3-1. QoS 클래스

kubelet이 requests/limits 조합으로 Pod의 **서비스 품질 클래스**를 정하고(`status.qosClass`), 노드의 자원이 부족할 때 **누구를 먼저 희생시킬지**의 기준으로 쓴다.

| 클래스 | 조건 | 이 환경의 예 |
|---|---|---|
| **Guaranteed** | **모든 컨테이너**가 cpu, memory 모두 requests = limits | `guar`, `limonly` |
| **Burstable** | Guaranteed도 BestEffort도 아닌 경우 (requests/limits가 하나라도 있음) | `burst`, `reqonly`, `frac` |
| **BestEffort** | 어떤 컨테이너에도 requests/limits가 없음 | `beff` |

실측: `frac`(requests 100m, limits 0.1 - memory는 없음)은 CPU가 같아도 **memory 설정이 없어서 Burstable**이다. Guaranteed는 cpu와 memory 둘 다 requests = limits여야 한다.

**QoS가 만드는 차이 (cgroup, oom_score_adj)**

| 클래스 | cgroup 부모 | `oom_score_adj` (실측) | 의미 |
|---|---|---|---|
| Guaranteed | `kubepods/pod<uid>` | **-997** | OOM killer가 거의 마지막에 죽인다 |
| Burstable | `kubepods/burstable/pod<uid>` | **992, 999** | requests가 작을수록 값이 크다 (먼저 죽는다) |
| BestEffort | `kubepods/besteffort/pod<uid>` | **1000** | 가장 먼저 죽는다 |

- Burstable의 값은 `1000 − 1000×(메모리 requests ÷ 노드 메모리)` 근사식으로 계산된다. `burst`(메모리 requests 64Mi, 노드 약 7.9GiB)는 992, 메모리 requests가 없는 `frac`은 999로 이 식과 맞는다 (식 자체는 문서 지식이고 두 값으로 맞는 것을 확인).
- 노드의 메모리가 부족하면 kubelet은 **BestEffort → (requests를 초과해 쓰는) Burstable → Guaranteed** 순으로 Pod를 퇴출(evict)한다 (문서 지식. 실제로 노드를 압박하는 실험은 하지 않았다). 9장의 `sizeLimit` 퇴출처럼 Pod 전체가 죽는다.
- 중요한 서비스는 requests = limits로 Guaranteed로 만든다. 로그 수집, 배치처럼 중요도가 낮은 것은 Burstable/BestEffort로 둔다.

##### 3-2. ResourceQuota

**네임스페이스 하나가 쓸 수 있는 총량을 제한**한다 (7장에서 `pods`, `configmaps`로 처음 봤다). 컴퓨트 자원의 합계도 제한할 수 있다.

```yaml
apiVersion: v1
kind: ResourceQuota
metadata: {name: compute, namespace: t11q}
spec:
  hard: {requests.cpu: "1", requests.memory: 1Gi, limits.cpu: "2", limits.memory: 2Gi, pods: "3"}
```

| 요청 | 결과 (실측) |
|---|---|
| requests/limits 없는 Pod | **거부**: `failed quota: compute: must specify limits.cpu for: c; limits.memory for: c; requests.cpu for: c; requests.memory for: c` |
| requests만 있는 Pod | **거부**: `must specify limits.cpu for: c; limits.memory for: c` (쿼터가 limits까지 제한하면 limits도 필요) |
| requests 600m / limits 1 Pod | 생성됨. `describe quota`: limits.cpu 1/2, requests.cpu 600m/1, pods 1/3 |
| 같은 Pod 한 개 더 | **거부**: `exceeded quota: compute, requested: requests.cpu=600m, used: requests.cpu=600m, limited: requests.cpu=1` |
| limits.cpu 2짜리 Pod | **거부**: `requested: limits.cpu=2, used: limits.cpu=1, limited: limits.cpu=2` (requests는 여유가 있어도 limits가 초과) |

- **쿼터가 컴퓨트 자원을 제한하면 모든 Pod가 해당 자원의 requests/limits를 가져야 한다.** 없으면 생성이 거부된다. 3-3절의 LimitRange가 기본값을 채워 주면 이 문제가 해결된다.
- 거부 메시지가 **요청량(requested) / 사용량(used) / 한도(limited)** 를 모두 알려 준다.
- 쿼터는 **새 요청**만 거부한다. Deployment가 만드는 Pod가 거부되면 ReplicaSet의 이벤트에 `exceeded quota`가 남는다 (7장).

**scope와 개수 제한**

```yaml
# scopes: BestEffort 쿼터 (BestEffort Pod만 센다)
spec: {hard: {pods: "1"}, scopes: [BestEffort]}
# 오브젝트 개수/스토리지 쿼터
spec: {hard: {count/deployments.apps: "1", services.nodeports: "0", count/secrets: "3", persistentvolumeclaims: "1", requests.storage: 300Mi}}
```

| 요청 | 결과 (실측) |
|---|---|
| BestEffort Pod 2개 (쿼터 `pods: 1`, scope BestEffort) | 첫 번째만 생성, 두 번째 거부 `exceeded quota: be, requested: pods=1, used: pods=1, limited: pods=1` |
| Burstable Pod 2개 | **둘 다 생성** (scope 밖이라 이 쿼터에 세어지지 않음) |
| Deployment 2개 (`count/deployments.apps: 1`) | 두 번째 거부 |
| NodePort Service (`services.nodeports: 0`) | 거부: `requested: services.nodeports=1, used: services.nodeports=0, limited: services.nodeports=0` |
| ClusterIP Service | 생성됨 |
| PVC 500Mi (`requests.storage: 300Mi`) | 거부 `requested: requests.storage=500Mi, used: requests.storage=0, limited: requests.storage=300Mi` |

- 컴퓨트 외에도 **오브젝트 개수**(`count/<resource>.<group>`, `services.nodeports`), **스토리지 용량**(`requests.storage`, 클래스별 가능)을 제한할 수 있다. 팀별 네임스페이스에 상한을 거는 용도다.
- `scopes`로 QoS(BestEffort/NotBestEffort), 종료 시간(Terminating) 등 일부 Pod만 대상으로 할 수 있다 (여기서는 BestEffort만 확인).

##### 3-3. LimitRange

**네임스페이스 안에서 개별 컨테이너/Pod/PVC의 크기에 대한 기본값, 최소/최대, 비율을 정한다.** ResourceQuota가 "총량"이면 LimitRange는 "하나의 크기"다.

```yaml
apiVersion: v1
kind: LimitRange
metadata: {name: lr, namespace: t11q}
spec:
  limits:
  - type: Container
    default:        {cpu: 200m, memory: 128Mi}     # limits 기본값
    defaultRequest: {cpu: 100m, memory: 64Mi}      # requests 기본값
    min: {cpu: 50m,  memory: 32Mi}
    max: {cpu: 500m, memory: 256Mi}
    maxLimitRequestRatio: {cpu: "4"}               # limits/requests 상한
  - type: PersistentVolumeClaim
    min: {storage: 100Mi}
    max: {storage: 1Gi}
```

**기본값 주입** (ResourceQuota가 있는 네임스페이스에서 requests/limits 없는 Pod가 이제 생성된다)

```
$ k -n t11q apply -f def.yaml                    # resources: {}
pod/def created
$ k -n t11q get pod def -o jsonpath='{.spec.containers[0].resources} {.status.qosClass}'
{"limits":{"cpu":"200m","memory":"128Mi"},"requests":{"cpu":"100m","memory":"64Mi"}} Burstable
$ (annotations)
kubernetes.io/limit-ranger: LimitRanger plugin set: cpu, memory request for container c; cpu, memory limit for container c
```

| 사용자가 지정 | 결과 (실측) |
|---|---|
| 아무것도 없음 | requests = `defaultRequest`, limits = `default` → Burstable |
| **limits만** (300m/128Mi) | **requests가 그 limits와 같게** (300m/128Mi) → **Guaranteed** (`defaultRequest`가 아니라 limits를 따라감) |
| **requests만** (60m/40Mi) | limits는 `default`(200m/128Mi) → Burstable |

- 어떤 값이 채워졌는지는 Pod의 어노테이션 `kubernetes.io/limit-ranger`에 남는다.
- LimitRange 덕분에 그 네임스페이스에는 **BestEffort Pod가 없다.**

**검증**

```
# max 초과 (limits.cpu 1, requests 100m)
Forbidden: pods "big" is forbidden: [maximum cpu usage per Container is 500m, but limit is 1, cpu max limit to request ratio per Container is 4, but provided ratio is 10.000000]
# min 미달 (requests.cpu 10m)
Forbidden: [minimum cpu usage per Container is 50m, but request is 10m, cpu max limit to request ratio per Container is 4, but provided ratio is 10.000000]
# 비율 초과 (requests 60m / limits 300m = 5배)
Forbidden: cpu max limit to request ratio per Container is 4, but provided ratio is 5.000000
# PVC 크기 범위 밖
Forbidden: persistentvolumeclaims "bigpvc" is forbidden: maximum storage usage per PersistentVolumeClaim is 1Gi, but request is 5Gi
```

- 위반한 조건이 **모두 한 번에** 메시지에 나열된다 (max와 ratio가 함께 위반).
- `maxLimitRequestRatio`는 "limits/requests" 상한이다. 비율이 크다는 것은 자원을 과다 약속(overcommit)한다는 뜻이라 이를 제한한다.
- **LimitRange는 생성 시점에만 적용**된다. 기본 limit 값을 `300m`으로 바꾼 뒤에도 이미 있는 Pod(`def`)의 limits.cpu는 `200m` 그대로였다.

**ResourceQuota vs LimitRange**

| 구분 | ResourceQuota | LimitRange |
|---|---|---|
| 제한 대상 | 네임스페이스 **총량/개수** | **개별** 컨테이너/Pod/PVC 크기 |
| 기본값 | 없음 (오히려 지정을 강제) | **requests/limits 기본값을 채워 줌** |
| 거부 시점 | 합계가 넘을 때 | 크기가 min/max/ratio를 벗어날 때 |
| 함께 쓰면 | 쿼터가 지정을 강제 → LimitRange가 기본값으로 채워서 편하게 | |

#### 4. 스케줄러와 etcd (필터링, 스코어링)

##### 4-1. 스케줄러는 어떤 일을 하나

**`spec.nodeName`이 비어 있는 Pod를 감시(watch)하다가 노드를 골라 Pod에 배정(바인딩)한다.** 6장에서 이벤트로 본 `default-scheduler`가 이것이다.

```
스케줄링 큐 → [Filter: 불가능한 노드 제거] → [Score: 남은 노드 점수 매김] → 최고점 노드 선택 → [Bind: Pod.spec.nodeName 기록]
              (필터링 / predicate)           (스코어링 / priority)                                (API 서버에 binding 요청)
```

- **필터링**: Pod를 실행할 수 없는 노드를 제거한다 (자원 부족, nodeSelector/affinity 불일치, taint, 포트 충돌 등). 남는 노드가 없으면 Pod는 `Pending`이고 `FailedScheduling` 이벤트가 이유를 적는다.
- **스코어링**: 남은 노드들에 플러그인별로 0~100점을 매기고 **플러그인 가중치를 곱해 합산**해서 최고점 노드를 고른다. 동점이면 그중 임의로 고른다.
- **바인딩**: 선택한 노드를 API 서버에 알리면(binding) Pod의 `spec.nodeName`이 채워지고 해당 노드의 kubelet이 이를 보고 실행한다.

##### 4-2. 스케줄러 로그로 본 한 번의 스케줄링 (kind, `--v=10`)

`kc -n s1 run first --image=nginx:alpine` 한 개가 스케줄되는 과정을 스케줄러 로그에서 뽑았다.

```
eventhandlers.go   "Add event for unscheduled pod" pod="s1/first"                        ← Pod 생성을 watch로 감지
active_queue.go    "Pod moved to an internal scheduling queue" ... queue="Active"         ← 스케줄링 큐에 들어감
schedule_one.go    "Attempting to schedule pod" pod="s1/first"                            ← 스케줄링 시작
resource_allocation.go "Listed internal info for allocatable resources, requested resources and score"
                   node="t11sched-worker" resourceAllocationScorer="LeastAllocated" allocatableResource=[12000,8217751552] requestedResource=[300,471859200] resourceScore=95
                   node="t11sched-control-plane" ... requestedResource=[1250,1562378240] resourceScore=84
schedule_one.go    "Plugin scored node for pod" plugin="TaintToleration"  node=... score=300
                   "Plugin scored node for pod" plugin="NodeResourcesFit" node="t11sched-worker2" score=95
                   (노드 3개 x 플러그인 4개 점수)
schedule_one.go    "Calculated node's final score for pod" node="t11sched-worker2" score=395
                                                         node="t11sched-worker"  score=395
                                                         node="t11sched-control-plane" score=384
pod_binding.go     "Attempting to bind pod to node" node="t11sched-worker2"               ← 동점 395 중 worker2 선택
schedule_one.go    "Successfully bound pod to node" node="t11sched-worker2" evaluatedNodes=3 feasibleNodes=3
```

- `evaluatedNodes=3 feasibleNodes=3`: 평가한 노드 3개 중 필터링을 통과한 노드가 3개다.
- worker와 worker2가 **395점 동점**이었고 그중 worker2가 선택되었다 (동점 시 임의 선택).
- 스케줄러는 etcd와 직접 통신하지 않는다 (4-5절).

##### 4-3. 필터링: 노드를 걸러내는 조건 (실측, kind 3노드)

| 상황 | 결과 / `FailedScheduling` 메시지 |
|---|---|
| `nodeSelector: {disk: hdd}` | `disk=hdd` 노드(worker2)에만 배치 |
| `nodeSelector: {disk: nvme}` (없음) | Pending. `0/3 nodes are available: 3 node(s) didn't match Pod's node affinity/selector.` |
| `requests.cpu: 13` (노드는 12코어) | Pending. `0/3 nodes are available: 3 Insufficient cpu.` |
| taint 걸린 노드 + toleration 없음 | Pending. `0/3 nodes are available: 1 node(s) had untolerated taint(s), 2 node(s) didn't match Pod's node affinity/selector.` |
| 같은 taint + toleration 있음 | 그 노드에 배치됨 |
| pod anti-affinity 위반 | `3 node(s) didn't match pod anti-affinity rules.` |
| 존재하지 않는 Pod에 대한 pod affinity | `3 node(s) didn't match pod affinity rules.` |

```
$ kc taint node t11sched-worker2 dedicated=gpu:NoSchedule
# 스케줄러 로그 (필터링 중 제외 이유)
taint_toleration.go "node had untolerated taints" logger="FilterWithNominatedPods.Filter.TaintToleration" node="t11sched-worker2" pod="s2/nt1" untoleratedTaint={"key":"dedicated","value":"gpu","effect":"NoSchedule"}
schedule_one.go "Status after running PostFilter plugins" ... preemption: 0/3 nodes are available: 3 Preemption is not helpful for scheduling.
```

- 메시지는 **노드 수와 거부 이유별 개수**(`1 ... , 2 ...`)로 나온다. 이 문장을 읽는 것이 Pending 디버깅의 시작이다.
- 필터링이 모두 실패하면 **PostFilter**(선점, preemption)가 실행되어 낮은 우선순위 Pod를 쫓아내고 자리를 만들 수 있는지 시도한다. 위에서는 `Preemption is not helpful`, `No preemption victims found`로 선점 대상이 없었다 (선점 자체를 일으키는 실험은 하지 않았다).
- 위 표의 필터에 해당하는 플러그인(NodeResourcesFit, NodeAffinity, TaintToleration, InterPodAffinity 등)의 이름은 점수 로그와 메시지에서 확인했고, 필터링 플러그인 전체 목록과 순서는 확인하지 않았다.

**taint / toleration**

| 구분 | 설명 |
|---|---|
| taint (노드) | `key=value:effect`. 노드가 "이 조건을 참는 Pod만 받겠다" |
| toleration (Pod) | 해당 taint를 허용 |
| effect | `NoSchedule`(스케줄 금지, 이 실험), `PreferNoSchedule`(가급적 피함), `NoExecute`(이미 실행 중인 Pod도 퇴출) |

- 컨트롤 플레인 노드에는 기본으로 `node-role.kubernetes.io/control-plane:NoSchedule`이 있다. 실험을 위해 kind의 control-plane 노드의 taint를 제거해서 3개 노드를 모두 쓸 수 있게 했다.
- `NoExecute`와 6장의 기본 톨러레이션(`node.kubernetes.io/not-ready` 등)의 상호작용은 이번에 실험하지 않았다.

##### 4-4. 스코어링: 점수 계산 (실측)

플러그인별 점수와 합계를 스케줄러 로그에서 읽었다.

**① 자원 사용률: `NodeResourcesFit` (LeastAllocated, 기본)**

`(allocatable − requested) / allocatable × 100`을 CPU와 메모리에서 구해 평균낸다. **여유가 많은 노드가 높은 점수**다 (부하를 퍼뜨리는 방향).

```
worker:        allocatable=[12000, 8217751552]  requested=[300, 471859200]    → 95
control-plane: allocatable=[12000, 8217751552]  requested=[1250, 1562378240]  → 84     (시스템 Pod가 이미 requests를 차지)
```

- CPU `(12000-300)/12000 = 97.5`, 메모리 `(8217751552-471859200)/8217751552 = 94.3`, 평균이 95다. (기본 요청 없는 Pod도 스케줄러가 CPU 100m, 메모리 200Mi 정도로 가정하는 계산이 `requested`에 들어 있는 것으로 보이지만 이 가정 값은 확인하지 않았다.)

**② 부하가 한 노드에 쏠린 경우**

worker에 `nodeName`으로 (requests cpu 4, memory 2Gi)짜리 Pod 2개를 직접 올려 두고 제약 없는 새 Pod를 만들었다.

| 플러그인 | control-plane | worker (부하 큼) | worker2 |
|---|---|---|---|
| NodeResourcesFit | 84 | **35** | 93 |
| NodeResourcesBalancedAllocation | 97 | 92 | 99 |
| TaintToleration | 300 | 300 | 300 |
| **합계** | 481 | 427 | **492** |

→ 새 Pod는 **worker2**로 갔다. 이미 바쁜 worker는 `NodeResourcesFit`이 35점으로 떨어져 선택되지 않았다. 스케줄러가 requests 기준으로 부하를 분산한다는 것을 점수로 확인했다.

**③ 선호 노드 어피니티**

`preferredDuringScheduling...`에 `disk=ssd` weight 80, `zone-b` weight 20을 줬다.

| 플러그인 | control-plane (ssd, zone-a) | worker (ssd, zone-a) | worker2 (hdd, zone-b) |
|---|---|---|---|
| NodeAffinity | 200 | 200 | 50 |
| NodeResourcesFit | 84 | 93 | 93 |
| 합계 | 681 | **692** | 542 |

→ ssd 노드에 높은 점수가 붙고 worker가 선택되었다. `NodeAffinity` 점수는 weight 합(80, 20)을 0~100으로 정규화(80/80 → 100, 20/80 → 25)한 뒤 플러그인 가중치(2로 보임)를 곱한 값으로 보이고 200, 50과 일치한다 (가중치 2는 문서 지식이고 로그의 숫자로만 맞춰 봤다).

**④ 점수 합산의 구조**

- `TaintToleration`이 항상 300, `NodeAffinity`가 200처럼 **플러그인마다 곱해지는 가중치가 다르다** (기본 가중치 TaintToleration 3, NodeAffinity 2, NodeResourcesFit 1 등으로 보임. 가중치 표는 문서 지식).
- 동점이면 임의로 고른다 (4-2절).
- 점수가 계산되는 대상은 필터링을 통과한 노드뿐이다.

**⑤ ImageLocality**

컨테이너 이미지가 이미 있는 노드에 점수를 더 주는 플러그인이다. `nginx:alpine`(26.3MB)이 worker2에만 있는 상태에서 새 Pod를 만들었으나 `ImageLocality`는 세 노드 모두 **0점**이었다. 이 이미지는 크기가 작은 편이라(노드 3개 중 1개에만 있어 크기를 1/3로 보정하면 약 8.8MB) 점수가 나오는 최소 기준(문서상 약 23MB) 아래인 것으로 추정한다. 더 큰 이미지로 시도했으나 노드의 이미지 목록에 반영되지 않아 점수가 나오는 경우는 확인하지 못했다.

##### 4-5. etcd와의 관계

**스케줄러, 컨트롤러, kubelet 어느 것도 etcd에 직접 접속하지 않는다. etcd와 직접 통신하는 것은 API 서버뿐이다** (6장). 이를 설정으로 확인했다.

```
# API 서버 실행 옵션: etcd 접속 정보가 있다
--etcd-servers=https://127.0.0.1:2379  --etcd-cafile=...  --etcd-certfile=.../apiserver-etcd-client.crt  --etcd-keyfile=...
# 스케줄러 실행 옵션: etcd 관련 옵션이 없고 kubeconfig로 API 서버에 접속한다
kube-scheduler --kubeconfig=/etc/kubernetes/scheduler.conf --leader-elect=true --v=10 ...
```

**바인딩이 etcd에 기록되는 과정을 직접 확인했다.** 스케줄되지 않는 Pod(`nodeSelector: {wait: here}`)를 만들고 etcd의 키를 조회한 뒤, 노드에 라벨을 달아 스케줄되게 하고 다시 조회했다.

```
$ etcdctl get /registry/pods/s4/watchme -w fields           # 스케줄 전 (Pending)
"CreateRevision" : 2847   "ModRevision" : 2848   "Version" : 2
$ kc -n s4 get pod watchme -o jsonpath='{.metadata.resourceVersion}'     →  2848
$ kc label node t11sched-control-plane wait=here             # 스케줄러가 노드를 찾게 됨
$ etcdctl get /registry/pods/s4/watchme --print-value-only | strings | grep t11sched
t11sched-control-planeX                                      # 값에 노드 이름이 들어감
$ etcdctl get /registry/pods/s4/watchme -w fields
"CreateRevision" : 2847   "ModRevision" : 2867   "Version" : 5
$ kc -n s4 get pod watchme -o jsonpath='resourceVersion={.metadata.resourceVersion} node={.spec.nodeName}'
resourceVersion=2867 node=t11sched-control-plane
```

- **Pod의 `resourceVersion`이 etcd 키의 `ModRevision`과 같다** (2848, 2867). 8장 Watch API에서 본 `resourceVersion`의 정체가 etcd의 변경 순번(revision)이다.
- 스케줄러가 Binding을 요청하면 API 서버가 etcd의 Pod 값에 `nodeName`을 써 넣고 `ModRevision`이 올라간다. 이 변경이 Watch 이벤트로 해당 노드의 kubelet에게 전달된다 (8장). `Version`이 2 → 5로 올라간 것은 그 사이 Pod가 여러 번(스케줄 결과, 상태 갱신 등) 갱신되었다는 뜻이다. 각 갱신이 무엇이었는지는 확인하지 않았다.
- 스케줄러는 **리더 선출**을 한다 (`--leader-elect=true`). `kube-system`의 Lease `kube-scheduler`의 `holderIdentity`가 현재 리더다 (이 클러스터는 스케줄러가 하나라 리더 전환은 실험하지 않았다).

**etcd 자체**

```
$ etcdctl endpoint status -w table          (kind control-plane)
VERSION 3.6.6   DB SIZE 3.8 MB   IS LEADER true   RAFT TERM 2   QUOTA 2.1 GB
$ etcdctl member list
771740a019c0ecf4 | started | t11sched-control-plane | peer https://192.168.16.5:2380
```

- etcd는 **Raft 합의**를 쓰는 분산 키-값 저장소다 (3장의 스웜 매니저와 같은 원리, 홀수 개 멤버가 권장되는 이유도 같다). 이 실험 클러스터와 Docker Desktop 클러스터는 etcd 멤버가 하나뿐이라 합의/장애 허용은 확인하지 못했다.
- DB 크기에는 쿼터(여기서 2.1GB)가 있다. 객체가 많아지면 쿼터에 걸릴 수 있다 (이 환경에서는 확인하지 않음).

#### 5. Pod / Node affinity

##### 5-1. 개요

| 기능 | 대상 | 의미 | 필수/선호 |
|---|---|---|---|
| `nodeSelector` | 노드 라벨 | 라벨이 일치하는 노드만 (가장 단순) | 필수 |
| **nodeAffinity** | 노드 라벨 | 연산자(`In`, `NotIn`, `Exists`, `Gt` ...)로 표현력 있게 | 필수 / 선호 |
| **podAffinity** | **다른 Pod**의 위치 | 특정 Pod와 **같은 곳**에 배치 | 필수 / 선호 |
| **podAntiAffinity** | **다른 Pod**의 위치 | 특정 Pod와 **다른 곳**에 배치 | 필수 / 선호 |
| topologySpreadConstraints | 같은 라벨 Pod들 | 도메인(노드/존)에 **고르게** 퍼뜨림 | 필수 / 선호 |
| taint / toleration | 노드가 거부 | 노드가 Pod를 **밀어냄** (4-3절) | |

- **필수(`requiredDuringSchedulingIgnoredDuringExecution`)**: 조건을 못 지키면 스케줄하지 않는다 → 필터링 단계.
- **선호(`preferredDuringSchedulingIgnoredDuringExecution`)**: 점수만 더해 준다. 못 지켜도 스케줄된다 → 스코어링 단계 (4-4의 ③).
- 이름 끝의 `IgnoredDuringExecution`은 **이미 실행 중인 Pod에는 조건 변경이 영향을 주지 않는다**는 뜻이다 (5-3).

##### 5-2. nodeAffinity (kind 노드: cp `disk=ssd gen=1 team=infra`, worker `disk=ssd gen=3`, worker2 `disk=hdd gen=5`)

| 조건 (`requiredDuring...`) | 후보 노드 | 배치된 노드 |
|---|---|---|
| `disk In [ssd]` | cp, worker | worker |
| `disk NotIn [ssd]` | worker2 | worker2 |
| `team Exists` | cp | control-plane |
| `team DoesNotExist` AND `disk In [ssd]` | worker | worker |
| `gen Gt 2` | worker, worker2 | worker2 |
| (`team Exists`) **OR** (`gen In [5]`) | cp, worker2 | worker2 |
| `disk In [ssd]` **AND** `gen In [5]` | 없음 | **Pending** |

```
$ k describe pod a7 | grep FailedScheduling
0/3 nodes are available: 3 node(s) didn't match Pod's node affinity/selector.
```

- **`matchExpressions` 안의 조건들은 AND**, **`nodeSelectorTerms`의 여러 항목은 OR**이다 (위 표의 6번째는 두 term, 7번째는 한 term 안의 두 expression).
- 연산자: `In`, `NotIn`, `Exists`, `DoesNotExist`, `Gt`, `Lt`. 후보가 여러 개일 때는 점수로 하나가 정해진다 (후보가 여러 개인 줄은 배치된 노드가 후보 중 하나라는 뜻이다).
- 필수와 선호는 같이 쓸 수 있다 (4-4의 ③은 선호만 쓴 실험).

##### 5-3. IgnoredDuringExecution과 라벨 변경

```
$ kc label node t11sched-worker2 disk-          # 이미 실행 중인 Pod(disk NotIn ssd 조건)가 있는 노드에서 라벨 제거
$ kc -n s2 get pod a2 -o wide
a2   Running   t11sched-worker2                  # Pod는 그대로 실행 (쫓겨나지 않음)

$ (disk=nvme 조건 Pod를 만들면 Pending)  →  kc label node t11sched-worker disk=nvme
$ kc -n s2 get pod a8                             # 6초 뒤
a8 → t11sched-worker                              # 노드 라벨이 생기자 대기하던 Pod가 스케줄됨
```

- 실행 중인 Pod의 노드 라벨이 바뀌어도 쫓겨나지 않는다. 조건은 **스케줄 시점에만** 평가된다.
- Pending Pod는 노드 라벨이 추가되면 스케줄러가 다시 평가해서 배정한다 (6초 안에).

##### 5-4. podAffinity / podAntiAffinity와 topologyKey

**`topologyKey`는 "같다/다르다"를 판단하는 단위**를 정하는 노드 라벨 키다.

| topologyKey | "같은 곳"의 의미 |
|---|---|
| `kubernetes.io/hostname` | 같은 노드 |
| `topology.kubernetes.io/zone` | 같은 가용 영역 (노드 여러 개가 한 존일 수 있다) |

이 실험 클러스터의 존: control-plane `zone-a`, worker `zone-a`, worker2 `zone-b`.

**① 노드 단위 podAntiAffinity (필수)**: 같은 앱의 Pod를 서로 다른 노드에 (`app=anti`, 4개, 노드 3개)

```
anti-...-4z92q  Pending
anti-...-bw62w  Running  t11sched-worker
anti-...-kw9kr  Running  t11sched-control-plane
anti-...-q7sph  Running  t11sched-worker2
$ k describe pod <Pending Pod>
0/3 nodes are available: 3 node(s) didn't match pod anti-affinity rules.
```

→ 3개만 각각 다른 노드에 배치되고 **4번째는 Pending**이다. 필수 anti-affinity는 노드 수 이상으로 복제할 수 없다.

**② 노드 단위 podAffinity (필수)**: `app=cache` Pod와 같은 노드에 (worker2에 cache를 두고 웹 3개)

```
cache on: t11sched-worker2
webs-...-422cp Running t11sched-worker2
webs-...-5x482 Running t11sched-worker2
webs-...-fbthg Running t11sched-worker2
```

→ 웹 3개가 모두 cache가 있는 worker2로 갔다.

**③ 존 단위 podAffinity**: cache를 `zone-a`의 worker에 두고 웹 4개를 `topologyKey: zone`으로

```
cache on: t11sched-worker
 2 t11sched-control-plane
 2 t11sched-worker                     # zone-a의 두 노드에 나뉨, zone-b(worker2)에는 없음
```

→ 같은 존이면 되므로 cache가 있는 노드가 아니라 **존 안의 다른 노드**(control-plane)에도 배치되었다. 노드 단위보다 완화된 조건이다.

**④ 대상이 없는 podAffinity**

```
$ (app=nothing Pod와 같은 노드에 있어야 하는 Pod 생성)
lonely   Pending       0/3 nodes are available: 3 node(s) didn't match pod affinity rules.
```

필수 podAffinity는 대상 Pod가 없으면 스케줄되지 않는다.

**⑤ 선호(soft) podAntiAffinity**: `preferredDuringScheduling`, weight 100, 5개

```
 1 control-plane
 2 worker
 2 worker2
```

→ 5개 모두 스케줄되었고(필수와 다르게 Pending 없음) 노드에 **가급적 퍼졌다** (1/2/2).

##### 5-5. topologySpreadConstraints (균등 분산)

`maxSkew: 1`, `topologyKey: zone`, `whenUnsatisfiable: DoNotSchedule`로 `app=spread` Pod를 늘렸다. (존 `zone-a`: 노드 2개, `zone-b`: 노드 1개)

```
replicas 4  →  worker 2, worker2 2          # zone-a 2개, zone-b 2개
replicas 7  →  worker 4, worker2 3          # zone-a 4개, zone-b 3개  (존 간 차이는 maxSkew 1 이내)
```

- **노드 수가 아니라 존(도메인) 단위로 Pod 수의 차이(skew)가 1 이내**가 되도록 맞춘다. zone-a의 노드가 2개여도 zone-a에 절반이 가고 zone-b가 나머지를 받는다.
- 같은 존 안에서는 이 제약이 균등을 요구하지 않아서 zone-a의 Pod가 모두 worker에 갔고 control-plane에는 가지 않았다 (노드 단위도 균등하게 하려면 `topologyKey: kubernetes.io/hostname` 제약을 추가해야 한다).
- `whenUnsatisfiable: ScheduleAnyway`(선호)와 `maxSkew` 외 필드(`minDomains` 등)는 이번에 시험하지 않았다.

##### 5-6. 정리: 무엇을 쓸까

| 목적 | 방법 |
|---|---|
| 특정 종류 노드에만 (GPU, SSD) | nodeSelector 또는 nodeAffinity(필수) |
| 가급적 특정 노드 선호 | nodeAffinity(선호) |
| 같은 앱의 복제본을 노드/존에 분산 (가용성) | **topologySpreadConstraints** (필수는 podAntiAffinity, 노드 수 제한 있음) |
| 앱 A와 B를 같은 노드/존에 붙임 (캐시, 지연) | podAffinity |
| 특정 노드를 전용으로 | taint + toleration (+ nodeAffinity) |

- podAffinity/podAntiAffinity는 **다른 Pod의 분포를 전부 계산**해야 해서 큰 클러스터에서는 스케줄링 비용이 크다 (문서 지식, 이 환경에서는 측정하지 않았다).

#### 6. 애플리케이션 상태 검사 (probe)

##### 6-1. 개념

kubelet이 컨테이너를 **주기적으로 검사**해서 상태에 따라 동작한다. 컨테이너 프로세스가 떠 있는 것(Running)과 **애플리케이션이 정상인 것**은 다르다.

| probe | 질문 | 실패하면 |
|---|---|---|
| **livenessProbe** | 살아 있나? (데드락, 먹통) | 컨테이너를 **재시작** |
| **readinessProbe** | 트래픽을 받을 준비가 됐나? | **Service 엔드포인트에서 제외** (재시작하지 않음). Pod는 `READY 0/1` |
| **startupProbe** | 시작이 끝났나? | 성공할 때까지 **liveness/readiness를 보류**. 끝까지 실패하면 재시작 |

| 방식 | 설정 | 성공 조건 |
|---|---|---|
| `exec` | 컨테이너 안에서 명령 실행 | 종료 코드 0 |
| `httpGet` | HTTP GET | 응답 코드 200~399 |
| `tcpSocket` | TCP 연결 | 연결 성공 |
| `grpc` | gRPC health check | 이번에 시험하지 않음 |

| 필드 | 의미 | 값 (실험에서 지정) |
|---|---|---|
| `initialDelaySeconds` | 컨테이너 시작 후 첫 검사까지 대기 | 5 |
| `periodSeconds` | 검사 간격 | 3~5 |
| `timeoutSeconds` | 한 번의 검사 제한 시간 | 1, 5 |
| `failureThreshold` | **연속 실패 횟수**가 이 값이면 실패로 판정 | 1~3, 15 |
| `successThreshold` | 연속 성공 횟수가 이 값이면 성공 (liveness는 1이어야 함) | 1 |

(`describe` 출력: `Liveness: exec [cat /tmp/healthy] delay=5s timeout=1s period=5s #success=1 #failure=3`)

##### 6-2. livenessProbe: 실패하면 재시작

`/tmp/healthy` 파일을 만들고 20초 뒤에 삭제하는 컨테이너에, `cat /tmp/healthy` exec probe를 `delay 5, period 5, failureThreshold 3`으로 걸었다.

```
이벤트:
Warning  Unhealthy  Liveness probe failed: cat: can't open '/tmp/healthy': No such file or directory
Normal   Killing    Container c failed liveness probe, will be restarted
$ k -n t11 describe pod live
Last State:  Terminated   Reason: Error   Exit Code: 137   Started: 22:46:02
Started:     22:47:07                                     RESTARTS 1
```

- 연속 3번 실패하면(`failureThreshold`) 컨테이너를 죽이고 재시작한다. 재시작되면 `RESTARTS`가 늘어난다. 같은 Pod 안에서 컨테이너만 재시작된다 (6장).
- **재시작까지 약 65초가 걸렸다.** 파일 삭제는 시작 20초 뒤, 검사 실패 3번은 약 15초이므로 대략 35~40초에 죽이기 시작했지만 종료 코드가 `137`(SIGKILL)이고 새 컨테이너 시작이 22:47:07이다. PID 1인 `sleep 600`은 SIGTERM 핸들러가 없어 종료되지 않았고, **`terminationGracePeriodSeconds`(기본 30초) 동안 기다린 뒤 SIGKILL**로 죽은 것으로 보인다. 첫 번째 `Killing` 이벤트의 정확한 시각을 기록하지 못해서 이 해석은 추정이다 (두 번째 주기의 `Killing` 이벤트와 시각은 일관되게 보였다).
- 그래서 **재시작까지의 시간 ≈ `initialDelay` + `period × failureThreshold` + 종료 유예 시간(SIGTERM을 무시하면 `terminationGracePeriodSeconds`)** 이다.

##### 6-3. readinessProbe: 실패하면 엔드포인트에서 빠지고, 재시작하지 않는다

`/tmp/ready`가 15초 뒤 생기고, 20초 뒤 사라졌다가, 15초 뒤 다시 생기는 컨테이너에 readiness exec probe(`period 3, failureThreshold 2`)를 걸고 Service를 연결했다. 5초 간격으로 상태를 기록했다.

```
시간   ready Pod(READY RESTARTS)   Service의 EndpointSlice (주소 | ready 조건)
  0s   0/1  0
  4s   0/1  0                       10.1.1.66|false
 20s   1/1  0                       10.1.1.66|true       ← 파일이 생기자 Ready
 41s   0/1  0                       10.1.1.66|false      ← 파일이 사라지자 NotReady (재시작 없음)
 53s   1/1  0                       10.1.1.66|true       ← 다시 Ready
```

```
이벤트: Warning Unhealthy Readiness probe failed: cat: can't open '/tmp/ready': No such file or directory
```

- Ready가 아닌 동안 **RESTARTS는 0이다.** readiness는 컨테이너를 죽이지 않고 트래픽만 막는다.
- EndpointSlice에는 주소가 남고 `ready` 조건이 false→true로 바뀐다. kube-proxy/인그레스 컨트롤러(8장)는 `ready=true`인 엔드포인트로만 트래픽을 보낸다 (이 환경에서 실제 트래픽 차단까지는 확인하지 않았고 엔드포인트 상태 변화만 확인했다).
- **readinessProbe가 없으면 컨테이너가 시작되자마자 Ready로 간주된다.** 아래 `slow-nostartup` Pod는 애플리케이션이 준비되기 전인데도 `READY 1/1`이었다.

**httpGet과 tcpSocket**

```
$ http-probe: nginx, readinessProbe httpGet /ready (404), livenessProbe httpGet /          → READY 0/1 계속, RESTARTS 0
$ tcp-probe : nginx, readinessProbe tcpSocket :8080 (닫혀 있음), livenessProbe tcpSocket :80 → READY 0/1 계속, RESTARTS 0
```

- HTTP probe는 응답 코드가 200~399가 아니면 실패다 (`/ready`가 404여서 NotReady). liveness는 `/`가 200이어서 재시작되지 않았다. **readiness와 liveness를 서로 다른 경로로 둘 수 있다.**
- TCP probe는 포트에 연결만 되면 성공이다.

##### 6-4. startupProbe: 느린 시작 보호

시작에 25초가 걸리는 컨테이너(`sleep 25; touch /tmp/up`)에, liveness(`period 3, failureThreshold 3` = 약 9초 안에 통과해야 함)를 걸고 startupProbe 유무를 비교했다.

```
시간   slow-nostartup(READY RESTARTS)   slow-startup(READY RESTARTS)
 5s    1/1 0                            0/1 0
16s    1/1 1                            0/1 0               ← startupProbe 없음: 아직 시작도 안 끝났는데 liveness가 죽임
26s    1/1 2                            0/1 0
31s    1/1 2                            1/1 0               ← startupProbe 통과(약 28~31초) 후 Ready, 재시작 0
52s    1/1 3 (재시작 반복)               1/1 0
```

- `startupProbe`(`period 3, failureThreshold 15` = 최대 45초) 가 있으면 시작이 끝날 때까지 liveness/readiness가 **검사를 시작하지 않아서** 느리게 뜨는 앱이 죽임 당하지 않는다. 없으면 liveness 한도 안에 못 뜨는 앱은 **재시작을 무한 반복**한다.
- 느리게 시작하는 앱(JVM 등)에서 `livenessProbe`의 `initialDelaySeconds`를 길게 늘리는 대신 `startupProbe`를 쓰는 것이 낫다. 시작이 빠를 때는 기다리지 않고 바로 liveness로 넘어가기 때문이다 (이 비교는 문서 지식이고 이번 실험은 느린 시작 케이스만 확인).

##### 6-5. timeoutSeconds

```
readinessProbe: exec `sh -c "sleep 3"`, period 5, failureThreshold 1
  timeoutSeconds: 1  →  READY 0/1   Unhealthy: command timed out: "sh -c sleep 3" timed out after 1s
  timeoutSeconds: 5  →  READY 1/1
```

검사가 `timeoutSeconds`를 넘기면 **실패**로 센다. 응답이 느린 앱에서 timeout이 너무 짧으면 정상인데도 NotReady/재시작이 된다.

##### 6-6. 롤링 업데이트와 readiness

6장 롤링 업데이트에서 새 Pod가 Ready가 되어야 옛 Pod를 줄였다. 새 버전에서 readiness가 항상 실패하도록 바꿨다 (`/tmp/ok`를 만들지 않음).

```
$ k -n t11 get deploy gate
gate  READY=2/2  UP-TO-DATE=1  AVAILABLE=2
$ k -n t11 get pod -l app=gate
gate-5cd5848ff6-l9qv6  1/1 Running       ← 옛 Pod 2개는 계속 서비스
gate-5cd5848ff6-qqncr  1/1 Running
gate-8dd86dbbc-j58g2   0/1 Running       ← 새 Pod는 NotReady
$ k -n t11 rollout status deploy/gate --timeout=5s
error: timed out waiting for the condition
```

새 Pod가 Ready가 되지 않으면 **롤아웃이 멈추고 옛 Pod가 서비스를 유지**한다 (잘못된 이미지 때와 같은 효과, 6장). readinessProbe는 **무중단 배포의 안전장치**다.

##### 6-7. 설계 시 주의

| 주의 | 이유 |
|---|---|
| liveness는 **앱 내부 상태만** 보게 | DB, 외부 API 장애까지 liveness가 실패로 보면 모든 Pod가 한꺼번에 재시작되어 상황이 악화된다 (문서 지식) |
| readiness에는 의존성(DB 연결 등) 포함 가능 | 준비가 안 됐으면 트래픽만 받지 않으면 된다 |
| 느리게 시작하는 앱은 `startupProbe` | 6-4절 |
| `timeoutSeconds`, `failureThreshold`를 여유 있게 | 일시적 지연으로 재시작/NotReady가 되지 않게 |
| liveness와 readiness를 같은 엔드포인트로 두지 않는 편이 안전 | 목적이 다르다 |
| SIGTERM을 처리하는 PID 1 | 재시작/종료가 `terminationGracePeriodSeconds`(30초)까지 늘어지지 않게 (6-2절) |

#### 7. 확인하지 못한 부분

- **CPU manager의 나머지**: topology manager, memory manager, `full-pcpus-only` 같은 옵션, 정책 `static`에서 Guaranteed가 아닌 Pod가 코어를 쓰는 경쟁은 확인하지 않았다. static 정책은 kind 노드의 kubelet 설정을 직접 수정해서 확인했고(`KubeletConfiguration` 패치는 워커에 적용되지 않았다), 12코어를 가진 노드(Docker VM과 공유)에서만 확인했다.
- **CPU 쓰로틀링이 주기 단위로 발생하는 짧은 버스트 사례**는 측정하지 않았다 (지속 부하만).
- **노드 압박 시 QoS별 퇴출 순서**: 노드의 메모리/디스크를 실제로 압박하는 실험은 하지 않았다 (`oom_score_adj` 값과 cgroup 위치만 확인).
- **`requests.memory`의 `memory.low` 반영**: 모든 Pod에서 0이었다. MemoryQoS 기능 게이트를 켠 환경에서는 달라질 수 있으나 확인하지 않았다.
- **`k top`**: metrics-server가 없어 확인하지 못했다. HPA도 다루지 않았다.
- **ResourceQuota**: 다른 scope(`Terminating`, `PriorityClass` 등), 스토리지 클래스별 쿼터, 쿼터 사용량이 갱신되는 지연은 확인하지 않았다. `count/secrets: 3`은 세 개까지만 만들어 봤고 네 번째는 시도하지 않았다.
- **LimitRange**: `type: Pod` 레벨, 컨테이너가 여럿인 Pod, initContainer는 확인하지 않았다.
- **스케줄러**: 필터링 플러그인 전체 목록/순서, 선점(preemption)과 PriorityClass, 스케줄러 프로파일/가중치 변경, 스코어링 가중치 표(문서 지식)와 점수 계산식의 정확한 값, `ImageLocality`가 양수 점수를 내는 경우, 스케줄러 리더 전환은 확인하지 못했다. 단일 `kube-scheduler`만 실험했다.
- **taint**: `PreferNoSchedule`, `NoExecute`와 `tolerationSeconds`는 실험하지 않았다.
- **어피니티**: `namespaceSelector`/`namespaces` 지정, `matchLabelKeys`, `topologySpreadConstraints`의 `ScheduleAnyway`, `minDomains`, 노드 단위 spread, 여러 제약의 조합은 확인하지 않았다. 후보 노드가 여러 개인 경우 최종 선택은 점수와 임의 선택에 따른다는 점만 확인했다.
- **etcd**: 단일 멤버라 Raft 합의, 장애 허용, 백업/복구, compaction과 `410 Expired`의 관계, DB 쿼터 초과 동작은 확인하지 않았다. etcd watch 이벤트 캡처는 실패했다 (클라이언트에서 이벤트 수신을 확인하지 못함).
- **probe**: `grpc` probe, `successThreshold`가 1보다 큰 readiness, `initialDelaySeconds` 단독 동작, probe 실패가 `terminationGracePeriodSeconds`와 맞물리는 정확한 타임라인(6-2절은 추정), `probe`의 `terminationGracePeriodSeconds` 필드(probe 수준), Pod가 Ready가 아닐 때 Service 트래픽이 실제로 차단되는 것(엔드포인트의 `ready` 조건만 확인)은 하지 못했다.
- **ImageLocality용 큰 이미지 실험**과 **etcd에서 본 Pod `Version` 증가의 원인 분해**는 시도했으나 확인하지 못했다.
