### 14장. 쿠버네티스 모니터링

> 정리 범위: 모니터링 메트릭의 분류(종류), Grafana 대시보드에서 봐야 할 지표, 추천 대시보드 레이아웃
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드). 실험을 위해 `t14` 네임스페이스에 **Prometheus v3.15.1**(kube-state-metrics v2.20.0, node-exporter v1.12.1 포함, helm chart `prometheus` 29.38.0), **Grafana 12.3.1**(chart `grafana` 10.5.15), **metrics-server v0.9.0**을 설치했다. 문제가 있는 워크로드(CrashLoop, OOM, Pending, ImagePullBackOff, 실패한 Job, CPU limit에 걸린 Pod)를 일부러 만들어 지표가 어떻게 보이는지 확인했고, **끝나고 모두 삭제했다** (helm release, 네임스페이스 3개, 클러스터 범위 APIService/ClusterRole, 이미지, 노드 디렉터리).
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl/helm 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`(helm은 `--kube-context docker-desktop`)을 명시했다. helm 저장소 설정은 이 PC의 helm 설정을 건드리지 않도록 별도 파일(`--repository-config`)을 썼다. 이 PC의 다른 kind 클러스터에는 이미 설치된 모니터링이 있었지만 **건드리지 않았다.**
>
> 이 장에는 검증한 Grafana 대시보드 JSON이 함께 있다: [`dashboard-k8s-overview.json`](dashboard-k8s-overview.json)

#### 목차

1. 모니터링 메트릭 분류
2. Grafana 대시보드에서 봐야 할 지표
3. 추천 대시보드 레이아웃
4. 확인하지 못한 부분

---

#### 1. 모니터링 메트릭 분류

##### 1-1. 모니터링 데이터의 세 종류

| 종류 | 질문 | 예 | 이 장 |
|---|---|---|---|
| **메트릭** (metrics) | "얼마나? 추세는?" 숫자 시계열 | CPU 사용률, 요청 수, 지연 시간 | 다룸 |
| 로그 (logs) | "무슨 일이 있었나?" 이벤트 텍스트 | 에러 스택, 접근 로그 | 다루지 않음 |
| 트레이스 (traces) | "요청이 어디서 느렸나?" 서비스 간 호출 경로 | 분산 추적 | 다루지 않음 |

- 메트릭은 값이 작고 저렴해서 **알림과 대시보드의 기본**이다. 원인 분석은 로그/트레이스로 이어진다 (대시보드에서 이상을 보고 → 로그로 내려가기).

##### 1-2. 쿠버네티스의 두 가지 메트릭 파이프라인

| 구분 | **리소스 메트릭 파이프라인** | **전체 메트릭 파이프라인** |
|---|---|---|
| 구성 | kubelet → **metrics-server** → **Metrics API** | exporter들 → **Prometheus** → Grafana/알림 |
| 내용 | 노드/Pod/컨테이너의 **현재** CPU, 메모리만 | 수천 종, **시계열 이력**, 사용자 정의 지표 |
| 소비자 | `kubectl top`, **HPA**, VPA | 대시보드, 알림, 용량 계획 |
| 저장 | 메모리 안(최근값), 이력 없음 | 시계열 DB (이 실험: 보존 2시간) |

**리소스 메트릭 파이프라인 (실측)**

```
$ k top node                                          # metrics-server 설치 전
error: Metrics API not available
$ helm install ms metrics-server/metrics-server -n t14 --set 'args={--kubelet-insecure-tls}'
$ k top node
NAME             CPU(cores)   CPU(%)   MEMORY(bytes)   MEMORY(%)
docker-desktop   816m         6%       2539Mi          32%
$ k top pod -n t14w --containers
burner  c  301m  0Mi                              ← 컨테이너별 값 (limit 300m에 붙어 있음)
$ k get apiservice v1beta1.metrics.k8s.io -o jsonpath='{.spec.service.name}.{.spec.service.namespace} available={.status.conditions[0].status}'
ms-metrics-server.t14 available=True                ← Metrics API는 APIService(집계 API)로 등록된다
$ k get --raw /apis/metrics.k8s.io/v1beta1/namespaces/t14w/pods/burner
{"kind":"PodMetrics","timestamp":"...","window":"14.984s"} [{'name':'c','usage':{'cpu':'300287106n','memory':'724Ki'}}]
$ k api-resources --api-group=metrics.k8s.io       →  nodes (NodeMetrics), pods (PodMetrics)
$ k get --raw /api/v1/nodes/docker-desktop/proxy/metrics/resource        # metrics-server가 긁는 kubelet 엔드포인트
# TYPE container_cpu_usage_seconds_total counter ... pod_cpu_usage_seconds_total ... node_memory_working_set_bytes ...
```

- 6장에서 `kubectl top`이 안 된 이유(`Metrics API not available`)가 이것이다. **metrics-server가 없으면 `kubectl top`과 HPA가 동작하지 않는다.** 이 환경에서는 설치가 필요했다.
- Metrics API는 `/apis/metrics.k8s.io/v1beta1/...`이고 **12장의 CRD가 아니라 집계 API(APIService)** 로 제공된다. metrics-server Pod가 요청을 받아 응답한다. 응답은 값이 **하나(현재값, window 약 15초)** 뿐이다 (이력 없음).
- metrics-server는 kubelet의 `/metrics/resource`(CPU/메모리 usage 몇 종류만)를 약 15초마다 긁는다. 이 경로는 Prometheus가 긁는 cAdvisor(`/metrics/cadvisor`)와 **다른 엔드포인트**다 (1-6절의 환경 차이 참고).
- 이 환경(Docker Desktop)의 kubelet 인증서 때문에 `--kubelet-insecure-tls`를 줬다. 운영에서는 인증서를 정상 구성하는 것이 맞다.

**HPA는 Metrics API의 소비자다 (실측)**

`requests.cpu 100m / limits.cpu 200m`으로 CPU를 계속 쓰는 Deployment에 HPA(목표 CPU 50%, 최대 3개)를 걸었다.

```
 15s  cpu: <unknown>/50%  1 3 0                    ← 처음 약 60초는 지표가 없어 unknown
 75s  cpu: 201%/50%       1 3 1
