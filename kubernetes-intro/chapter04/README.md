### 4장. 쿠버네티스 아키텍처

> 정리 범위: 클러스터 기본 구성(마스터, 노드), 주요 컴포넌트(마스터용, 노드용, 애드온), 네임스페이스 개념
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), **Docker Desktop 내장 쿠버네티스 v1.32.2**(단일 노드 `docker-desktop`)와, 마스터/워커 구분을 보려고 만든 **임시 kind 클러스터 `t4arch`**(v1.35.0, control-plane 1 + worker 2, 실험 후 삭제). kind는 별도 kubeconfig 파일을 써서 이 PC의 kubeconfig는 건드리지 않았다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 Docker Desktop 실험은 모두 `--context docker-desktop`을 명시했다 (`k`로 줄여 적음). `kc`는 임시 kind 클러스터용이다. 이 PC의 다른 kind 클러스터는 건드리지 않았다.
>
> 다른 책 `시작하세요! 도커/쿠버네티스`와 겹치는 부분(컴포넌트 역할 [6장](../../start-docker-kubernetes/chapter06/README.md), 네임스페이스/ResourceQuota [7장](../../start-docker-kubernetes/chapter07/README.md))은 짧게 적고, 이 장에서는 **마스터와 노드의 차이, 컴포넌트 하나가 멈췄을 때 일어나는 일, 네임스페이스 삭제가 멈추는 경우**를 실측으로 보강했다.

```bash
k()  { kubectl --context docker-desktop "$@"; }
kc() { kubectl --kubeconfig kind.conf "$@"; }       # 임시 3노드 kind 클러스터
```

#### 목차

1. 클러스터 기본 구성: 마스터와 노드
2. 주요 컴포넌트: 마스터용, 노드용, 애드온
3. 네임스페이스
4. 확인하지 못한 부분

---

#### 1. 클러스터 기본 구성: 마스터와 노드

##### 1-1. 개념

쿠버네티스 클러스터는 **컨테이너를 어디에 어떻게 실행할지 결정하는 쪽**과 **실제로 컨테이너를 실행하는 쪽**으로 나뉜다.

| 구분 | 역할 | 부르는 이름 |
|---|---|---|
| **마스터** | 클러스터의 두뇌. 상태를 저장하고 스케줄링과 제어 루프를 돌린다 | **컨트롤 플레인(control plane)**, 마스터 노드, 컨트롤 플레인 노드 |
| **노드** | 실제 Pod(컨테이너)를 실행하는 일꾼 | **워커 노드(worker node)**, 노드, 미니언 |

- 현재 공식 용어는 **control plane**이다. 책의 "마스터"와 같은 것이다. 노드의 `ROLES` 열에는 `control-plane`이라고 나온다.
- 모든 노드(마스터 포함)에는 **kubelet(노드 에이전트)** 이 있고, 마스터 전용 컴포넌트(API 서버, etcd, 스케줄러, 컨트롤러 매니저)가 더해진 노드가 마스터다.

```
                      ┌────────────── 마스터 (control plane) ──────────────┐
  kubectl ──────────▶ │  kube-apiserver ◀──▶ etcd                          │
                      │       ▲      ▲                                      │
                      │       │      └─ kube-scheduler, kube-controller-manager
                      └───────┼──────────────────────────────────────────────┘
                              │ (모든 컴포넌트는 API 서버를 통해서만 통신)
              ┌───────────────┼───────────────┐
        ┌─────┴─────┐   ┌─────┴─────┐   ┌─────┴─────┐
        │ 노드 1     │   │ 노드 2     │   │ 노드 N     │
        │ kubelet    │   │ kubelet    │   │ kubelet    │
        │ kube-proxy │   │ kube-proxy │   │ kube-proxy │
        │ 컨테이너런타임│   │ 컨테이너런타임│   │ 컨테이너런타임│
        │  [Pod][Pod]│   │  [Pod][Pod]│   │  [Pod][Pod]│
        └───────────┘   └───────────┘   └───────────┘
```

##### 1-2. 이 두 클러스터의 모습 (실측)

**Docker Desktop: 노드 하나가 마스터이자 워커**

```
$ k get node docker-desktop -o wide
docker-desktop  Ready  control-plane  v1.32.2  192.168.65.3  Docker Desktop  6.10.14-linuxkit  docker://28.0.4
$ k get node docker-desktop -o jsonpath='taints={.spec.taints}'
taints=                                          ← taint 없음 (마스터인데 일반 Pod도 실행됨)
$ k get node docker-desktop --show-labels | tr ',' '\n' | grep role
node-role.kubernetes.io/control-plane=           ← 역할 라벨은 있음
```

- 역할은 **노드 라벨 `node-role.kubernetes.io/control-plane`** 으로 표시된다 (`ROLES` 열이 이 라벨에서 나온다).
- 일반적인 클러스터에서는 마스터에 **taint**를 걸어 일반 Pod가 오지 못하게 하지만, 이 단일 노드 환경은 taint가 없어서 마스터가 워커 역할을 겸한다 (그래서 6~14장의 모든 실험이 이 노드에서 돌았다).

**kind 3노드: 마스터와 워커가 분리된 모습**

