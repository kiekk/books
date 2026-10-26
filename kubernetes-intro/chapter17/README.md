### 17장. 로깅과 모니터링

> 정리 범위: 쿠버네티스 모니터링 아키텍처 (시스템 메트릭, 서비스 메트릭, 메트릭 서버 등)
>
> 실측 환경: 16장과 같은 **임시 kind 클러스터 `t16`**(v1.35.0, control-plane 1 + worker 1, 별도 kubeconfig)을 이어서 썼다. Helm 차트로 **metrics-server 차트 3.14.0 (앱 v0.9.0)** 을 설치했고(`--kubelet-insecure-tls` 추가, kind의 kubelet 인증서 때문), 네임스페이스는 `t16`이다. 18장 오토스케일링 실험도 같은 클러스터에서 했다. Prometheus, kube-state-metrics, 로그 수집기(EFK, Loki 등)는 설치하지 않았다(`prometheus-community` Helm 저장소가 시간 초과로 열리지 않아 kube-state-metrics 설치도 못 했다).
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다. **이 장은 제목이 "로깅과 모니터링"이지만 요청 범위가 모니터링이어서 로깅(컨테이너 로그, 로그 수집)은 다루지 않았다.**

#### 목차

1. 모니터링 아키텍처 한눈에 보기
2. 시스템 메트릭: kubelet과 cAdvisor
3. 컨트롤 플레인 메트릭
4. 메트릭 서버 (Metrics Server)와 Metrics API
5. 서비스(애플리케이션) 메트릭과 오브젝트 상태 메트릭
6. 리소스 메트릭 파이프라인 vs 전체 메트릭 파이프라인
7. 선택 가이드와 함정
8. 확인하지 못한 부분

---

#### 1. 모니터링 아키텍처 한눈에 보기

```
[노드]  컨테이너 ─(cgroup)─▶ kubelet(cAdvisor 내장)
                                │  /metrics/resource  (CPU/메모리, 경량)
                                │  /metrics/cadvisor  (컨테이너 상세, 3000줄 이상)
                                │  /stats/summary     (JSON 요약)
                                ▼
                    ┌──── Metrics Server ────▶ API 서버에 Aggregated API(metrics.k8s.io)로 등록
                    │                              └─▶ kubectl top, HPA, (VPA)
                    │
[컨트롤 플레인] API 서버 /metrics, scheduler, controller-manager, etcd ─┐
[오브젝트 상태] kube-state-metrics ────────────────────────────────────┼─▶ Prometheus 등 (장기 저장, 알림, 대시보드)
[애플리케이션]  /metrics (서비스 메트릭) ──────────────────────────────┘
```

| 구분 | 무엇을 | 누가 노출 | 누가 소비 |
|---|---|---|---|
| **시스템(리소스) 메트릭** | 노드/컨테이너 CPU, 메모리, 네트워크, 디스크 | kubelet(cAdvisor) | Metrics Server → `kubectl top`, HPA |
| **컨트롤 플레인 메트릭** | API 서버 요청 수/지연, 스케줄러, etcd | 각 컴포넌트의 `/metrics` | Prometheus 등 |
| **오브젝트 상태 메트릭** | Deployment 레플리카 수, Pod phase 같은 K8s 오브젝트 상태 | kube-state-metrics(별도 설치) | Prometheus 등 |
| **서비스(애플리케이션) 메트릭** | 요청 수, 지연, 큐 길이 등 앱 고유 값 | 앱이 `/metrics` 등으로 노출 | Prometheus, 커스텀 메트릭 어댑터 |

---

#### 2. 시스템 메트릭: kubelet과 cAdvisor

**cAdvisor는 kubelet에 내장**돼 있어 노드의 모든 컨테이너 자원 사용량을 cgroup에서 읽는다. 별도 에이전트를 설치하지 않아도 kubelet이 메트릭을 제공한다. API 서버의 노드 프록시(`/api/v1/nodes/<노드>/proxy/…`)로 확인했다(metrics-server를 설치하기 **전**에도 읽혔다).