135s  cpu: 201%/50%       1 3 3                    ← 약 135초 만에 1 → 3
$ k -n t14h get hpa busy -o jsonpath='{range .status.conditions[*]}{.type}={.status}({.reason}) {end}'
AbleToScale=True(ReadyForNewScale) ScalingActive=True(ValidMetricFound) ScalingLimited=True(TooManyReplicas)
$ k -n t14h top pod         →  busy-...  201m  (3개 모두 limit 200m에 붙어 있음)
```

- HPA의 사용률 `201%`는 **limit이 아니라 requests 대비**(201m ÷ 100m)다. 요청을 낮게 잡으면 사용률이 크게 나와 일찍 스케일 아웃된다 (11장의 requests 의미와 연결).
- 처음 `<unknown>`은 metrics-server가 아직 값을 모으지 못한 시간이다. 이 시간은 이 환경에서 1회 관찰한 값이다.

##### 1-3. 수집 대상 계층 (전체 메트릭 파이프라인)

Prometheus는 **각 대상의 `/metrics` HTTP 엔드포인트를 주기적으로 긁어가는(pull) 방식**이다. 대상별 exporter가 메트릭을 노출한다.

```
$ (Prometheus targets, 이 실험의 scrape job)
kubernetes-api-servers        up x1       ← API 서버 자체 메트릭
kubernetes-nodes              up x1       ← kubelet 자체 메트릭
kubernetes-nodes-cadvisor     up x1       ← 컨테이너 리소스 (cAdvisor, kubelet 안에 내장)
kubernetes-service-endpoints  up x5       ← kube-state-metrics, node-exporter, coredns 등
prometheus                    up x1
$ (metric 이름 수) 전체 1207 / api-servers 351 / kubelet 314 / cadvisor 73 / service-endpoints 467
$ (스크레이프 한 번의 샘플 수) cadvisor 3,566 / kubelet 2,088 / service-endpoints 4,322 / **api-servers 35,274**
$ (Prometheus 자체) head series 46,593 / 초당 약 3,000 샘플 수집 / 메모리(RSS) 약 339MB   (보존 2시간, 노드 1개)
```

| 계층 | 출처 (exporter) | 무엇을 보나 | 메트릭 접두어 (실측) | 대표 메트릭 |
|---|---|---|---|---|
| **① 노드(호스트)** | node-exporter (DaemonSet, 13장) | 노드 OS의 CPU, 메모리, 디스크, 네트워크, 로드 | `node_*` (273종) | `node_cpu_seconds_total`, `node_memory_MemAvailable_bytes`, `node_filesystem_avail_bytes`, `node_load1`, `node_network_receive_bytes_total` |
| **② 컨테이너/Pod 리소스** | **cAdvisor** (kubelet 내장, `/metrics/cadvisor`) | 컨테이너/Pod의 CPU, 메모리, 네트워크, 파일시스템 사용량 | `container_*` (59종), `machine_*` | `container_cpu_usage_seconds_total`, `container_memory_working_set_bytes`, `container_network_receive_bytes_total`, `container_oom_events_total` |
| **③ kubelet** | kubelet (`/metrics`) | 노드 에이전트의 동작: 실행 Pod 수, 런타임 지연, Pod 시작 시간, 볼륨 | `kubelet_*` (94종) | `kubelet_running_pods`, `kubelet_pod_start_duration_seconds`, `kubelet_runtime_operations_duration_seconds` |
| **④ 오브젝트 상태** | **kube-state-metrics** (Deployment 1개) | API 오브젝트의 **선언된 상태/현재 상태** (사용량이 아니라 "상태") | `kube_*` (111종) | `kube_pod_status_phase`, `kube_pod_container_status_restarts_total`, `kube_deployment_status_replicas_available`, `kube_node_status_condition`, `kube_pod_container_resource_requests` |
| **⑤ 컨트롤 플레인** | API 서버 (`/metrics`), etcd, scheduler, controller-manager | 요청 처리량/지연/오류, 큐, etcd 저장소 | `apiserver_*` (126종), `etcd_*`, `workqueue_*`, `rest_client_*`, `authentication_*` | `apiserver_request_total`, `apiserver_request_duration_seconds`, `etcd_request_duration_seconds`, `workqueue_depth` |
| **⑥ 애드온** | CoreDNS 등 | 클러스터 DNS 등 | `coredns_*` (43종) | `coredns_dns_requests_total` |
| **⑦ 애플리케이션** | 앱이 직접 노출 (`/metrics`) | 비즈니스/서비스 지표 | 앱마다 | 요청 수, 오류 수, 지연, 큐 길이 |
| (Prometheus 자체) | `prometheus_*` (232종), `go_*`(143), `process_*` | 모니터링 시스템의 건강 | | `prometheus_tsdb_head_series` |

**②와 ④는 이름이 비슷하지만 전혀 다른 질문에 답한다.**

| | cAdvisor `container_*` | kube-state-metrics `kube_*` |
|---|---|---|
| 답하는 질문 | **얼마나 쓰고 있나** (실제 사용량) | **어떤 상태인가** (선언/상태) |
| 예 | "이 Pod가 CPU를 0.3코어 쓴다" | "이 Pod는 Pending이고 requests가 100m이다", "재시작 4번", "OOMKilled로 종료됨" |
| 값의 출처 | cgroup (커널 통계) | API 서버의 오브젝트 |
| 결합 | `usage / kube_..._limits`처럼 둘을 나눠 **사용률**을 만든다 (2절) |

- **cAdvisor**는 kubelet에 들어 있는 컨테이너 통계 수집기라 별도 설치가 없다. kube-state-metrics와 node-exporter는 따로 배포한다. Prometheus가 kubelet/cAdvisor를 긁는 경로는 **API 서버 프록시**(`/api/v1/nodes/<노드>/proxy/metrics/cadvisor`)를 쓰는 구성이 흔하다 (이 chart의 기본 구성이고 `kubernetes-nodes-cadvisor` job이 `up`이었다).
- **카디널리티(시계열 수)에 주의**: API 서버 한 번의 스크레이프가 **35,274 샘플**로 다른 대상들(2~4천)의 약 10배였다. 히스토그램 버킷(`apiserver_request_duration_seconds_bucket` 등)이 라벨 조합마다 시계열을 만들기 때문이다. 시계열 수가 많아질수록 Prometheus 메모리와 비용이 늘어난다 (이 실험은 총 46,593 시계열, 메모리 약 339MB).

##### 1-4. Prometheus 메트릭 타입 (실측으로 확인한 대표 메트릭)

| 타입 | 의미 | 쓰는 법 | 실측 예 |
|---|---|---|---|
| **counter** | **계속 증가만** 하는 누적값 (재시작 시 0으로 리셋) | **`rate()` / `increase()`** 로 변화율을 본다. 값 자체는 의미 없음 | `node_cpu_seconds_total`, `container_cpu_usage_seconds_total`, `apiserver_request_total`, `kube_pod_container_status_restarts_total`, `container_oom_events_total` |
| **gauge** | 오르내리는 현재값 | 그대로 그래프 | `node_memory_MemAvailable_bytes`, `container_memory_working_set_bytes`, `kube_pod_status_phase`, `kubelet_running_pods`, `apiserver_current_inflight_requests` |
| **histogram** | 값의 **분포**(버킷별 누적 개수 + 합계 + 개수) | **`histogram_quantile(0.99, sum by (le)(rate(..._bucket[5m])))`** 로 백분위 계산 | `apiserver_request_duration_seconds_bucket`, `etcd_request_duration_seconds_bucket`, `kubelet_pod_start_duration_seconds_bucket` |
| summary | 클라이언트가 미리 계산한 분위수 | 그대로 사용 (집계 불가) | (이 장에서 직접 쓰지 않음) |

- 실수하기 쉬운 점: **counter를 `rate()` 없이 그리면** 계속 오르는 직선이 되어 쓸모가 없다. 위 PromQL이 모두 `rate`/`increase`로 시작하는 이유다.
- 백분위는 **평균이 아니라 p99** 로 본다 (평균은 느린 요청을 숨긴다). histogram에서 `histogram_quantile`로 계산한다.
- 라벨(`namespace`, `pod`, `verb` ...)로 쪼개고(`sum by`) 합친다. `up == 0`처럼 **스크레이프 자체가 실패**한 것도 별도 신호다.

##### 1-5. 관점별 분류 방법론

지표가 수천 종이어서 "무엇을 볼지"를 정하는 틀이 필요하다.

| 방법 | 대상 | 질문 | 이 장에서의 대응 |
|---|---|---|---|
| **USE** (Brendan Gregg) | **자원**(노드, 디스크, 네트워크) | **U**tilization(사용률), **S**aturation(포화/대기), **E**rrors | 노드 행: CPU 사용률, 메모리 가용, **load/코어**(포화), 디스크 IO |
| **RED** | **서비스**(요청 처리) | **R**ate(요청률), **E**rrors(오류율), **D**uration(지연) | API 서버 행(요청률/5xx/p99), 앱 행(앱이 노출한 지표) |
| **4 Golden Signals** (Google SRE) | 서비스 | Latency, Traffic, Errors, Saturation | RED + 포화(inflight, workqueue depth, CPU/limit) |

- 자원(노드, 디스크)은 **USE**, 사용자에게 서비스를 주는 컴포넌트(API 서버, 앱)는 **RED**로 본다. 쿠버네티스에서는 둘이 섞인다 (API 서버는 서비스이므로 RED, 그 아래 노드는 USE).

##### 1-6. 이 환경(Docker Desktop)에서 달랐던 점

일반적인 클러스터와 다르게 보인 부분이다. **다른 환경(containerd/CRI-O 기반 클러스터)에서는 다를 수 있다.**

| 항목 | 이 환경에서 관찰 | 영향 |
|---|---|---|
| **컨테이너별 cAdvisor 시계열** | `container_*`가 **Pod 단위 cgroup 시리즈**(`container=""`, `pod`/`namespace` 라벨 있음)만 있고 **컨테이너별(`container="c"`) 시리즈가 없음**. `kubectl get --raw .../proxy/metrics/cadvisor`로 raw 출력도 같았다 | 흔한 쿼리 `container!=""`가 **빈 결과**. 이 장은 `container="",pod!=""`(Pod 합계)를 썼다. 이 형태는 일반 클러스터에도 있어서 **이중 계산 없이 Pod 합계**를 구할 수 있다. 컨테이너별 분해는 일반 클러스터에서만 가능 |
| CPU 쓰로틀링 지표 | `container_cpu_cfs_throttled_periods_total` 등 **없음** (`container_cpu*` 목록에 cfs 계열이 없다) | 11장에서 cgroup 파일(`cpu.stat`)로 직접 본 쓰로틀링을 **Prometheus로는 못 본다**. 대신 `사용량/limit` 비율이 1에 붙는지로 추정 (아래 2절) |
| 파일시스템 지표 | `node_filesystem_*`이 `tmpfs` 2개뿐 (node-exporter가 호스트 루트를 마운트하지 않게 구성하고 Docker Desktop VM 구조도 영향) | 디스크 사용률 패널이 비었다 → 이 환경에서는 디스크 용량 패널을 검증하지 못했다 |
| PVC 사용량 | `kubelet_volume_stats_*` **없음** (PVC를 쓰는 Pod에 20MB를 쓰게 했지만 지표가 나오지 않았다) | PVC 용량 패널을 검증하지 못했다 (일반 클러스터에서는 CSI/in-tree 볼륨이 kubelet이 통계를 낸다고 알려져 있으나 이 환경에서는 확인하지 못했다) |
| scheduler / controller-manager / etcd 메트릭 | **수집되지 않음** (`scheduler_*`, `etcd_server_*`, `controller_runtime_*` 모두 없음). 원인: 모두 **`127.0.0.1`에만 바인딩** | 아래 표 참고 |
| kube-state-metrics | 오브젝트가 존재할 때만 시리즈가 생긴다 (PVC/Job/StatefulSet 지표는 해당 오브젝트를 만들기 전에는 `absent`) | 패널이 비어 있어도 "문제 없음"일 수 있다. 오브젝트가 없다는 뜻 |

**컨트롤 플레인 메트릭 엔드포인트의 접근성 (실측)**

| 컴포넌트 | 바인딩/포트 | Pod → 노드 IP | 노드(VM 안) → 127.0.0.1 |
|---|---|---|---|
| etcd | `--listen-metrics-urls=http://127.0.0.1:2381` | **Connection refused** | **메트릭 응답** (`# HELP etcd_cluster_version ...`) |
| kube-scheduler | `--bind-address=127.0.0.1` (포트 10259, https) | **Connection refused** | **403 Forbidden** (인증 필요) |
| kube-controller-manager | `--bind-address=127.0.0.1` (포트 10257, https) | **Connection refused** | **403 Forbidden** (인증 필요) |
| kube-apiserver | 6443 (Service `kubernetes`로 노출) | 수집됨 (`kubernetes-api-servers` up) | - |