```
$ kc get nodes -o wide
t4arch-control-plane   Ready   control-plane   192.168.16.3
t4arch-worker          Ready   <none>          192.168.16.4
t4arch-worker2         Ready   <none>          192.168.16.2
$ kc get node t4arch-control-plane -o jsonpath='{.spec.taints}'
[{"effect":"NoSchedule","key":"node-role.kubernetes.io/control-plane"}]     ← 마스터에는 NoSchedule taint
$ kc get node t4arch-worker -o jsonpath='{.spec.taints}'                    →  (없음)
```

**마스터와 워커의 차이: 어떤 프로세스가 돌고 있나**

```
$ docker exec <노드 컨테이너> ps -eo comm | grep -E '^(kubelet|kube-apiserver|kube-scheduler|kube-controller|etcd|containerd|kube-proxy|kindnetd|coredns)'
t4arch-control-plane  containerd(1) containerd-shim(9) coredns(2) etcd(1) kindnetd(1) kube-apiserver(1) kube-controller(1) kube-proxy(1) kube-scheduler(1) kubelet(1)
t4arch-worker         containerd(1) containerd-shim(2) kindnetd(1) kube-proxy(1) kubelet(1)
$ kc -n kube-system get pods -o wide        (노드별)
t4arch-control-plane: coredns coredns etcd kindnet kube-proxy kube-apiserver kube-controller-manager kube-scheduler
t4arch-worker:        kindnet kube-proxy
t4arch-worker2:       kindnet kube-proxy
$ docker exec <노드> ls /etc/kubernetes/manifests
t4arch-control-plane  etcd.yaml kube-apiserver.yaml kube-controller-manager.yaml kube-scheduler.yaml
t4arch-worker         (비어 있음)
$ docker exec t4arch-worker grep server: /etc/kubernetes/kubelet.conf
    server: https://t4arch-control-plane:6443               ← 워커의 kubelet이 마스터의 API 서버에 접속
```

| | 마스터 (control plane) | 워커 |
|---|---|---|
| kubelet, containerd(런타임), kube-proxy, CNI 에이전트(kindnet) | 있음 | 있음 |
| kube-apiserver, etcd, scheduler, controller-manager | **있음** (스태틱 Pod) | 없음 |
| 스태틱 Pod 매니페스트 (`/etc/kubernetes/manifests`) | 4개 파일 | 비어 있음 |
| 일반 Pod 스케줄 | taint 때문에 기본은 안 감 (CoreDNS는 톨러레이션이 있어 마스터에서 돌고 있었다) | 감 |
| API 서버 접속 방식 | 자기 자신(로컬) | `kubelet.conf`의 `server`로 마스터에 접속 |

- **스태틱 Pod**: kubelet이 API 서버 없이 노드의 `/etc/kubernetes/manifests` 디렉터리의 파일만 보고 직접 띄우는 Pod다. 마스터 컴포넌트들이 이 방식으로 뜬다 (아래 1-4절 실험이 이 성질을 이용했다).
- **노드 합류**: 워커가 클러스터에 들어올 때는 kubelet이 마스터의 API 서버에 접속해서 Node 오브젝트를 등록한다 (kubeadm의 `join`이 이 설정을 만든다는 것은 문서 지식이고 이번에 직접 확인한 것은 `kubelet.conf`의 `server`다).

##### 1-3. Node 오브젝트가 알려 주는 것

```
$ k get node docker-desktop -o jsonpath=...
Conditions : MemoryPressure=False DiskPressure=False PIDPressure=False Ready=True
capacity   : 12 cpu / 8025148Ki     allocatable: 12 cpu / 7922748Ki, pods=110
addresses  : InternalIP=192.168.65.3  Hostname=docker-desktop
nodeInfo   : kubelet=v1.32.2 runtime=docker://28.0.4 os=Docker Desktop kernel=6.10.14-linuxkit kubeProxy=v1.32.2
kubelet API port: 10250
```

| 항목 | 의미 |
|---|---|
| `conditions` | `Ready`(정상), `MemoryPressure`/`DiskPressure`/`PIDPressure`(자원 압박) — 스케줄러/퇴출 판단 근거 |
| `capacity` vs `allocatable` | 노드 전체 자원 vs **Pod에 배정 가능한 양** (시스템 예약을 뺀 값. 11장의 requests 합계 기준) |
| `addresses` | 노드 IP, 호스트명 |
| `nodeInfo` | kubelet, 런타임, OS, 커널 버전 (여기서 `docker://28.0.4`: 런타임이 도커) |
| `daemonEndpoints` | kubelet API 포트(10250) |

**노드 하트비트: Lease**

```
$ k -n kube-node-lease get lease docker-desktop -o jsonpath='{.spec.holderIdentity} {.spec.renewTime} duration={.spec.leaseDurationSeconds}'
docker-desktop  2026-10-25T10:37:10Z   duration=40
(12초 뒤)  renewTime: 2026-10-25T10:37:20Z                   ← 약 10초마다 갱신
```

- kubelet이 **약 10초마다 Lease(`kube-node-lease` 네임스페이스)를 갱신**해서 "살아 있다"고 알린다. 노드 컨트롤러가 이 갱신이 끊기는지를 본다 (1-4절의 장애 실험).