| 엔드포인트 | 내용 (실측, 노드 `t16-worker`) |
|---|---|
| `/metrics/resource` | **CPU/메모리의 최소 집합**. 약 100줄. `container_cpu_usage_seconds_total{container,namespace,pod}`(누적 CPU 초), 컨테이너/Pod/노드 메모리 등. Metrics Server가 읽는 경로(문서 기준) |
| `/metrics/cadvisor` | **컨테이너 상세 메트릭**. `container_*` 계열만 **3221줄**. cgroup 단위(`id="/kubelet.slice/..."`)와 컨테이너별 값, 전체 노드 합(`id="/"`) 포함 |
| `/metrics` | kubelet 자체 메트릭. 예: `kubelet_node_name{node="t16-worker"} 1`, `kubelet_running_pods 12` |
| `/stats/summary` | JSON 요약. 노드 키 `nodeName, systemContainers, startTime, cpu, memory, io, network, fs`, `pods` 12개. CPU에 `usageNanoCores`, `usageCoreNanoSeconds`와 **PSI(압력 지표)** 도 포함 |

- **CPU 메트릭은 누적 값(counter)** 이다. `container_cpu_usage_seconds_total`의 두 시점 차이를 시간으로 나눠 사용률(코어 수)을 구한다. `burn` Pod(무한 루프)의 값은 10초 간격 두 번 읽어 `14.89 → 31.84`로 변했다(증가분을 10초로 나누면 약 1.7 코어. 같은 때 metrics-server는 `994m`을 보여서 **두 값이 일치하지 않았고** 원인은 확인하지 못했다).
- `/metrics/cadvisor`는 줄 수가 많아 수집 비용이 크다. 자원 사용량만 필요하면 `/metrics/resource`로 충분하다.
- 위 엔드포인트는 kubelet의 보안 포트(10250)에서 인증/인가가 필요하다. 이 실험에서는 API 서버 프록시(관리자 권한)로 접근했고, 노드에서 직접 접근은 하지 않았다.

---

#### 3. 컨트롤 플레인 메트릭

| 대상 | 확인 결과 |
|---|---|
| **API 서버** | `kubectl get --raw /metrics`로 읽힘. `apiserver_*` 계열이 **15,526줄**. 예: `apiserver_request_total{code,group,resource,subresource,verb,version}` 카운터 |
| **scheduler, controller-manager, etcd** | control-plane 노드에서 `127.0.0.1:10259`(scheduler), `127.0.0.1:10257`(controller-manager), `127.0.0.1:2381`(etcd 메트릭 포트)이 **로컬 주소로만** LISTEN 중이었다. `*:10250`은 kubelet. 이 실험에서는 각 `/metrics`를 직접 읽지는 않았다 |

- scheduler와 controller-manager의 메트릭 포트가 **127.0.0.1에만 바인딩**돼 있어(kind/kubeadm 기본 구성) 클러스터 안의 Prometheus가 그대로 긁을 수 없다. 노출하려면 설정(`bind-address`)을 바꾸거나 별도 방법이 필요하다(문서 지식이며 이 실험에서 바꿔 보지 않았다).
- 관리형 서비스(EKS 등)에서는 컨트롤 플레인 메트릭 접근 방식이 다르다(확인 못 함).

---

#### 4. 메트릭 서버 (Metrics Server)와 Metrics API

##### 4-1. 설치 전과 후 (실측)

```
(설치 전)
$ kubectl top nodes             → error: Metrics API not available
$ kubectl get apiservice        → 모두 Local 서비스 (metrics.k8s.io 없음)
(helm install metrics-server)
$ kubectl get apiservice | grep -v Local
v1beta1.metrics.k8s.io   kube-system/ms-metrics-server   True
$ kubectl top nodes
NAME                CPU(cores)   CPU(%)   MEMORY(bytes)   MEMORY(%)
t16-control-plane   165m         1%       1125Mi          14%
t16-worker          54m          0%       542Mi           6%
$ kubectl top pod client --containers      → POD client / NAME c / CPU 0m / MEMORY 0Mi
```