- kubeadm으로 만든 클러스터의 기본 구성이다 (6장에서 `--bind-address=127.0.0.1` 확인). **클러스터 안의 Prometheus Pod는 scheduler/controller-manager/etcd 메트릭에 닿을 수 없다.** 운영에서는 `bind-address`/`listen-metrics-urls`를 바꾸거나, 컨트롤 플레인 노드에서 접근하는 별도 설정, 인증 토큰이 필요하다 (이 환경에서 바꿔 보지는 않았다). 매니지드 서비스(EKS 등)에서는 컨트롤 플레인 메트릭을 제공하는 방식이 따로 있다 (문서 지식).
- 이 환경에서도 **API 서버가 노출하는 `etcd_request_duration_seconds`로 etcd 지연을 간접적으로 볼 수 있다** (etcd 서버 자체의 `etcd_server_*`와는 별개).

#### 2. Grafana 대시보드에서 봐야 할 지표

##### 2-1. 원칙: 질문 순서대로 본다

지표를 나열하기보다 **장애 때 던지는 질문 순서**로 정리하면 대시보드가 실용적이다.

| 순서 | 질문 | 해당 지표군 |
|---|---|---|
| 1 | **지금 깨진 게 있나?** | 노드 Ready, 실패/Pending Pod, 재시작, CrashLoop, API 5xx |
| 2 | **자원이 모자라지 않나?** (용량) | requests/allocatable, 사용량, 노드 포화 |
| 3 | **어느 네임스페이스/워크로드가 문제인가?** | 네임스페이스별 CPU/메모리, limit 근접, 재시작 Top |
| 4 | **컨트롤 플레인이 건강한가?** | API 서버 지연/오류, etcd 지연, 큐 |
| 5 | **서비스 지표는?** | RED (앱) |