##### 1-4. 컴포넌트가 하나 멈추면 무슨 일이 생기나 (kind 3노드 실측)

마스터 컴포넌트의 스태틱 Pod 매니페스트를 `/etc/kubernetes/manifests`에서 치워서(kubelet이 Pod를 내린다) 하나씩 멈췄다가 복구했다. `app` Deployment(Pod 2개) + `cli` Pod를 두고 실험했다.

| 멈춘 것 | 관찰 (실측) | 의미 |
|---|---|---|
| **kube-scheduler** | 새 Pod `new1`이 **`Pending`, `nodeName` 비어 있음**. 이벤트도 **없음**(`No resources found`). 기존 Pod는 정상 (`svc http=200`). 매니페스트를 되돌리고 30초 뒤 `new1`이 `Running`, 노드 `t4arch-worker` | 스케줄러가 없으면 **새 Pod는 노드를 배정받지 못한다.** `FailedScheduling` 같은 이벤트도 스케줄러가 남기므로 아무 이벤트가 없다 (스케줄러가 있는데 자원이 모자라 Pending인 11장의 경우와 구분됨) |
| **kube-controller-manager** | Deployment를 2 → 4로 늘렸는데 **`READY=2/4`, ReplicaSet의 `desired=2`** 그대로 (Pod가 안 늘어남). Pod 하나를 삭제해도 **대체 Pod가 안 생겨 1개만 남음**. 되돌리고 35초 뒤 `READY=4/4` | 컨트롤러 매니저가 없으면 **제어 루프가 멈춘다**: Deployment → ReplicaSet → Pod 생성, 자가 치유, 롤아웃이 모두 멈춘다. 이미 떠 있는 Pod는 계속 돈다 |
| **kube-apiserver** | `kubectl get pods` → `Unable to connect to the server: EOF`. 그런데 **워커의 컨테이너는 계속 Running**이고 Pod IP로 직접 `curl` → `http=200`. 워커의 kubelet은 `active`이고 로그에 `Failed to update lease ... https://t4arch-control-plane:6443/...` 오류를 반복. API 서버를 되돌리자 약 10초 뒤 `kubectl`이 다시 동작하고 모든 Pod가 `Running` | **API 서버가 죽어도 이미 실행 중인 워크로드(데이터 플레인)는 계속 돈다.** 하지만 조회/변경/스케줄/자가 치유가 모두 불가능해진다 (**컨트롤 플레인과 데이터 플레인의 분리**, 도커 스웜 3장의 "매니저가 죽어도 서비스는 계속"과 같은 성질) |
| **kubelet (워커)** | 워커 `t4arch-worker2`에서 `systemctl stop kubelet` → **약 46초 뒤 `NotReady`** (`Ready=Unknown (NodeStatusUnknown)`). taint `node.kubernetes.io/unreachable:NoSchedule`와 `:NoExecute`가 자동으로 붙음. API 기준 그 노드의 Pod(`app-...`, `cli`)는 **계속 `Running`으로 표시**됨. 노드의 컨테이너 런타임에서는 컨테이너가 실제로 계속 돌고 있음. kubelet을 다시 켜자 `Ready`로 돌아오고 taint 사라짐 | 노드 컨트롤러가 **Lease 갱신이 끊기고 약 40초 이상 지나면** 노드를 `NotReady`/`Unknown`으로 표시하고 taint를 건다. 컨테이너는 kubelet 없이도 런타임이 유지한다 |

```
$ kc get node t4arch-worker2 (kubelet 정지 후)
  0s  node=Ready     ...
 41s  node=Ready     Ready=True(KubeletReady)
 46s  node=NotReady  Ready=Unknown(NodeStatusUnknown) taints=node.kubernetes.io/unreachable node.kubernetes.io/unreachable
$ kc get node t4arch-worker2 -o jsonpath='{range .spec.taints[*]}{.key}:{.effect} {end}'
node.kubernetes.io/unreachable:NoSchedule node.kubernetes.io/unreachable:NoExecute
$ kc -n arch get pod -l app=app -o jsonpath='{.items[0].spec.tolerations}'
[{"effect":"NoExecute","key":"node.kubernetes.io/not-ready","operator":"Exists","tolerationSeconds":300},
 {"effect":"NoExecute","key":"node.kubernetes.io/unreachable","operator":"Exists","tolerationSeconds":300}]
```

- **NotReady 판정까지 약 46초** (5초 간격 폴링이라 오차가 있다). 문서상 노드 컨트롤러의 `node-monitor-grace-period`(기본 40초)와 일치한다.
- 모든 Pod에는 어드미션이 **`tolerationSeconds: 300`의 톨러레이션**(`not-ready`, `unreachable`)을 자동으로 넣어 준다 (도커 책 10장에서 본 어드미션의 기본값 주입). 그래서 노드가 죽어도 **5분(300초) 동안은 Pod를 다른 노드로 옮기지 않는다.** 이 5분 안에 노드가 돌아오면 Pod가 그대로 유지된다. 이번 실험에서는 5분이 지나 Pod가 퇴출되는 것까지는 기다리지 않았다.
- 노드가 `NotReady`인데 그 노드의 Pod가 API에서 `Running`으로 보이는 것은 **kubelet이 상태를 보고하지 못해서 API 서버가 마지막 상태를 갖고 있기 때문**이다. `Running` 표시를 그대로 믿으면 안 된다.