- `kubectl top`은 **metrics-server가 없으면 동작하지 않는다**. 이 "Metrics API"가 `kubectl top`과 HPA의 데이터 원천이다(18장).
- metrics-server의 실행 인자(차트 기본): `--metric-resolution=15s`(수집 주기 15초), `--kubelet-preferred-address-types=InternalIP,ExternalIP,Hostname`, `--kubelet-use-node-status-port`, `--cert-dir=/tmp`, 그리고 이 실험에서 추가한 `--kubelet-insecure-tls`(kubelet 인증서 검증 생략, 운영에는 부적합).

##### 4-2. Aggregated API로 등록된다

```
$ kubectl get apiservice v1beta1.metrics.k8s.io
group: metrics.k8s.io   groupPriorityMinimum: 100   insecureSkipTLSVerify: true
service: {name: ms-metrics-server, namespace: kube-system, port: 443}
$ kubectl api-resources --api-group=metrics.k8s.io
nodes  metrics.k8s.io/v1beta1  false  NodeMetrics
pods   metrics.k8s.io/v1beta1  true   PodMetrics
```

- metrics-server는 **API 서버의 확장(Aggregated API)** 이다. `APIService` 오브젝트가 "`metrics.k8s.io` 그룹의 요청을 이 Service로 보내라"고 API 서버에 알린다. 그래서 `kubectl`은 평범한 API 서버 주소로 요청해도 metrics-server가 응답한다(19장에서 CRD와 비교).
- 읽기 전용 API다(`NodeMetrics`, `PodMetrics`).

##### 4-3. 응답 구조와 수집 주기 (실측)

```
$ kubectl get --raw /apis/metrics.k8s.io/v1beta1/nodes/t16-worker
{"kind":"NodeMetrics", "metadata":{"name":"t16-worker", ...}, "timestamp":"2026-10-26T13:39:33Z", "window":"20.065s", "usage":{...}}
$ kubectl get --raw /apis/metrics.k8s.io/v1beta1/namespaces/t16/pods/client
client  2026-10-26T13:39:13Z  13.351s  [{'name':'c','usage':{'cpu':'0','memory':'348Ki'}}]
```

- 각 값에는 `timestamp`(측정 시각)와 `window`(평균을 낸 구간 길이, 위에서는 13~20초)가 붙는다. **순간값이 아니라 최근 구간의 평균**이다.
- 메트릭은 **현재 값 하나뿐**이다. 과거 이력(시계열)은 저장하지 않는다(메모리에만 보관, 문서 기준. 저장소 여부는 실험에서 확인하지 않았다).

##### 4-4. 지연 (실측)

| 상황 | 결과 |
|---|---|
| 새 Pod `fresh` 생성 직후 `kubectl top pod fresh` | `podmetrics.metrics.k8s.io "t16/fresh" not found` |
| 약 20초 뒤 | `fresh 4m 9Mi` |
| 무한 루프 Pod `burn` 생성 후 `kubectl top` | 약 5초 간격으로 확인했을 때 **26초 지나서야 `994m`** 이 처음 보임 (그 전까지는 `<none>`) |

- 새로 뜬 Pod는 수집 주기(15초)와 CPU 계산에 필요한 샘플 2개 때문에 **약 20~30초 동안 메트릭이 없다**. HPA 반응이 느린 이유 중 하나다(18장).

---

#### 5. 서비스(애플리케이션) 메트릭과 오브젝트 상태 메트릭

이 부분은 이번 실험으로 직접 검증하지 못했다(Prometheus, kube-state-metrics 미설치). 개념만 정리한다(문서/일반 지식).

- **서비스 메트릭**: 앱이 직접 `/metrics`(Prometheus 형식 등)로 요청 수, 지연, 에러율, 큐 길이 같은 **비즈니스/앱 고유 지표**를 노출한다. 수집기(Prometheus)가 주기적으로 긁어(pull) 저장한다. 쿠버네티스는 이 값을 직접 모으지 않는다.
- **오브젝트 상태 메트릭(kube-state-metrics)**: Deployment의 원하는/현재 레플리카 수, Pod phase, 노드 컨디션 같은 **쿠버네티스 오브젝트의 상태**를 메트릭으로 변환한다. kubelet(cAdvisor)의 자원 사용량과는 다른 종류의 정보다.
- 두 종류를 합쳐야 "CPU는 낮은데 에러가 증가" 같은 상황을 볼 수 있다.