아래 표의 **"실측"** 열은 문제 워크로드를 일부러 만든 상태에서 해당 쿼리를 Prometheus에 실행한 결과다.

##### 2-2. 필수 지표 (반드시 보는 것)

**A. 상태 한눈에 (Health)**

| 지표 | PromQL | 의미/알림 기준 (제안) | 실측 |
|---|---|---|---|
| Ready 노드 수 | `sum(kube_node_status_condition{condition="Ready",status="true"})` | 노드 수와 같아야 함. **NotReady ≥ 1이면 즉시** | 1 |
| 정상이 아닌 Pod | `sum(kube_pod_status_phase{phase=~"Pending\|Failed\|Unknown"})` | 0이어야 함. **Pending이 오래가면** 스케줄/자원 문제 (11장) | 4 (Pending 1, ImagePullBackOff Pod 1, 실패한 Job의 Pod 2) |
| Pod 단계 분포 | `sum by (phase)(kube_pod_status_phase)` | Running 비율 | 5개 phase 시리즈 |
| 컨테이너 재시작 | `sum(increase(kube_pod_container_status_restarts_total[1h]))` / Top: `topk(5, sum by (namespace,pod)(increase(...[15m])))` | **재시작이 늘면 CrashLoop/OOM/liveness 실패** (11장) | 1시간 증가 7.8, `oomer`가 Top (4.3) |
| 대기 중인 컨테이너 사유 | `sum by (reason)(kube_pod_container_status_waiting_reason == 1)` | `CrashLoopBackOff`, `ImagePullBackOff`, `CreateContainerConfigError`(7장) 등을 **이름으로** 구분 | `ImagePullBackOff` 1, `CrashLoopBackOff` 시리즈 포함 총 2종 |
| OOMKilled | `kube_pod_container_status_last_terminated_reason{reason="OOMKilled"} == 1` | 메모리 limit 부족 (11장) | `oomer` 1건 |
| 준비 안 된 Pod | `kube_pod_status_ready{condition="false"} == 1` | readiness 실패 (11장 6절) | 6건 (시스템 Pod 포함) |
| Deployment 가용성 | `kube_deployment_spec_replicas - kube_deployment_status_replicas_available > 0` | 롤아웃 정체/Pod 부족 (6장) | 0 (이 시점 문제 없음) |
| 실패한 Job | `kube_job_status_failed > 0` | 배치 실패 (13장) | 1 (`BackoffLimitExceeded`) |
| API 서버 5xx 비율 | `sum(rate(apiserver_request_total{code=~"5.."}[5m])) / sum(rate(apiserver_request_total[5m]))` | 1% 이상이면 이상 | 0 |