##### 1-5. 마스터를 여러 대로 (고가용성)

- 운영에서는 컨트롤 플레인을 보통 **3대(홀수)** 로 두고 API 서버를 로드밸런서 뒤에 둔다. etcd가 Raft 합의를 쓰므로 홀수가 권장된다 (도커 책 3장의 Raft, 11장의 etcd).
- 이 장의 두 클러스터는 컨트롤 플레인이 1대라서 **이중화 동작은 확인하지 못했다.** 스케줄러/컨트롤러 매니저는 여러 개를 띄우면 리더 한 개만 일하는 **리더 선출**을 쓴다 (아래 2-4절의 Lease).

#### 2. 주요 컴포넌트: 마스터용, 노드용, 애드온

##### 2-1. 한눈에 정리

| 구분 | 컴포넌트 | 역할 |
|---|---|---|
| **마스터** | **kube-apiserver** | 모든 요청의 입구(REST API). 인증 → 인가 → 어드미션 → etcd. **etcd와 직접 통신하는 유일한 컴포넌트** |
| | **etcd** | 클러스터 상태 저장소 (키-값, Raft) |
| | **kube-scheduler** | 노드가 없는 Pod에 노드를 배정 (필터링 → 스코어링, 도커 책 11장) |
| | **kube-controller-manager** | 여러 컨트롤러(노드, 레플리카셋, 디플로이먼트, 엔드포인트, 서비스어카운트...)를 한 프로세스로 실행하는 제어 루프 |
| | cloud-controller-manager | 클라우드(AWS 등)와 연동 (로드 밸런서, 노드 정보, 라우트). **클라우드에서만** 있다 |
| **노드** | **kubelet** | 노드 에이전트. 배정된 Pod를 컨테이너 런타임으로 실행하고 상태를 보고 (프로브 실행, 볼륨 마운트, Lease 갱신) |
| | **kube-proxy** | Service의 가상 IP를 Pod IP로 연결하는 규칙(iptables 등)을 노드에 설정 |
| | **컨테이너 런타임** | 컨테이너를 실행 (containerd, CRI-O, 도커+cri-dockerd) |
| **애드온** | **CoreDNS** | 클러스터 DNS (서비스 이름 해석) |
| | **CNI 플러그인** (kindnet, Calico, Flannel, Cilium 등) | Pod 네트워크(Pod IP, Pod 간 통신) |
| | 대시보드, **metrics-server**, 인그레스 컨트롤러(8장), 로깅/모니터링 에이전트(17장) 등 | 필수는 아니지만 클러스터에 붙이는 부가 기능 |

- **애드온**은 쿠버네티스 핵심 컴포넌트는 아니지만 클러스터가 제대로 동작하려면 사실상 필요한 부가 구성요소다. 일반 워크로드처럼 Deployment/DaemonSet으로 배포한다.

##### 2-2. 컴포넌트가 어떻게 배포되어 있나 (실측)

**Docker Desktop 클러스터**

```
$ k -n kube-system get pods -o custom-columns=NAME,OWNER,HOSTNET,PRIORITYCLASS
coredns-668d6bf9bc-c74pw                 ReplicaSet   <none>  system-cluster-critical     ← 애드온: Deployment
coredns-668d6bf9bc-kx275                 ReplicaSet   <none>  system-cluster-critical
etcd-docker-desktop                      Node         true    system-node-critical        ← 마스터: 스태틱 Pod (owner=Node)
kube-apiserver-docker-desktop            Node         true    system-node-critical
kube-controller-manager-docker-desktop   Node         true    system-node-critical
kube-proxy-w54n4                         DaemonSet    true    system-node-critical        ← 노드마다: DaemonSet
kube-scheduler-docker-desktop            Node         true    system-node-critical
storage-provisioner                      <none>       <none>  <none>                      ← 9장의 동적 프로비저너 (애드온, 소유자 없는 Pod)
vpnkit-controller                        <none>       <none>  <none>                      ← Docker Desktop 전용 구성요소
```

| 컴포넌트 | 배포 방식 | `hostNetwork` | 설명 |
|---|---|---|---|
| etcd, apiserver, controller-manager, scheduler | **스태틱 Pod** (`ownerReferences`가 `Node`) | true | kubelet이 직접 관리. 노드 네트워크를 그대로 사용 |
| kube-proxy | **DaemonSet** | true, privileged | 노드마다 하나. 톨러레이션 `Exists`로 모든 taint를 견딤 (13장 DaemonSet) |
| CoreDNS | **Deployment** (replicas 2) | 아님 | 일반 앱처럼 배포. 클러스터 필수 애드온이라 `system-cluster-critical` |
| **kubelet** | **Pod가 아니라 노드의 프로세스** | - | VM 안에서 직접 실행. API 서버가 죽어도 컨테이너를 유지해야 하므로 Pod일 수 없다 |
| 컨테이너 런타임 | 노드의 프로세스 | - | 이 환경: `cri-dockerd` → `dockerd` → `containerd`. kind: `containerd` |