---

#### 6. 리소스 메트릭 파이프라인 vs 전체 메트릭 파이프라인

| | 리소스 메트릭 파이프라인 | 전체(커스텀) 메트릭 파이프라인 |
|---|---|---|
| 구성 | kubelet(cAdvisor) → **Metrics Server** → `metrics.k8s.io` | kubelet/컨트롤 플레인/앱/kube-state-metrics → **Prometheus 등** |
| 저장 | 최신 값만, 메모리 | 시계열 장기 저장 |
| 용도 | `kubectl top`, **HPA(CPU/메모리)**, 스케줄링 보조 | 대시보드, 알림, 용량 계획, 커스텀 HPA |
| 지표 | CPU, 메모리만 | 임의의 지표 |
| 쿠버네티스 연동 | `metrics.k8s.io` API | `custom.metrics.k8s.io`, `external.metrics.k8s.io` API를 **어댑터**(Prometheus Adapter 등)가 제공해야 HPA에서 사용 가능 (문서 지식, 이번에 설치하지 않음) |

- Metrics Server는 **"HPA와 `kubectl top`용 최소 도구"** 이지 모니터링 시스템이 아니다. 알림, 이력, 앱 지표는 따로 구성해야 한다.

---

#### 7. 선택 가이드와 함정

| 질문 | 답 |
|---|---|
| `kubectl top`이 안 된다 | metrics-server 설치/`APIService` 상태(`AVAILABLE True`) 확인 |
| 값이 이상하게 낮다/비어 있다 | 새 Pod는 20~30초 지연, `window` 평균값 |
| kind/Docker Desktop에서 metrics-server가 안 뜬다 | kubelet 인증서 문제로 `--kubelet-insecure-tls`가 필요할 수 있음(이 실험의 kind에서 사용, 운영에서는 인증서를 정상 구성) |
| CPU 사용률의 의미 | `kubectl top`은 현재 사용량(m 단위, 1000m = 1코어). `CPU(%)`는 노드 할당량 대비. HPA의 사용률은 **요청(requests) 대비**(18장) |
| 장기 이력/알림이 필요하다 | Prometheus 등 별도 구성 |
| 앱 지표로 오토스케일링 | 커스텀 메트릭 파이프라인 필요 |

---

#### 8. 확인하지 못한 부분

- **로깅**: 컨테이너 로그(`kubectl logs`의 동작, 로그 로테이션, 노드 레벨 수집), 중앙 로그 수집(Fluent Bit, EFK, Loki)은 실험하지 않았다.
- **Prometheus 구성**(서비스 디스커버리, ServiceMonitor, scrape 설정), **kube-state-metrics**, 알림(Alertmanager), Grafana 대시보드는 설치하지 못했다. 5절과 6절의 해당 부분은 문서/일반 지식이다.
- **커스텀/외부 메트릭 API**(`custom.metrics.k8s.io`, `external.metrics.k8s.io`)와 Prometheus Adapter, KEDA는 실험하지 않았다.
- **scheduler, controller-manager, etcd의 `/metrics`** 를 직접 읽지는 않았다(포트만 확인).
- **metrics-server의 자체 메트릭** 확인은 프록시 경로 시도에서 출력을 얻지 못했다.
- **`/metrics/resource`와 `kubectl top` 값 불일치**(burn Pod 1.7코어 vs 994m)의 원인은 확인하지 못했다.
- **Aggregated API 인증 구조**(요청 인증 전달, 프론트 프록시 클라이언트 인증서)와 `insecureSkipTLSVerify`의 영향은 19장 범위에서도 깊게 다루지 못했다.
- **메트릭 서버의 고가용성(replicas 2), 대규모 클러스터에서의 메모리 사용량**, `--metric-resolution` 변경 효과는 시험하지 않았다.
- 클라우드 환경(EKS의 CloudWatch Container Insights, 관리형 Prometheus)과 **노드/컨트롤 플레인 메트릭 포트의 접근 제어**는 확인하지 못했다.