**B. 용량 (Capacity)**

| 지표 | PromQL | 의미 | 실측 |
|---|---|---|---|
| CPU requests / allocatable | `sum(kube_pod_container_resource_requests{resource="cpu"}) / sum(kube_node_status_allocatable{resource="cpu"})` | **1에 가까우면 새 Pod가 Pending** (스케줄러는 requests로 판단, 11장 2절) | **5.42** (주의) |
| Memory requests / allocatable | `...{resource="memory"}...` | 같음 | 0.048 |
| 클러스터 CPU 사용량 | `sum(rate(container_cpu_usage_seconds_total{container="",pod!=""}[5m]))` | **사용량 ≪ requests면 과다 요청(낭비)** | 0.26코어 |
| 클러스터 메모리 | `sum(container_memory_working_set_bytes{container="",pod!=""})` | `working_set`이 OOM 판단에 쓰이는 값 | 약 1.08GB |

- **주의: CPU requests / allocatable이 5.42(542%)로 나온 것**은 kube-state-metrics가 **Pending Pod의 requests까지 합산**하기 때문이다 (일부러 만든 `cpu: 64` 요청 Pod 한 개가 포함). 실제로 노드에 배정된 용량을 보려면 Running Pod만 곱해서 합쳐야 한다. 예: `sum(kube_pod_container_resource_requests{resource="cpu"} * on(namespace,pod) group_left() (kube_pod_status_phase{phase="Running"} == 1))`. 이 보정 쿼리는 아이디어로 적었고 이 환경에서 실행해 보지는 못했다 (대시보드 JSON에는 위 검증된 쿼리를 그대로 두었다).
- **`working_set`** 은 캐시를 제외한 "회수하기 어려운 메모리"로, kubelet의 OOM/퇴출 판단 기준에 가깝다. `container_memory_usage_bytes`는 파일 캐시를 포함해서 더 크게 보인다.

**C. 노드 (USE)**

| 지표 | PromQL | 의미 | 실측 |
|---|---|---|---|
| CPU 사용률 | `1 - avg by (instance)(rate(node_cpu_seconds_total{mode="idle"}[5m]))` | U: 지속 80% 이상이면 주의 | 28% |
| 메모리 가용 | `node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes` | **`Free`가 아니라 `Available`** (캐시 회수 가능분 포함). 10% 이하면 위험 | 68% |
| Load / 코어 수 | `node_load1 / scalar(count(count by (cpu)(node_cpu_seconds_total{mode="idle"})))` | **S(포화)**: 지속적으로 1 초과면 실행 대기가 코어보다 많음 | 0.24 |
| 네트워크 | `sum(rate(node_network_receive_bytes_total{device!~"lo\|veth.*\|docker.*\|br-.*"}[5m]))` (+transmit) | 대역폭, 비정상적 급증 | 약 6.2MB/s |
| 디스크 IO | `rate(node_disk_io_time_seconds_total[5m])` | 1에 가까우면 디스크 포화 | 0 (18개 디바이스) |
| 디스크 용량 | `1 - node_filesystem_avail_bytes{...} / node_filesystem_size_bytes{...}` | 85% 이상 주의 | **이 환경에서는 `tmpfs`뿐이라 빈 결과** |

- 처음 만든 `load1 per core` 쿼리는 빈 결과였다. 라벨이 달라 벡터끼리 나눠지지 않았기 때문이고, 분모를 `scalar(...)`로 감싸서 해결했다 (PromQL의 **라벨 매칭 함정**).

**D. 워크로드 (네임스페이스/Pod)**

| 지표 | PromQL | 의미 | 실측 |
|---|---|---|---|
| 네임스페이스별 CPU | `sum by (namespace)(rate(container_cpu_usage_seconds_total{container="",pod!=""}[5m]))` | 누가 많이 쓰나 (비용/원인) | t14w 0.148, kube-system 0.083, t14 0.031 |
| 메모리 Top Pod | `topk(10, sum by (namespace,pod)(container_memory_working_set_bytes{container="",pod!=""}))` | 메모리 많이 쓰는 Pod | apiserver 289MB, prometheus 249MB, grafana 127MB |
| **CPU 사용량 / limit** | `sum by (namespace,pod)(rate(...[5m])) / sum by (namespace,pod)(kube_pod_container_resource_limits{resource="cpu"})` | **1에 붙으면 쓰로틀링 중** (11장 1-3절). cAdvisor에 cfs 지표가 없는 이 환경에서 쓰로틀링의 대안 | `burner` 0.47 (5분 평균, 시작 직후라 낮음) |
| **CPU 사용량 / requests** | 위와 같은 형태, 분모만 `requests` | **1 초과 = 요청보다 많이 사용** (경합 시 밀림, HPA 사용률 기준) | `burner` 1.40 |
| **메모리 working set / limit** | `... / sum by (namespace,pod)(kube_pod_container_resource_limits{resource="memory"})` | **1에 붙으면 OOMKilled 임박** | coredns 0.20, burner 0.019 |
| Pod 네트워크 | `sum by (namespace,pod)(rate(container_network_receive_bytes_total[5m]))` | 트래픽 급증 | 있음 |

- `kubectl top`이 `burner 301m`(limit 300m)을 보였는데 Prometheus의 사용량/limit이 0.47로 낮게 나온 것은 **5분 평균이 Pod 시작 직후를 포함**하기 때문이다. 쓰로틀링 판단에는 짧은 구간(`[1m]`)이 낫다 (1분 구간으로 같은 Pod를 재면 0.241코어였고 `top`의 301m와 차이가 있었다. 두 값은 측정 구간과 시점이 달라서 정확히 비교하지 못했다).
- 리소스 사용량을 **limit/requests와 나눈 비율**로 보면 서로 다른 크기의 Pod를 한 눈에 비교할 수 있다. 사용량 자체(절대값)와 비율을 같이 본다.