- 마스터 컴포넌트와 kube-proxy는 `system-node-critical`, CoreDNS는 `system-cluster-critical` 우선순위 클래스를 가진다. 노드 자원이 부족할 때 이 Pod들은 마지막에 퇴출/선점된다 (11장 QoS와 별개의 우선순위).
- **kubelet이 Pod가 아닌 이유**: 다른 모든 컴포넌트를 Pod로 띄우는 주체가 kubelet이므로 kubelet은 먼저 떠 있어야 한다 (닭과 달걀). 위 1-4절에서 kubelet이 멈추면 노드가 `NotReady`가 된 것도 같은 맥락이다.
- kind에서는 kube-system에 `DaemonSet` 소유 Pod가 6개(kube-proxy 3개 + kindnet 3개), `Node` 소유(스태틱) 4개, `ReplicaSet` 소유(CoreDNS) 2개였다. **CNI 에이전트(kindnet)와 kube-proxy는 모든 노드에** 있고(DaemonSet), 마스터 컴포넌트는 마스터에만 있다.

**CoreDNS 애드온의 구성**

```
$ k -n kube-system get svc kube-dns -o jsonpath='clusterIP={.spec.clusterIP} ports={.spec.ports[*].port}'
clusterIP=10.96.0.10 ports=53 53 9153                                     ← Pod의 /etc/resolv.conf의 네임서버 (UDP/TCP 53, 9153은 메트릭)
$ k -n kube-system get cm coredns -o jsonpath='{.data.Corefile}'
.:53 {
    errors
    health { lameduck 5s }
    ready
    kubernetes cluster.local in-addr.arpa ip6.arpa { pods insecure; fallthrough in-addr.arpa ip6.arpa; ttl 30 }
    prometheus :9153
    forward . /etc/resolv.conf { max_concurrent 1000 }
    cache 30 { disable success cluster.local; disable denial cluster.local }
    loop
    reload
    loadbalance
}
```

- 설정이 **ConfigMap(`coredns`)** 이라서 `kubectl`로 바꿀 수 있다 (7장). 플러그인 구성: `kubernetes`(클러스터 도메인 `cluster.local` 해석, 서비스 TTL 30초), `forward`(그 외 도메인은 노드의 `/etc/resolv.conf` 네임서버로), `cache`(외부 도메인 캐시, **클러스터 내부 이름은 캐시하지 않음**), `prometheus :9153`(메트릭, 14장).
- 서비스/Pod의 DNS 이름 형식(`<서비스>.<네임스페이스>.svc.cluster.local`)은 이 `kubernetes` 플러그인이 만든다.

##### 2-3. 컴포넌트의 포트와 상태 확인 (Docker Desktop 노드에서 `ss -ltnp`)

| 컴포넌트 | 포트 | 바인딩 | 용도 |
|---|---|---|---|
| kube-apiserver | **6443** | 모든 인터페이스 | API (HTTPS) |
| etcd | **2379** (클라이언트), **2380** (피어), 2381 (메트릭) | 2379는 127.0.0.1과 노드 IP, 2380은 노드 IP, 2381은 127.0.0.1 | API 서버가 접속, 다른 etcd 멤버와 통신, 메트릭 |
| **kubelet** | **10250** | 모든 인터페이스 | kubelet API (API 서버가 `logs`/`exec`/`port-forward` 요청을 이쪽으로 보낸다) |
| kubelet | 10248 | 127.0.0.1 | 헬스 체크 (`/healthz` → `ok`) |
| kube-proxy | 10256 | 모든 인터페이스 | 헬스 체크 (`/healthz`, `/livez`가 JSON으로 응답) |
| kube-proxy | 10249 | 127.0.0.1 | 메트릭 |
| kube-controller-manager | 10257 | **127.0.0.1** | 보안 포트(HTTPS) |
| kube-scheduler | 10259 | **127.0.0.1** | 보안 포트(HTTPS) |
| cri-dockerd, dockerd | 임의 포트 / 로컬 | | 런타임 계층 |

```
$ k get --raw '/livez?verbose' | tail -3
[+]poststarthook/apiservice-openapiv3-controller ok     livez check passed
$ k get --raw /version      →  apiserver v1.32.2 linux/arm64
```

- 컨트롤러 매니저와 스케줄러는 **`127.0.0.1`에만 바인딩**되어 클러스터 안의 다른 Pod에서는 접근할 수 없다 (14장에서 이것 때문에 메트릭 수집이 안 되는 것을 확인).
- `kubectl logs`가 동작하는 흐름: kubectl → API 서버 → **노드의 kubelet(10250)** → 컨테이너 런타임. 로그/exec 같은 요청은 API 서버가 노드로 **되돌아가서 가져오는** 방향이다 (이 흐름은 설정/포트로 확인했고 패킷을 따라가 보지는 않았다).
- 헬스 엔드포인트: API 서버 `/livez`, `/readyz` (`?verbose`로 항목별), kubelet `:10248/healthz`, kube-proxy `:10256/healthz`.