**E. 컨트롤 플레인 (RED)**

| 지표 | PromQL | 의미 | 실측 |
|---|---|---|---|
| API 요청률 (코드별) | `sum by (code)(rate(apiserver_request_total[5m]))` | R: 요청량, 4xx/5xx 비중 | 10개 코드 시리즈 |
| **API p99 지연** (verb별) | `histogram_quantile(0.99, sum by (le,verb)(rate(apiserver_request_duration_seconds_bucket{verb!~"WATCH\|CONNECT"}[5m])))` | D: **WATCH/CONNECT는 길게 열려 있어 제외**해야 한다 | LIST 약 5ms 외 7개 verb |
| **etcd p99 지연** (API 서버 관점) | `histogram_quantile(0.99, sum by (le,operation)(rate(etcd_request_duration_seconds_bucket[5m])))` | **etcd가 느리면 모든 API가 느려진다** (6장) | create 약 29ms 외 5개 |
| in-flight 요청 | `sum by (request_kind)(apiserver_current_inflight_requests)` | S: 포화 (한도 도달 시 429) | 2 |
| workqueue 깊이 | `sum by (name)(workqueue_depth)` | 컨트롤러가 밀리고 있나 | 28개 큐 모두 0 근처 |
| 저장된 오브젝트 수 | `topk(5, apiserver_storage_objects)` | etcd 용량 계획 (12장 CR 개수) | 상위 73 |

**F. kubelet**

| 지표 | PromQL | 의미 | 실측 |
|---|---|---|---|
| 노드당 실행 Pod | `kubelet_running_pods` | 노드별 상한(기본 110) 대비 | 19 |
| Pod 시작 p99 | `histogram_quantile(0.99, sum by (le)(rate(kubelet_pod_start_duration_seconds_bucket[10m])))` | 이미지 pull/볼륨/CNI 지연 | 28.3초 (이 환경은 이미지를 새로 pull한 Pod가 있어서 큼) |

##### 2-3. 있으면 좋은 지표

| 지표 | 쓰임 | 이 환경 |
|---|---|---|
| `container_memory_rss` vs `working_set` | 캐시 vs 실사용 구분 | cAdvisor 존재 |
| `container_oom_events_total` | **OOM이 실제로 일어난 횟수** (`kube_..._last_terminated_reason`는 마지막 사유만) | 시리즈는 있었으나 `increase(...[1h]) > 0` 결과는 **0건** (이 환경에서 OOM 이벤트가 이 카운터로 잡히지 않았다. 이유는 확인하지 못했다) |
| `kube_daemonset_status_number_unavailable` | DaemonSet 가용성 (13장) | 0 |
| `kube_statefulset_status_replicas_ready` | StatefulSet 준비 수 (13장) | 이 시점에 StatefulSet이 없어 `absent` |
| `kube_persistentvolumeclaim_status_phase` | PVC Pending/Lost (9장) | `Bound` 1건 확인 |
| `kubelet_volume_stats_used_bytes / capacity` | **PVC 용량 임박** | 이 환경에서 지표 없음 |
| `rest_client_requests_total`, `workqueue_*` | 컨트롤러 건강 | 있음 |
| `coredns_dns_request_duration_seconds` | DNS 지연 (서비스 디스커버리 문제의 흔한 원인) | coredns 지표 있음 (43종) |
| 이미지 pull 시간, 스케줄링 지연 | 배포 속도 | `kubelet_pod_start_duration_seconds`만 확인 |
| 인증서 만료일, 노드 디스크 압박 `kube_node_status_condition{condition=~"DiskPressure|MemoryPressure|PIDPressure"}` | 노드 상태 | 이 시점 검증하지 않음 |
| 비용: `requests` 대비 `usage` 비율 | 리소스 낭비 | 위 B, D |

##### 2-4. 알림으로 옮길 후보 (대시보드의 "언제 알려줄까")

대시보드는 사람이 보지 않으면 소용없으므로 중요한 것은 알림 규칙으로 옮긴다. 아래는 위 쿼리를 조건식으로 만든 후보이고, **알림 규칙(Alertmanager)은 설치하지 않았다.** 조건식이 데이터를 반환하는지만 확인했다.

| 알림 | 조건 (제안) | 확인한 것 |
|---|---|---|
| 노드 NotReady | `kube_node_status_condition{condition="Ready",status="true"} == 0` 5분 | 쿼리 형태만 |
| CrashLoop/재시작 급증 | `increase(kube_pod_container_status_restarts_total[15m]) > 3` | **2개 시리즈 반환**(`oomer` 4.19, `crasher` 3.31) |
| Pod Pending 지속 | `kube_pod_status_phase{phase="Pending"} == 1` 10분 | **2개 시리즈 반환**(`pending`, `badimage`) |
| 메모리 limit 임박 | `working_set / limit > 0.9` 5분 | 쿼리 형태만 (이 시점 0.9 초과 없음) |
| CPU 쓰로틀링 의심 | `사용량 / limit > 0.9` 지속 | 쿼리 형태만 |
| 노드 메모리 부족 | `MemAvailable / MemTotal < 0.1` | 쿼리 형태만 |
| API 서버 5xx | 위 5xx 비율 > 1% | 쿼리 형태만 |
| 모니터링 자체 | `up == 0`, `prometheus_tsdb_head_series` 급증 | `up`은 모든 job이 1 |

- **원인(증상의 뿌리)이 아니라 증상(사용자 영향)에 알림**을 거는 것이 일반적이고, CPU 80% 같은 단순 임계치는 알림 피로를 만든다는 점은 문서 지식이다.

#### 3. 추천 대시보드 레이아웃

##### 3-1. 설계 원칙

| 원칙 | 이유 |
|---|---|
| **위에서 아래로: 요약 → 용량 → 상세** | 장애 때 위쪽 한 줄만 봐도 정상/비정상이 판단되어야 한다 |
| 첫 행은 **큰 숫자(Stat) + 색**(초록/노랑/빨강) | 숫자를 읽지 않고 색으로 판단 |
| 행(row)마다 **질문 하나** | 2-1절의 질문 순서. 행 제목에 질문을 쓴다 |
| 패널 제목에 **단위와 해석**을 쓴다 ("near 1 = throttled") | 처음 보는 사람도 읽을 수 있게 |
| **변수(`$namespace`)** 로 범위를 좁힌다 | 패널을 복제하지 않고 같은 대시보드로 드릴다운 |
| 단위(`bytes`, `percentunit`, `s`)와 **임계값 색**을 지정 | 숫자의 의미가 즉시 보인다 |
| **요청 대비/limit 대비 비율**을 함께 | 크기가 다른 워크로드를 한눈에 비교 |
| 한 패널에 시리즈를 너무 많이 넣지 않는다 (Top N, 합계) | 읽을 수 없는 그래프 방지, 카디널리티 부담 |
| 새로고침 30초, 기본 범위 1시간 | 장애 대응 시 "최근" 중심 |

##### 3-2. 대시보드 계층 (드릴다운)

```
[Cluster Overview]  ← 이 장에서 만든 것: 상태 → 용량 → 노드 → 워크로드 → 컨트롤 플레인
      │ 노드 클릭              │ 네임스페이스 클릭         │ 서비스 클릭
      ▼                       ▼                           ▼
[Node 상세]            [Namespace / Workload]        [Application (RED)]
 (USE 전체, 디스크,      (Deployment별 replica,         (요청률, 오류율, p99,
  프로세스, 네트워크)      Pod별 CPU/메모리/재시작)       의존성, 앱 지표)
                              │ Pod 클릭
                              ▼
                        [Pod 상세] → 로그 링크 (Loki 등), 이벤트
```

| 대시보드 | 대상 독자 | 핵심 질문 |
|---|---|---|
| Cluster Overview | 온콜, 인프라 | 지금 깨졌나? 용량은? |
| Node | 인프라 | 이 노드가 포화인가? (USE) |
| Namespace / Workload | 앱 팀 | 우리 서비스의 Pod가 정상인가? 리소스는 적절한가? |
| Pod 상세 | 디버깅 | 이 Pod가 왜 재시작/느린가? |
| Control Plane | 인프라 | API 서버/etcd 건강 |
| Application (RED) | 앱 팀 | 서비스 SLO |

##### 3-3. Cluster Overview 레이아웃 (JSON으로 구현, 총 6행 29패널)