##### 2-4. 컴포넌트 간 통신과 리더 선출

```
$ k -n kube-system get lease
apiserver-457xdk...   holder=apiserver-457xdk..._fbb3f596-...      ← API 서버 인스턴스 정체성
kube-scheduler        holder=docker-desktop_89c92f66-...            ← 스케줄러 리더
$ k -n kube-node-lease get lease      →  docker-desktop (노드 하트비트)
```

- **모든 컴포넌트는 API 서버를 통해서만 통신**한다. 스케줄러, 컨트롤러, kubelet 모두 API 서버를 watch/요청하고, etcd는 API 서버만 만난다 (도커 책 11장에서 스케줄러 설정과 etcd 연결 옵션으로 확인).
- **리더 선출(Lease)**: 스케줄러와 컨트롤러 매니저가 여러 개일 때 `kube-system`의 Lease를 먼저 잡은 쪽이 일한다. 위에서 `kube-scheduler` Lease의 `holderIdentity`가 현재 리더다. **`kube-controller-manager` Lease가 없는 것**은 이 환경의 컨트롤러 매니저가 `--leader-elect=false`로 떠 있기 때문이다 (도커 책 6장에서 확인한 옵션).
- 네 종류의 흐름: ① 사용자 → API 서버 (kubectl) ② 컨트롤러/스케줄러 → API 서버 (watch) ③ kubelet → API 서버 (노드 상태, Pod 상태 보고, Lease) ④ API 서버 → kubelet (logs/exec/port-forward).

##### 2-5. 노드가 늘어날 때 컴포넌트가 따라 붙는 방식

| 컴포넌트 | 새 노드에서 | 방식 |
|---|---|---|
| kubelet, 컨테이너 런타임 | 노드를 만들 때 직접 설치 | OS 패키지/프로세스 |
| kube-proxy | **자동으로 Pod가 생김** | DaemonSet |
| CNI 에이전트 | **자동으로 Pod가 생김** | DaemonSet |
| node-exporter 등 모니터링 에이전트 | **자동** | DaemonSet (도커 책 14장) |
| CoreDNS, 인그레스 컨트롤러 | 노드 수와 무관 | Deployment |

#### 3. 네임스페이스

##### 3-1. 개념

**하나의 물리 클러스터를 논리적으로 나누는 칸막이.** 이름의 유일성 범위, 권한(RBAC), 자원 쿼터를 네임스페이스 단위로 관리한다 (도커 책 7장: 같은 이름 공존, ResourceQuota/LimitRange, 네트워크 격리는 아님, 삭제하면 안의 리소스도 삭제).

##### 3-2. 기본 네임스페이스 (실측)

```
$ k get ns
default          Active   129d
kube-node-lease  Active   129d
kube-public      Active   129d
kube-system      Active   129d
```

| 네임스페이스 | 용도 | 안에 있는 것 (이 클러스터) |
|---|---|---|
| `default` | 지정하지 않으면 들어가는 곳 | `kubernetes` Service, `kube-root-ca.crt` ConfigMap, `default` ServiceAccount |
| **`kube-system`** | **쿠버네티스 시스템 컴포넌트 전용** | Pod 9개, Deployment(CoreDNS), DaemonSet(kube-proxy), ConfigMap 7개, Lease 2개, ServiceAccount 38개 |
| **`kube-public`** | **인증 없이 읽을 수 있는 공개 정보** | ConfigMap 2개 (`cluster-info` 등) |
| **`kube-node-lease`** | 노드 하트비트(Lease) | `docker-desktop` Lease 1개 |

**kube-public은 정말 인증 없이 읽힌다**

```
$ curl -sk https://127.0.0.1:6443/api/v1/namespaces/kube-public/configmaps/cluster-info -o /dev/null -w "%{http_code}"
200                                                     ← 인증 정보 없이 조회 성공
$ curl -sk https://127.0.0.1:6443/api/v1/namespaces/kube-system/configmaps -o /dev/null -w "%{http_code}"
403                                                     ← kube-system은 거부
$ k -n kube-public get cm cluster-info -o jsonpath='{.data}' | head -c 100
{"kubeconfig":"apiVersion: v1\nclusters:\n- cluster:\n    certificate-authority-data: LS0tLS1C...
```

- `cluster-info` ConfigMap에는 **API 서버 주소와 CA 인증서가 담긴 kubeconfig 조각**이 있다. 새 노드가 클러스터에 합류(join)할 때 인증 정보를 가지기 전에 이 공개 정보로 API 서버를 확인하는 용도라고 알려져 있다 (문서 지식이고 join 과정은 시험하지 않았다. 그래서 인증 없이 읽을 수 있게 열려 있다. 도커 책 10장의 `익명 사용자 403`과 대비된다). 민감한 정보를 `kube-public`에 넣으면 안 된다.
- `kube-system`에는 시스템 컴포넌트가 있으므로 **일반 앱을 배포하지 않고** 권한도 엄격하게 제한한다.

##### 3-3. 네임스페이스에 속하는 리소스와 속하지 않는 리소스

```
$ echo "namespaced: $(k api-resources --namespaced=true --no-headers | wc -l)  cluster-scoped: $(k api-resources --namespaced=false --no-headers | wc -l)"
namespaced: 31  cluster-scoped: 27
```