```
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ 변수: [namespace ▾ All]                                      기본 범위 1시간, 새로고침 30초   │
├────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ① Health at a glance (Stat 6개, 높이 4)                                                         │
│ [Nodes Ready][Nodes NotReady][Pods not healthy][Restarts 1h][Containers waiting][API 5xx ratio] │
├────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ② Capacity (높이 8)                                                                              │
│ [CPU req/alloc][Mem req/alloc] [Cluster CPU: usage·requests·allocatable][Cluster memory: ...]   │
├────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ③ Nodes - USE (시계열 4개, 높이 8)                                                              │
│ [CPU utilization][Memory available %][Load per core (saturation)][Network rx/tx]               │
├────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ④ Workloads - $namespace (높이 8 x 4줄)                                                         │
│ [CPU by namespace][Memory by namespace][Pod CPU usage/limit]                                    │
│ [Pod memory/limit][Restarts 15m top10][Table: waiting reason]                                  │
│ [Table: not Ready][Table: OOMKilled][Table: Deployments unavailable]                           │
├────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ⑤ Control plane (시계열 4개, 높이 8)                                                            │
│ [API req/s by code][API p99 by verb][etcd p99][in-flight / workqueue depth]                    │
├────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ⑥ Kubelet [Running pods per node][Pod start p99]                                                │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

| 행 | 질문 | 패널 종류 | 보는 법 |
|---|---|---|---|
| ① Health | 지금 깨졌나 | **Stat** (색 임계) | 빨강/노랑이 있으면 아래 행으로 내려간다 |
| ② Capacity | 모자라나, 낭비되나 | Stat(비율) + 시계열 3선(usage/requests/allocatable) | usage ≪ requests = 낭비, requests ≈ allocatable = Pending 위험 |
| ③ Nodes | 노드가 포화인가 | 시계열 | USE (사용률/포화) |
| ④ Workloads | 누가 문제인가 | 누적 시계열(네임스페이스별), 비율 시계열, **표(Table)** | 표는 **"문제 Pod 목록"** (사유까지) — 숫자만 있는 Stat과 달리 어느 Pod인지 알려 준다 |
| ⑤ Control plane | API 서버/etcd | 시계열 | RED |
| ⑥ Kubelet | 노드 에이전트 | 시계열 | 보조 |

- **Stat은 "있다/없다", Table은 "무엇이 문제인가"** 에 쓴다. 첫 행의 `Pods not healthy = 4`를 보고 ④행의 표(`waiting reason`, `not Ready`, `OOMKilled`)에서 원인을 찾는 흐름이다.
- 시계열에 **세 선(usage, requests, allocatable)** 을 함께 그리면 한 그래프에서 낭비와 부족을 모두 읽을 수 있다.
- 각 패널의 `description`에 해석을 적었다 (예: "usage << requests = over-provisioned").

##### 3-4. 제공하는 대시보드 JSON ([`dashboard-k8s-overview.json`](dashboard-k8s-overview.json))

- 구조: 6개 행, 29개 패널(Stat 8, 시계열 17, 표 4), 변수 `namespace`(`label_values(kube_pod_info, namespace)`, 다중 선택/All), 데이터소스 uid `prom`(Prometheus 하나로 가정, **import 시 데이터소스 uid를 맞춰야 한다**).
- **검증한 것**: Grafana 12.3.1에 `POST /api/dashboards/db`로 import에 **성공**했고, 대시보드의 모든 패널 쿼리 **35개를 Grafana의 쿼리 API(`/api/ds/query`)로 실행해 모두 시계열(frame)을 반환**했다 (오류/빈 결과 0건). 문제 워크로드를 만들어 둔 상태에서 확인한 것이다.
- **검증하지 못한 것**: 화면 렌더링(스크린샷)은 찍지 못했다 (이미지 렌더러를 설치하지 않았다). 패널 배치가 보기 좋은지, 색 임계값이 적절한지는 직접 눈으로 확인하지 않았다. 디스크 용량/PVC 용량 패널은 이 환경에 지표가 없어 넣지 않았다.
- 쿼리는 **Pod 단위 합계**(`container="",pod!=""`)로 작성했다. 일반 클러스터에서는 컨테이너별로 나누려면 `container!=""`와 `by (container)`를 쓰면 된다 (이 환경에서는 컨테이너별 시리즈가 없어 확인하지 못했다).
- 실제 운영 대시보드를 새로 만드는 대신 **커뮤니티에서 검증된 대시보드(예: kube-prometheus-stack에 포함된 대시보드, Grafana 공식 목록)** 를 출발점으로 쓰고 이 장의 질문 순서에 맞게 다듬는 것이 일반적이다 (이 환경에서 그런 대시보드를 가져와 보지는 않았다).

##### 3-5. 체크리스트

| 체크 | 내용 |
|---|---|
| 첫 화면 | 정상/비정상이 3초 안에 판단되는가 (Stat + 색) |
| 비교 | 사용량과 requests/limits를 같이 보는가 |
| 변수 | 네임스페이스/노드/Pod로 좁힐 수 있는가 |
| 문제 목록 | 숫자 옆에 **무엇이** 문제인지 표로 나오는가 |
| 단위/임계 | 단위가 맞고 임계 색이 있는가 |
| 알림 연결 | 대시보드의 빨간 패널이 알림 규칙과 일치하는가 |
| 한계 인지 | 이 환경처럼 **수집되지 않는 지표**(쓰로틀링, PVC, 컨트롤 플레인)를 알고 있는가 |

#### 4. 확인하지 못한 부분

- **다른 런타임/클러스터에서의 지표**: containerd/CRI-O 기반(EKS, kind 등) 클러스터에서 **컨테이너별 cAdvisor 시리즈**, `container_cpu_cfs_throttled_*`(쓰로틀링), `kubelet_volume_stats_*`(PVC 용량), 파일시스템 지표가 어떻게 보이는지는 확인하지 못했다. 이 환경의 결과는 Docker Desktop(cri-dockerd + dockerd) 특성일 수 있다.
- **컨트롤 플레인 메트릭 수집**: scheduler/controller-manager/etcd를 Prometheus가 긁도록 바꾸는 설정(`bind-address`, 인증 토큰, ServiceMonitor)은 시험하지 않았다. 이 환경에서 접근이 막힌 것(127.0.0.1 바인딩)까지만 확인했다. 매니지드 쿠버네티스(EKS 등)의 컨트롤 플레인 메트릭 제공 방식은 문서 지식이다.
- **알림(Alertmanager)과 알림 규칙**: 조건식이 데이터를 반환하는지만 확인했고, 규칙을 로드하거나 알림을 발송해 보지 않았다. `for:` 지속 시간, 라우팅, 억제(inhibit)는 다루지 않았다.
- **kube-prometheus-stack/Prometheus Operator**(ServiceMonitor, PodMonitor CRD, 12장과 연결): 이 장은 단순한 `prometheus` chart를 썼다. 이 PC의 다른 클러스터에 설치된 것은 일부러 건드리지 않았다.
- **로그와 트레이스**(Loki, Tempo, OpenTelemetry), Grafana의 데이터 소스 연결/Explore, 대시보드 변수 체이닝(노드 → Pod)은 다루지 않았다.
- **애플리케이션 메트릭(RED)**: 실제 앱에 Prometheus 클라이언트 라이브러리를 붙여 지표를 노출하고 `prometheus.io/scrape` 어노테이션/PodMonitor로 수집하는 실험은 하지 않았다. 레이아웃의 Application 대시보드는 설계만 적었다.
- **`container_oom_events_total`이 OOM에도 0이었던 이유**: 시리즈는 존재하고 `kube_..._last_terminated_reason{reason="OOMKilled"}`는 1건을 잡았지만 `increase(container_oom_events_total[1h])`는 0건이었다. 원인(카운터 수집 경로, 이 환경의 cAdvisor 한계)을 확인하지 못했다.
- **cardinality 관리**: 시계열 수를 줄이는 방법(`metric_relabel_configs`로 버킷 제거, 보존 기간, 원격 저장소 Thanos/Mimir)과 보존 기간별 디스크/메모리 사용량은 시험하지 않았다. 이 실험의 수치(46,593 시계열, RSS 339MB)는 노드 1개, 보존 2시간, 짧은 실행에서 얻은 값이다.
- **HPA**: CPU 목표 하나만 확인했다. 메모리/사용자 정의 지표(Prometheus Adapter/KEDA), 스케일 다운 안정화 시간, `behavior`는 확인하지 않았다. HPA가 `<unknown>`이던 약 60초와 스케일 아웃에 걸린 약 135초는 1회 관찰값이다.
- **`kubectl top`과 Prometheus 값 차이**: `burner`에서 `top` 301m, Prometheus 1분 rate 0.241코어로 차이가 났다. 측정 시점과 구간(15초 window vs 1분 rate)이 다른 탓으로 추정하고 같은 시점에 비교해 보지는 않았다.
- **Grafana 대시보드 화면 확인**: 스크린샷/렌더링 확인을 하지 못했다 (3-4절).
- **requests/allocatable 보정 쿼리**: Pending Pod 제외 쿼리는 실행해 보지 못했다 (2-2절의 B).