- 네임스페이스 범위 31종(Pod, Service, Deployment, ConfigMap, Secret, PVC...), 클러스터 범위 27종(Node, Namespace, PV, StorageClass, ClusterRole, CRD...). **Node와 PV는 네임스페이스에 속하지 않는다.** 네임스페이스 오브젝트 자체도 클러스터 범위다 (도커 책 6, 7장).

##### 3-4. 이름 규칙 (실측)

| 시도 | 결과 |
|---|---|
| `Team-A` (대문자) | **거부**: `a lowercase RFC 1123 label must consist of lower case alphanumeric characters or '-'...` |
| `my_ns` (밑줄) | **거부** |
| `1abc` (숫자로 시작) | **생성됨** (소문자/숫자/`-`로 이뤄지고 영숫자로 시작하고 끝나면 된다) |
| `a-very-long-name-...` (63자 초과) | **거부**: `metadata.name`과 **`metadata.labels`**(자동 라벨 `kubernetes.io/metadata.name`) 두 곳의 길이 오류 |
| `kube-mine` (`kube-` 접두) | **생성됨** (오류/경고 없음) |
| `default2` | 생성됨 |

- 네임스페이스 이름은 **RFC 1123 레이블 규칙: 소문자, 숫자, `-`, 최대 63자**다. 같은 이름이 서비스 DNS 이름(`<서비스>.<네임스페이스>.svc`)에도 쓰이므로 DNS 규칙을 따른다.
- 63자 초과가 이름과 **자동으로 붙는 라벨 `kubernetes.io/metadata.name=<이름>`** 두 곳에서 모두 오류가 났다. 모든 네임스페이스에 이 라벨이 자동으로 붙는다 (`k get ns t4n --show-labels` → `kubernetes.io/metadata.name=t4n`).
- **`kube-` 접두어는 시스템용으로 예약하는 관례**라고 문서에 있지만, 이 환경에서는 `kube-mine`이 **막히지 않고 만들어졌다.** 관례일 뿐이므로 직접 쓰지 않는 규칙이 필요하다 (이 점은 이 서버에서 만든 결과이고 다른 설정에서는 다를 수 있다).

##### 3-5. 같은 이름, 다른 네임스페이스

```
$ k -n t4n  create cm same --from-literal=a=1
$ k -n t4n2 create cm same --from-literal=a=2
$ k get cm -A --field-selector metadata.name=same --no-headers
t4n   same
t4n2  same                                              ← 같은 이름이 네임스페이스마다 공존
$ k get cm same -A
error: a resource cannot be retrieved by name across all namespaces
$ k get cm same                                         # 기본 네임스페이스(default)에서는
Error from server (NotFound): configmaps "same" not found
```

- `-A`와 이름을 같이 쓸 수 없다. 모든 네임스페이스에서 이름으로 찾으려면 **`--field-selector metadata.name=<이름>`** 을 쓴다.
- `-n`을 생략하면 **컨텍스트의 기본 네임스페이스(보통 `default`)** 만 본다. "분명히 만들었는데 안 보인다"의 흔한 원인이다 (3장 컨텍스트).

##### 3-6. 삭제가 멈추는 경우: Terminating에 갇힌 네임스페이스

네임스페이스를 삭제하면 안의 모든 리소스를 정리한 뒤에야 사라진다. 한 리소스가 **finalizer**를 가지고 있으면 네임스페이스가 `Terminating`에서 멈춘다.

```
$ k -n t4stuck apply -f - <<EOF          # finalizer를 가진 ConfigMap
kind: ConfigMap
metadata: {name: blocker, namespace: t4stuck, finalizers: ["t4.example.com/cleanup"]}
EOF
$ k delete ns t4stuck --wait=false  ; sleep 8
$ k get ns t4stuck
t4stuck   Terminating
$ k get ns t4stuck -o jsonpath='{range .status.conditions[*]}{.type}={.status} ({.reason}): {.message}{"\n"}{end}'
NamespaceDeletionDiscoveryFailure=False (ResourcesDiscovered): All resources successfully discovered
NamespaceDeletionGroupVersionParsingFailure=False (ParsedGroupVersions): All legacy kube types successfully parsed
NamespaceDeletionContentFailure=False (ContentDeleted): All content successfully deleted, may be waiting on finalization
NamespaceContentRemaining=True (SomeResourcesRemain): Some resources are remaining: configmaps. has 1 resource instances
NamespaceFinalizersRemaining=True (SomeFinalizersRemain): Some content in the namespace has finalizers remaining: t4.example.com/cleanup in 1 resource instances
$ k get ns t4stuck -o jsonpath='spec.finalizers={.spec.finalizers} deletionTimestamp={.metadata.deletionTimestamp}'
spec.finalizers=["kubernetes"] deletionTimestamp=2026-10-25T10:44:48Z
$ k -n t4stuck get cm blocker -o jsonpath='cm deletionTimestamp={.metadata.deletionTimestamp} finalizers={.metadata.finalizers}'
cm deletionTimestamp=2026-10-25T10:44:53Z finalizers=["t4.example.com/cleanup"]       ← 삭제 요청은 됐지만 finalizer 때문에 남음
$ k -n t4stuck create cm another --from-literal=a=b
error: ... forbidden: unable to create new content in namespace t4stuck because it is being terminated
```

**해결: 막고 있는 리소스의 finalizer를 제거**

```
$ k -n t4stuck patch cm blocker --type=merge -p '{"metadata":{"finalizers":null}}'
$ k get ns t4stuck           →  Error from server (NotFound)         ← 6초 안에 네임스페이스가 사라짐
```

- **finalizer**는 "삭제 전에 정리 작업이 필요하다"는 표시다. 삭제 요청을 받으면 `deletionTimestamp`만 찍히고, 컨트롤러가 정리를 마치고 finalizer를 제거해야 실제로 삭제된다 (이 실험에서는 정리를 맡은 컨트롤러가 없어서 영원히 멈춘다).
- `Terminating` 네임스페이스에는 **새 리소스를 만들 수 없다** (`unable to create new content ... being terminated`).
- 진단 순서: ① `k get ns <이름> -o jsonpath='{.status.conditions}'`에서 `NamespaceContentRemaining`(남은 리소스 종류)과 `NamespaceFinalizersRemaining`(남은 finalizer)를 본다 ② 해당 리소스를 찾아 정말 정리가 필요한지 확인한 뒤 finalizer를 제거한다.
- 흔한 실제 원인: **CRD를 먼저 지워서** 그 CRD의 CR을 지울 컨트롤러/정의가 없는 경우, 네임스페이스 삭제 중 외부 시스템이 응답하지 않는 경우 (12장의 CRD 삭제 순서와 연결. 이번에는 CRD 시나리오로 재현하지 않았다).
- 네임스페이스 오브젝트 자체의 `spec.finalizers: ["kubernetes"]`는 쿠버네티스가 네임스페이스 안의 내용물을 모두 정리한 뒤 제거한다. 이를 `/finalize` 서브리소스로 강제 제거하는 방법이 널리 알려져 있지만 **내용물이 정리되지 않은 채 남을 수 있어 마지막 수단**이다 (이 방법은 시험하지 않았다).

##### 3-7. 네임스페이스로 할 수 있는 일, 못 하는 일

| 가능 | 불가능 (별도 수단 필요) |
|---|---|
| 이름 충돌 방지 (같은 이름 공존) | **네트워크 격리** (다른 네임스페이스의 Service/Pod IP로 접근 가능, 도커 책 7장. NetworkPolicy 필요) |
| RBAC 범위 (Role/RoleBinding) | 노드 격리 (같은 노드를 공유, taint/affinity 필요) |
| ResourceQuota / LimitRange (도커 책 7, 11장) | 보안 격리 수준의 완전한 분리 (hostPath 등으로 우회 가능, 9장) |
| 환경(dev/stg/prod), 팀, 앱 단위의 논리 구분 | |

#### 4. 확인하지 못한 부분

- **컨트롤 플레인 이중화(HA)**: 마스터 여러 대, etcd 멤버 장애, API 서버 로드 밸런싱, 스케줄러/컨트롤러 매니저 리더 전환은 컨트롤 플레인이 1대인 두 환경에서 확인하지 못했다.
- **cloud-controller-manager**: Docker Desktop과 kind에는 없어서 확인하지 못했다 (클라우드 환경 전용).
- **노드 장애 후 Pod 퇴출/재배치**: `tolerationSeconds: 300` 이후 Pod가 다른 노드로 옮겨지는 것은 5분을 기다리지 않았다. NotReady 판정 46초는 5초 간격 폴링으로 1회 측정했다.
- **노드 합류(join) 절차**: 워커의 `kubelet.conf`의 `server` 값만 확인했다. 부트스트랩 토큰, TLS 부트스트랩, 인증서 발급은 확인하지 않았다.
- **kubelet이 스태틱 Pod를 감시하는 주기와 미러 Pod**: 매니페스트를 치우고 되돌렸을 때 Pod가 내려가고 올라오는 것은 확인했지만 감시 주기(실험 중 20~30초 기다림)나 API 서버에 만들어지는 미러 Pod는 확인하지 않았다.
- **kube-proxy 모드(iptables/IPVS)**, **CNI 플러그인의 동작**(kindnet이 어떻게 Pod 네트워크를 구성하는지), Docker Desktop 클러스터의 CNI 구성은 확인하지 않았다.
- **API 서버가 kubelet(10250)으로 `logs`/`exec` 요청을 보내는 흐름**은 포트와 구조로 설명했고 패킷으로 확인하지 않았다.
- **네임스페이스의 `/finalize` 서브리소스로 강제 삭제**하는 방법과 CRD를 먼저 지운 경우의 Terminating 재현은 시험하지 않았다.
- **`kube-` 접두 네임스페이스 허용**은 이 서버에서의 결과이고 어드미션/정책에 따라 막힐 수 있다.
- 2-3절의 포트 표는 **Docker Desktop 노드에서 `ss`로 확인한 값**이다. kind 클러스터(kubeadm 방식)에서 같은 포트를 확인하지는 않았다.
