### 6장. 쿠버네티스 시작하기

> 정리 범위: 쿠버네티스 관리 단위(오브젝트), 기본 컴포넌트(API 서버, 컨트롤러 매니저, 스케줄러 등), Pod / ReplicaSet / Service / Deployment, Pod와 도커 컨테이너의 차이, ReplicaSet의 동작 원리(라벨 셀렉터)
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), **Docker Desktop 내장 쿠버네티스 v1.32.2** (단일 노드 `docker-desktop`, 컨테이너 런타임 `docker://28.0.4`), kubectl 클라이언트 v1.35.2 (서버와 마이너 버전 차이가 커서 경고가 뜨지만 동작에는 문제 없었다)
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키고 있어서, 모든 명령에 `--context docker-desktop`을 명시했다 (아래 명령에서는 `k`로 줄여 적었다). 실험은 전용 네임스페이스 `t6`에서만 했고 끝나고 네임스페이스째 삭제했다.

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. 쿠버네티스 관리 단위: 오브젝트
2. 기본 컴포넌트
3. Pod, ReplicaSet, Service, Deployment 개념
4. Pod vs 도커 컨테이너
5. ReplicaSet 동작 원리: 라벨 셀렉터
6. Service와 Deployment 동작 확인
7. 확인하지 못한 부분

---

#### 1. 쿠버네티스 관리 단위: 오브젝트

쿠버네티스가 관리하는 모든 것(Pod, Service, Deployment, 노드, 네임스페이스 …)은 **API 오브젝트**이고, 오브젝트의 **원하는 상태(desired state)** 를 API 서버에 저장하면 컨트롤러들이 **현재 상태(current state)** 를 거기에 맞춘다. 도커의 `docker run`처럼 "이렇게 실행해라"라는 명령이 아니라 "이런 상태여야 한다"라는 선언이다.

##### 1-1. 오브젝트의 공통 구조

```yaml
apiVersion: apps/v1        # API 그룹/버전
kind: ReplicaSet           # 오브젝트 종류
metadata:                  # 이름, 네임스페이스, 라벨, 어노테이션, ownerReferences ...
  name: front
  namespace: t6
spec:                      # 사용자가 선언하는 "원하는 상태"
  replicas: 3
  ...
status:                    # 컨트롤러/kubelet이 채우는 "현재 상태" (사용자가 쓰지 않는다)
```

- `spec`은 사용자가, `status`는 시스템이 쓴다. 컨트롤러가 하는 일은 `spec`과 `status`의 차이를 줄이는 것이다 (reconcile loop).
- 필드 설명은 `kubectl explain`으로 서버 버전에 맞게 볼 수 있다. 이 서버에서 `k explain pod.spec --recursive`는 1123줄이 나왔다.

##### 1-2. 이 클러스터가 제공하는 리소스 종류

```
$ k api-resources --no-headers | wc -l
58
```

| 구분 | 리소스 (이 환경에서 확인) | API 그룹 |
|---|---|---|
| 네임스페이스에 속함 | pods, services, endpoints, configmaps, secrets, persistentvolumeclaims, events | `v1` (core) |
| | deployments, replicasets, statefulsets, daemonsets | `apps/v1` |
| | jobs, cronjobs | `batch/v1` |
| | endpointslices | `discovery.k8s.io/v1` |
| | ingresses | `networking.k8s.io/v1` |
| 클러스터 전체 | nodes, namespaces, persistentvolumes | `v1` |

- **네임스페이스**: 오브젝트 이름의 범위를 나누는 논리적 칸막이다. 같은 이름의 Pod도 네임스페이스가 다르면 공존한다. nodes, namespaces, persistentvolumes처럼 어느 네임스페이스에도 속하지 않는 클러스터 범위 리소스도 있다 (`api-resources`의 NAMESPACED 열이 false).
- 이 책에서 순서대로 다루는 단위는 Pod → ReplicaSet → Deployment → Service이고, 이후 장에서 Ingress(8장), PV/PVC(9장), ServiceAccount/RBAC(10장) 등이 이어진다.

##### 1-3. 오브젝트는 etcd에 저장된다

생성한 오브젝트가 실제로 어디에 있는지, etcd(컨트롤 플레인 Pod 안에 `etcdctl`이 있다)에서 키를 조회했다.

```
$ k -n kube-system exec etcd-docker-desktop -- etcdctl --endpoints=https://127.0.0.1:2379 \
    --cacert=... --cert=... --key=... get /registry/ --prefix --keys-only | grep /t6
/registry/configmaps/t6/kube-root-ca.crt
/registry/deployments/t6/web
/registry/events/t6/web-5bd64f6ccb-hmmrr.18e08d926a2a9429
...
/registry/namespaces/t6
/registry/pods/t6/web-5bd64f6ccb-hmmrr
/registry/pods/t6/web-5bd64f6ccb-lwbmt
/registry/replicasets/t6/web-5bd64f6ccb
/registry/serviceaccounts/t6/default
```

- 키 형식이 `/registry/<리소스>/<네임스페이스>/<이름>`이다. Deployment 하나를 만들었는데 ReplicaSet과 Pod, Event, 기본 ServiceAccount, ConfigMap(`kube-root-ca.crt`)까지 같이 저장되어 있다.
- 네임스페이스를 만들면 `kube-root-ca.crt` ConfigMap과 `default` ServiceAccount가 자동으로 생긴다.

#### 2. 기본 컴포넌트

클러스터는 **컨트롤 플레인**(결정하는 쪽)과 **노드**(실제로 컨테이너를 돌리는 쪽)로 나뉜다.

| 컴포넌트 | 위치 | 역할 |
|---|---|---|
| **kube-apiserver** | 컨트롤 플레인 | 모든 요청의 단일 진입점(REST API). 인증/인가/어드미션을 거쳐 etcd에 읽고 쓴다. **etcd와 직접 통신하는 유일한 컴포넌트** |
| **etcd** | 컨트롤 플레인 | 클러스터의 모든 상태를 저장하는 키-값 저장소 |
| **kube-scheduler** | 컨트롤 플레인 | 아직 노드가 정해지지 않은 Pod를 감시하다가, 조건에 맞는 노드를 골라 Pod에 배정(바인딩) |
| **kube-controller-manager** | 컨트롤 플레인 | 여러 컨트롤러(deployment, replicaset, node, endpoint …)를 한 프로세스로 묶어 실행. 현재 상태를 원하는 상태에 맞춘다 |
| **kubelet** | 각 노드 | 자기 노드에 배정된 Pod를 컨테이너 런타임으로 실제로 실행하고 상태를 API 서버에 보고 |
| **kube-proxy** | 각 노드 | Service의 가상 IP를 실제 Pod IP로 연결하는 네트워크 규칙을 노드에 설정 |
| 컨테이너 런타임 | 각 노드 | 컨테이너를 실행 (이 환경은 dockerd → containerd → runc, 4장 참고) |
| CoreDNS (애드온) | 클러스터 | 클러스터 내부 DNS. 서비스 이름을 IP로 해석 |

##### 2-1. 이 환경에서 직접 확인한 것

```
$ k get pods -n kube-system
coredns-668d6bf9bc-...            1/1 Running
etcd-docker-desktop               1/1 Running
kube-apiserver-docker-desktop     1/1 Running
kube-controller-manager-docker-desktop  1/1 Running
kube-proxy-w54n4                  1/1 Running
kube-scheduler-docker-desktop     1/1 Running
storage-provisioner               1/1 Running
```

- 컨트롤 플레인 컴포넌트가 **Pod로** 떠 있다. `ownerReferences`의 종류가 `Node`인 **스태틱 Pod**(kubelet이 노드의 매니페스트 디렉터리에서 직접 띄우는 Pod)다. API 서버가 죽어도 kubelet이 이것들을 다시 띄울 수 있는 이유다.
- kube-proxy는 `DaemonSet`으로, 노드마다 하나씩 뜬다.
- 주요 실행 옵션(이 환경):

```
kube-apiserver:  --etcd-servers=https://127.0.0.1:2379  --service-cluster-ip-range=10.96.0.0/12
                 --authorization-mode=Node,RBAC  --enable-admission-plugins=NodeRestriction  --secure-port=6443
etcd:            --listen-client-urls=https://127.0.0.1:2379  --data-dir=/var/lib/etcd
kube-scheduler:  --leader-elect=true
kube-controller-manager:  --controllers=*,bootstrapsigner,tokencleaner  --leader-elect=false
                          --node-monitor-period=30s  --node-monitor-grace-period=180s
```

  - API 서버는 `127.0.0.1:2379`의 etcd만 바라본다.
  - Service에 할당되는 ClusterIP 범위가 `10.96.0.0/12`다 (아래 6절의 `10.111.x.x`가 이 안에 있다).
  - 컨트롤러 매니저의 `--controllers=*`는 내장 컨트롤러를 전부 켠다는 뜻이다.
  - `node-monitor-grace-period=180s`는 Docker Desktop이 바꾼 값으로 보인다. 기본값과 비교하는 실험은 하지 않았다.
- 노드의 kubelet은 VM 안에서 `kubelet ... --container-runtime-endpoint unix:///var/run/cri-dockerd.sock`로 실행 중이다. kubelet은 도커를 직접 부르지 않고 **CRI**(Container Runtime Interface) 규격으로 `cri-dockerd`에 요청하고, cri-dockerd가 dockerd를 호출한다. 같은 VM에 다른 kubelet 프로세스도 보이는데 (이 PC의 kind 클러스터 노드들) 읽기만 했다.
- `k get --raw='/readyz?verbose'`로 API 서버의 준비 상태 점검 항목이 `[+] ... ok`로 나열되고 마지막에 `readyz check passed`가 나온다.

##### 2-2. Deployment 하나 만들 때 누가 무엇을 하는가 (이벤트로 확인)

```
$ k create ns t6
$ k -n t6 create deployment web --image=nginx:alpine --replicas=2
$ k -n t6 get events.events.k8s.io -o custom-columns=CTRL:.reportingController,REASON:.reason | sort | uniq -c
      2 default-scheduler       Scheduled
      1 deployment-controller   ScalingReplicaSet
      2 kubelet                 Created
      2 kubelet                 Pulled
      2 kubelet                 Started
      2 replicaset-controller   SuccessfulCreate
```

시간순으로 정리하면 다음과 같다 (모두 13:11:51~52, 1초 안에 끝났다).

| 순서 | 주체 | 한 일 |
|---|---|---|
| 1 | kubectl → **API 서버** | Deployment 오브젝트를 etcd에 저장 |
| 2 | **deployment-controller** | Deployment를 보고 ReplicaSet 생성 ("Scaled up replica set web-5bd64f6ccb from 0 to 2") |
| 3 | **replicaset-controller** | ReplicaSet의 replicas=2를 보고 Pod 2개 생성 ("Created pod: …") |
| 4 | **default-scheduler** | `nodeName`이 비어 있는 Pod 2개에 노드 배정 ("Successfully assigned t6/web-… to docker-desktop") |
| 5 | **kubelet** | 자기 노드에 배정된 Pod의 이미지를 확인(Pulled)하고 컨테이너를 만들고(Created) 시작(Started) |

- 컨트롤러들이 서로 직접 호출하지 않는다. 각자 API 서버를 **watch**하면서, 자기가 관심 있는 오브젝트가 바뀌면 자기 몫만 처리하고 결과를 다시 API 서버에 쓴다. 다음 컴포넌트는 그 변경을 보고 움직인다.
- 오브젝트 소유 관계도 확인했다.

```
$ k -n t6 get pod -o jsonpath='{range .items[*]}{.metadata.name}{" owner="}{.metadata.ownerReferences[0].kind}/{.metadata.ownerReferences[0].name}{" node="}{.spec.nodeName}{" ip="}{.status.podIP}{"\n"}{end}'
web-5bd64f6ccb-hmmrr owner=ReplicaSet/web-5bd64f6ccb node=docker-desktop ip=10.1.0.227
web-5bd64f6ccb-lwbmt owner=ReplicaSet/web-5bd64f6ccb node=docker-desktop ip=10.1.0.228
```

  Deployment → ReplicaSet → Pod 순으로 `ownerReferences`가 이어진다.

##### 2-3. 스케줄러의 동작 확인

존재하지 않는 노드 라벨을 `nodeSelector`로 요구하는 Pod를 만들었다.

```
$ k -n t6 run pend --image=nginx:alpine --overrides='{"spec":{"nodeSelector":{"disk":"ssd"}}}'
$ k -n t6 get pod pend
pend   0/1   Pending   0     3s
  Warning  FailedScheduling  default-scheduler  0/1 nodes are available: 1 node(s) didn't match Pod's node affinity/selector. ...

$ k label node docker-desktop disk=ssd        # 노드에 라벨을 달자
$ k -n t6 get pod pend                        # 4초 뒤
pend   1/1   Running   0     15s
$ k label node docker-desktop disk-           # 라벨 제거 (실험 후 원복, 확인함)
```

- 조건에 맞는 노드가 없으면 Pod는 `Pending`으로 남고 스케줄러가 `FailedScheduling` 이벤트로 이유를 남긴다.
- 노드에 라벨이 생기자 스케줄러가 자동으로 다시 시도해서 배정했다. 사용자가 다시 요청할 필요가 없다.

#### 3. Pod, ReplicaSet, Service, Deployment 개념

| 오브젝트 | 한 줄 정의 | 관리 대상 | 핵심 필드 |
|---|---|---|---|
| **Pod** | 쿠버네티스가 배포/스케줄링하는 **최소 단위**. 컨테이너 1개 이상 + 공유 네트워크/볼륨 | 컨테이너 | `containers`, `volumes` |
| **ReplicaSet** | 같은 Pod를 **지정한 개수만큼** 항상 유지 | Pod | `replicas`, `selector`, `template` |
| **Deployment** | ReplicaSet을 관리하며 **버전 교체(롤링 업데이트), 롤백**을 담당 | ReplicaSet | `replicas`, `strategy`, `template` |
| **Service** | 라벨로 고른 Pod 묶음에 **고정된 가상 IP/DNS 이름**을 부여하고 부하를 분산 | Pod 접근 경로 | `selector`, `ports`, `type` |

- 관계: `Deployment ─owns→ ReplicaSet ─owns→ Pod`, `Service ─selector→ Pod`. Service는 Pod를 소유하지 않고 **라벨로만** 연결한다.
- **Pod는 일회용이다.** 죽으면 같은 Pod가 살아나는 것이 아니라 (컨테이너 재시작은 제외) 새 Pod가 새 IP로 생긴다. 그래서 (1) 개수를 유지해 주는 ReplicaSet이 필요하고, (2) IP가 바뀌어도 접근할 수 있게 Service가 필요하며, (3) 버전 교체를 안전하게 해 주는 Deployment가 필요하다.
- 실무에서는 Pod나 ReplicaSet을 직접 만드는 일이 드물고 **Deployment로 선언**한다. 이 장의 ReplicaSet 실험은 동작 원리를 보기 위한 것이다.

#### 4. Pod vs 도커 컨테이너

##### 4-1. 비교

| 구분 | 도커 컨테이너 | 쿠버네티스 Pod |
|---|---|---|
| 단위 | 프로세스 하나(격리된 환경) | **컨테이너 1개 이상의 묶음** |
| 네트워크 | 컨테이너마다 자기 네트워크 네임스페이스와 IP | **Pod 안의 모든 컨테이너가 하나의 IP와 네트워크 네임스페이스를 공유** (`localhost`로 통신) |
| 볼륨 | 컨테이너에 직접 마운트 | `volumes`로 Pod에 선언하고, 컨테이너별로 `volumeMounts`. 컨테이너끼리 공유 가능 |
| IPC | 기본은 분리 | Pod 안에서 공유 |
| PID 네임스페이스 | 분리 | 기본은 분리, `shareProcessNamespace: true`로 공유 가능 |
| 재시작 | `--restart` 정책을 도커 데몬이 처리 | `restartPolicy`를 **kubelet**이 처리 |
| 죽은 뒤 | 같은 컨테이너가 재시작되거나 사라짐 | 컨테이너는 같은 Pod 안에서 재시작, **Pod 자체는 지워지면 새 Pod(새 IP)** 로 대체 |
| 생성 주체 | 사용자가 `docker run` | 보통 컨트롤러(ReplicaSet 등)가 만든다 |
| 스케줄링 | 호스트를 사용자가 정함 | 스케줄러가 노드를 정함 |
| 최소 구성 | 컨테이너 | Pod = 컨테이너들 + 보이지 않는 **pause 컨테이너** |

##### 4-2. 직접 확인: 컨테이너 2개짜리 Pod

nginx와, 2초마다 공유 볼륨에 날짜를 쓰는 alpine 컨테이너를 한 Pod에 넣었다.

```yaml
apiVersion: v1
kind: Pod
metadata: {name: multi, namespace: t6, labels: {app: multi}}
spec:
  volumes:
  - name: shared
    emptyDir: {}
  containers:
  - name: web
    image: nginx:alpine
    volumeMounts: [{name: shared, mountPath: /usr/share/nginx/html}]
  - name: writer
    image: alpine:3.20
    command: ["sh","-c","while true; do date > /data/index.html; sleep 2; done"]
    volumeMounts: [{name: shared, mountPath: /data}]
```

**(1) localhost와 볼륨 공유**

```
$ k -n t6 exec multi -c writer -- wget -qO- http://localhost:80
Wed Oct 21 13:12:20 UTC 2026
```

`writer` 컨테이너에서 `localhost:80`으로 `web` 컨테이너의 nginx에 접근했고, `writer`가 `emptyDir`에 쓴 날짜가 nginx 응답으로 나왔다.

**(2) IP와 호스트명은 하나**

```
$ k -n t6 exec multi -c web    -- sh -c 'hostname; ip addr show eth0 | grep "inet "'
multi
    inet 10.1.0.229/16 ...
$ k -n t6 exec multi -c writer -- sh -c 'hostname; ip addr show eth0 | grep "inet "'
multi
    inet 10.1.0.229/16 ...
```

두 컨테이너의 IP(10.1.0.229)와 호스트명(`multi`)이 같다.

**(3) 네임스페이스를 어떻게 공유하는가**

```
$ docker inspect -f '{{.Name}} Net={{.HostConfig.NetworkMode}} Ipc={{.HostConfig.IpcMode}}' <각 컨테이너>
/k8s_writer_multi_t6_..._0 Net=container:17cd0fdc6ad9... Ipc=container:17cd0fdc6ad9...
/k8s_web_multi_t6_..._0    Net=container:17cd0fdc6ad9... Ipc=container:17cd0fdc6ad9...

$ docker inspect -f '{{.Name}} {{.Config.Image}} {{.State.Pid}}' 17cd0fdc6ad9
/k8s_POD_multi_t6_..._0 registry.k8s.io/pause:3.10 ...
```

- 두 컨테이너의 네트워크/IPC 모드가 모두 `container:17cd0fdc…`, 즉 **`pause` 컨테이너의 네임스페이스를 가져다 쓰도록(2장의 `--net=container:<id>`와 같은 방식) 만들어져 있다.** pause 컨테이너는 아무 일도 하지 않고 네임스페이스만 붙들고 있는 "Pod의 뼈대"다.
- 네임스페이스 inode를 VM 안에서 `/proc/<pid>/ns/*`로 비교했다.

| 네임스페이스 | web | writer | 같은가 |
|---|---|---|---|
| net | 4026533779 | 4026533779 | ✅ 공유 |
| ipc | 4026533776 | 4026533776 | ✅ 공유 |
| pid | 4026533911 | 4026533915 | ❌ 분리 |
| uts | 4026533910 | 4026533914 | ❌ 분리 (호스트명은 kubelet이 같은 값으로 설정) |
| mnt | 4026533909 | 4026533913 | ❌ 분리 (파일시스템은 서로 다르고 볼륨만 공유) |

- 즉 Pod = **net, ipc는 공유하고 pid, mnt는 컨테이너별로 격리**된 컨테이너 묶음이다. `writer`에서 `ps`를 하면 자기 프로세스만 보인다 (PID 1이 `sh -c while true …`).
- 이 환경(Docker Desktop)의 `docker ps`에는 pause 컨테이너가 나오지 않는다 (`docker ps -a | grep -c pause` → 0). 숨겨진 것으로 보이고, ID로 `docker inspect`하면 조회되며 VM 안의 `ps`에서는 `/pause` 프로세스가 보인다. 화면에는 `k8s_web_…`, `k8s_writer_…`만 나온다. 컨테이너 이름이 `k8s_<컨테이너>_<파드>_<네임스페이스>_<uid>_<재시작 횟수>` 형식이다.

**(4) `shareProcessNamespace: true`**

```
$ k -n t6 exec spn -c a -- ps
PID   USER     COMMAND
    1 65535    /pause
    7 root     sleep 3600        # 컨테이너 a
   13 root     sleep 3601        # 컨테이너 b
```

PID 네임스페이스를 공유하면 한 컨테이너에서 다른 컨테이너의 프로세스가 보이고, **PID 1이 pause**가 된다.

**(5) 컨테이너 재시작 vs Pod 재생성**

```
$ k -n t6 get pod multi -o jsonpath='{.status.podIP}'      → 10.1.0.229
$ docker kill <web 컨테이너 ID>                            # 컨테이너만 강제 종료
$ k -n t6 get pod multi -o jsonpath='{.status.podIP}{" restarts="}{.status.containerStatuses[?(@.name=="web")].restartCount}'
10.1.0.229 restarts=1                                      # kubelet이 같은 Pod 안에서 재시작, IP 유지

$ k -n t6 delete pod multi ; k apply -f multi.yaml         # Pod를 지우고 다시 생성
10.1.0.230 uid=8e51ca19-...                                 # 새 IP, 새 uid
```

- 컨테이너가 죽으면 kubelet이 **같은 Pod 안에서** 재시작한다 (`restartCount` 증가, IP 유지).
- Pod 자체를 지우면 같은 이름으로 다시 만들어도 **새 오브젝트(새 uid, 새 IP)** 다. 이 Pod는 컨트롤러 없이 직접 만든 Pod라서 지우면 되살아나지 않았다 (ReplicaSet이 있어야 되살아난다, 5절).

##### 4-3. 왜 컨테이너 여러 개를 한 Pod에 넣는가

- 항상 **같이 배치되고 같이 확장되어야 하는** 보조 프로세스(로그 수집기, 프록시 같은 사이드카)를 `localhost`와 공유 볼륨으로 붙이기 위해서다. 위 실험의 `writer`가 그 예다.
- 서로 독립적으로 확장해야 하는 서비스(웹과 DB)는 **별도의 Pod**로 분리하는 것이 원칙이다. 한 Pod의 컨테이너는 같이 스케줄링되고 같이 복제된다.

#### 5. ReplicaSet 동작 원리: 라벨 셀렉터

##### 5-1. 원리

ReplicaSet은 **`selector`로 라벨이 맞는 Pod를 세어서**, `replicas`보다 적으면 `template`으로 만들고 많으면 삭제한다. Pod의 이름이나 생성 경로가 아니라 **라벨**이 기준이다.

```
ReplicaSet(selector: tier=front, replicas: 3)
        │ 감시: 라벨이 selector와 맞고 + 소유자가 자신(또는 소유자 없음)인 Pod 수
        ▼
   부족 → template으로 Pod 생성      과다 → Pod 삭제
```

- `template.metadata.labels`는 반드시 `selector`와 맞아야 한다. 안 맞으면 만들어진 Pod를 자기 것으로 못 세기 때문에 API 서버가 아예 거부한다.
- `selector`는 만든 뒤에 바꿀 수 없다 (immutable).

```
$ k apply -f rs-bad.yaml          # selector는 tier=front, template 라벨은 tier=other
The ReplicaSet "bad" is invalid: spec.template.metadata.labels: Invalid value: map[string]string{"tier":"other"}: `selector` does not match template `labels`

$ k -n t6 patch rs front --type=merge -p '{"spec":{"selector":{"matchLabels":{"tier":"x"}}}}'
The ReplicaSet "front" is invalid:
* spec.selector: Invalid value: ...: field is immutable
```

##### 5-2. 자가 치유: Pod를 지우면 다시 채운다

```
$ k -n t6 delete pod web-5bd64f6ccb-hmmrr
$ k -n t6 get pod -l app=web          # 3초 뒤
web-5bd64f6ccb-lwbmt Running
web-5bd64f6ccb-mqkll Running           # 새 이름의 Pod가 생겨 개수 2를 유지
```

##### 5-3. 라벨을 바꾸면 ReplicaSet 관리에서 빠진다

Deployment가 만든 ReplicaSet(selector: `app=web, pod-template-hash=…`)의 Pod 하나에서 `app` 라벨만 바꿨다.

```
$ k -n t6 label pod web-5bd64f6ccb-lwbmt app=web-debug --overwrite
$ k -n t6 get pod --show-labels             # 3초 뒤
web-5bd64f6ccb-4ptjg Running app=web,pod-template-hash=5bd64f6ccb      # ← 새로 생김
web-5bd64f6ccb-lwbmt Running app=web-debug,pod-template-hash=...       # 살아 있지만 관리 밖
web-5bd64f6ccb-mqkll Running app=web,pod-template-hash=...
$ k -n t6 get pod web-5bd64f6ccb-lwbmt -o jsonpath='{.metadata.ownerReferences}'
(빈 출력)                                                               # 소유자 정보가 제거됨
```

- 라벨이 셀렉터와 안 맞게 되자 ReplicaSet이 **그 Pod를 "내 것이 아니다"로 판단하고 소유 관계(`ownerReferences`)를 제거**했고, 부족해진 1개를 새로 만들었다.
- 관리에서 빠진 Pod는 그대로 살아 있으므로 **문제가 생긴 Pod를 삭제하지 않고 격리해서 디버깅**하는 방법으로 쓸 수 있다.
- 다시 `app=web`으로 되돌리면:

```
$ k -n t6 label pod web-5bd64f6ccb-lwbmt app=web --overwrite
$ k -n t6 get pod                            # 3초 뒤
web-5bd64f6ccb-lwbmt Running ...               # 다시 입양(owner=ReplicaSet)
web-5bd64f6ccb-mqkll Running ...
                                               # 개수가 3개로 넘치자 가장 새로 만든 4ptjg가 삭제됨
```

##### 5-4. 이미 있는 Pod를 입양한다 (adoption)

`tier=front` 라벨의 일반 Pod 2개(`bare1`, `bare2`)를 먼저 만들고, 같은 셀렉터로 `replicas: 3`인 ReplicaSet을 만들었다.

```
$ k -n t6 get pod -l tier=front -o 'custom-columns=NAME:.metadata.name,OWNER:.metadata.ownerReferences[0].name'
bare1         front
bare2         front
front-w6jmb   front        # ReplicaSet이 만든 것은 1개뿐
```

- ReplicaSet은 기존 Pod 2개를 **입양**하고 부족한 1개만 새로 만들었다. 즉 "자기가 만든 Pod"가 아니라 "라벨이 맞는 Pod"를 센다.
- 이 때문에 **셀렉터가 겹치는 ReplicaSet/Pod를 따로 만들면 서로의 Pod를 가져가거나 삭제하는 혼란**이 생길 수 있다.
- 입양된 Pod는 ReplicaSet이 삭제될 때 같이 사라진다 (`k -n t6 delete rs front` 뒤 `bare1`, `bare2`도 없어짐).

**이미 다른 컨트롤러가 소유한 Pod는 가져가지 않는다.** `matchExpressions`(`env in (dev, stg)`)를 쓰는 ReplicaSet `expr`을 만든 뒤, `bare1`에 `env=stg`를 붙였다.

```
$ k -n t6 label pod bare1 env=stg
$ k -n t6 get pod -l 'env in (dev,stg)' -o 'custom-columns=NAME:.metadata.name,LABELS:.metadata.labels,OWNER:.metadata.ownerReferences[0].name'
bare1        map[env:stg tier:front]   front        # 셀렉터는 맞지만 소유자는 여전히 front
expr-ptzbg   map[env:dev]              expr         # expr은 자기 Pod를 새로 만들었다
```

- `bare1`은 `expr`의 셀렉터에도 맞지만 이미 `front`가 소유(`ownerReferences`에 `controller: true`)하고 있어서 입양되지 않았다. 입양 대상은 **소유자가 없는(orphan) Pod**뿐이다.
- 셀렉터는 `matchLabels`(동등 비교) 외에 `matchExpressions`(`In`, `NotIn`, `Exists`, `DoesNotExist`)도 쓸 수 있다.

##### 5-5. 삭제 방식: 연쇄 삭제와 `--cascade=orphan`

```
$ k -n t6 delete rs expr --cascade=orphan
$ k -n t6 get pod expr-ptzbg -o jsonpath='{.metadata.name} owner={.metadata.ownerReferences}'
expr-ptzbg owner=              # Pod는 남고 소유 정보만 제거됨

$ k -n t6 delete rs front       # 기본 삭제
                                # → 소유한 Pod도 가비지 컬렉터가 함께 삭제
```

- 기본값은 ReplicaSet을 지우면 `ownerReferences`를 따라 **Pod도 함께 지워진다.**
- `--cascade=orphan`을 주면 Pod를 남기고 ReplicaSet만 지운다.

##### 5-6. Deployment가 관리하는 ReplicaSet은 직접 못 바꾼다

```
$ k -n t6 scale rs web-5bd64f6ccb --replicas=5
$ k -n t6 get rs web-5bd64f6ccb         # 3초 뒤
web-5bd64f6ccb   2     2     2          # 이미 2로 되돌아와 있음
```

ReplicaSet을 직접 수정해도 소유자인 Deployment 컨트롤러가 `spec.replicas`를 자기 값(2)으로 되돌린다. 개수를 바꾸려면 Deployment를 바꿔야 한다 (`k scale deploy web --replicas=4`는 ReplicaSet까지 4가 됨).

#### 6. Service와 Deployment 동작 확인

##### 6-1. Service: 라벨 셀렉터로 Pod를 고르고 가상 IP를 준다

```
$ k -n t6 expose deploy web --port=80 --name=web-svc
$ k -n t6 get svc web-svc
web-svc   ClusterIP   10.111.178.121   <none>   80/TCP

$ k -n t6 get endpoints web-svc
web-svc   10.1.0.228:80,10.1.0.232:80        # 지금 app=web 라벨인 Pod들의 IP
```

- `ClusterIP`(10.111.178.121)는 클러스터 안에서만 쓰는 **가상 IP**로, Pod가 바뀌어도 고정이다. 대상 Pod 목록은 Endpoints/EndpointSlice가 라벨 셀렉터로 계속 갱신한다.
- ReplicaSet과 같은 방식으로 **라벨이 바뀌면 엔드포인트에서 빠진다.** Pod 하나의 `app` 라벨을 `out`으로 바꾸자 5초 뒤 엔드포인트가 새 Pod(10.1.0.242)로 교체되었다 (ReplicaSet이 대체 Pod를 만들었기 때문).

**DNS와 환경 변수**

```
$ k -n t6 exec cli -- cat /etc/resolv.conf
nameserver 10.96.0.10
search t6.svc.cluster.local svc.cluster.local cluster.local
options ndots:5

$ k -n t6 exec cli -- nslookup web-svc.t6.svc.cluster.local
Address: 10.111.178.121
$ k -n t6 exec cli -- env | grep WEB_SVC
WEB_SVC_SERVICE_HOST=10.111.178.121
WEB_SVC_SERVICE_PORT=80
```

- `10.96.0.10`은 CoreDNS의 Service IP다. `search`에 네임스페이스가 들어 있어서 같은 네임스페이스에서는 `web-svc`만으로 해석된다. 다른 네임스페이스는 `web-svc.<ns>`처럼 붙인다.
- Pod 생성 시점에 이미 존재하던 Service는 `<이름>_SERVICE_HOST` 형태의 환경 변수로도 주입된다.

**부하 분산: kube-proxy가 만든 iptables 규칙**

```
$ for i in $(seq 1 12); do wget -qO- http://web-svc; done | sort | uniq -c       # Pod마다 다른 응답을 하게 해 둠
      8 pod web-5bd64f6ccb-lwbmt
      4 pod web-5bd64f6ccb-mqkll                  # 라운드 로빈이 아니라 랜덤 분산

# VM(노드)의 nat 테이블
-A KUBE-SERVICES -d 10.111.178.121/32 -p tcp --dport 80 -j KUBE-SVC-V3J36KHVPXSU43VA
-A KUBE-SVC-V3J36KHVPXSU43VA -m statistic --mode random --probability 0.50000000000 -j KUBE-SEP-YLB34AW3VBGRB7CT
-A KUBE-SVC-V3J36KHVPXSU43VA                                                      -j KUBE-SEP-YKE7THSBO2LJKZHI
-A KUBE-SEP-YLB34AW3VBGRB7CT -p tcp -j DNAT --to-destination 10.1.0.228:80
-A KUBE-SEP-YKE7THSBO2LJKZHI -p tcp -j DNAT --to-destination 10.1.0.232:80
```

- ClusterIP로 가는 패킷을 **iptables DNAT가 Pod IP로 바꾼다.** ClusterIP는 어떤 네트워크 인터페이스에도 붙어 있지 않은 가상 IP이고, kube-proxy가 만든 이 규칙이 있어서 동작한다.
- 엔드포인트가 2개이면 첫 규칙이 `probability 0.5`, 마지막 규칙이 나머지 전부다. 12번 요청에서 8:4가 나온 것은 이 확률 기반 분산 때문이다 (엄밀한 균등 분배가 아니다).
- kube-proxy 설정의 `mode`가 빈 값이고 iptables 규칙이 실제로 보이므로 이 환경은 iptables 모드다.

**외부 접근**

| 방법 | 결과 (이 환경) |
|---|---|
| `type: NodePort` (31065) | VM의 호스트 네트워크에서는 접속됨 (`wget 127.0.0.1:31065`). **Mac의 `localhost:31065`는 HTTP 000 (연결 실패)** |
| `type: LoadBalancer` | `EXTERNAL-IP`가 `<pending>`으로 남았고 Mac의 `localhost:18081`도 연결 실패 |
| `kubectl port-forward svc/web-svc 18082:80` | `localhost:18082`가 HTTP 200 |

- NodePort와 LoadBalancer가 Mac에서 안 되는 이유는 확인하지 못했다. Docker Desktop이 노드 포트를 Mac으로 전달하지 않는 것으로 추정되지만 원인을 확인한 것은 아니다. 로컬 확인에는 `port-forward`가 확실했다.

##### 6-2. Deployment: ReplicaSet을 교체하며 버전을 올린다

**롤링 업데이트** (replicas=2, 기본 전략 `maxSurge: 25%`, `maxUnavailable: 25%`)

```
$ k -n t6 set image deploy/web nginx=nginx:1.27-alpine
$ k -n t6 get rs -w
NAME             DESIRED   CURRENT   READY   AGE
web-5bd64f6ccb   2         2         2       3m30s      # 기존
web-7dd566fbcc   1         1         0       0s         # ① 새 RS가 생기고 1개 생성
web-7dd566fbcc   1         1         1       9s         # ② 새 Pod Ready
web-5bd64f6ccb   1         2         2       3m39s      # ③ 기존 RS를 1로 줄임
web-7dd566fbcc   2         1         1       9s         # ④ 새 RS를 2로 늘림
web-7dd566fbcc   2         2         2       10s
web-5bd64f6ccb   0         1         1       3m40s      # ⑤ 기존 RS 0
```

- Deployment는 Pod 템플릿이 바뀌면 **새 ReplicaSet을 만들고, 새 RS는 늘리고 옛 RS는 줄이는 식으로** 교체한다. ReplicaSet 이름의 해시(`5bd64f6ccb` → `7dd566fbcc`)는 Pod 템플릿의 해시이고, Pod에는 `pod-template-hash` 라벨로 붙어서 **버전별 ReplicaSet의 셀렉터가 서로 겹치지 않게 한다.**
- replicas=2에서 `maxSurge 25%`는 올림해 1개, `maxUnavailable 25%`는 내림해 0개이므로, "새 Pod 1개를 먼저 띄워서 Ready가 된 뒤 옛 Pod를 줄이는" 순서가 위 로그와 맞다 (이 반올림 규칙은 문서 지식이고, 위 로그가 그와 모순되지 않는 것까지만 확인했다).
- 롤링 업데이트 후 옛 ReplicaSet은 **replicas 0으로 남는다** (`web-5bd64f6ccb 0 0 0`). 롤백에 쓰기 위해서다.

**리비전과 롤백**

```
$ k -n t6 rollout history deploy/web
REVISION  CHANGE-CAUSE
1         <none>
2         <none>
$ k -n t6 rollout undo deploy/web
$ k -n t6 rollout history deploy/web
2         <none>
3         <none>             # 되돌린 옛 버전이 새 리비전 번호(3)로 올라옴
$ k -n t6 get rs
web-5bd64f6ccb 2 2 2          # 옛 RS의 replicas가 다시 늘어남 (새 RS를 만들지 않고 재사용)
web-7dd566fbcc 0 0 0
```

**잘못된 이미지로 업데이트하면** (`nginx:does-not-exist`)

```
$ k -n t6 get pod -l app=web
web-5bd64f6ccb-944x6 1/1 Running            # 옛 Pod 2개는 그대로 서비스 중
web-5bd64f6ccb-jl2tt 1/1 Running
web-7cc4dc4688-m7jsw 0/1 ErrImagePull        # 새 Pod 1개만 실패
$ k -n t6 get deploy web
web   2/2   1     2                          # READY 2/2, UP-TO-DATE 1, AVAILABLE 2
$ k -n t6 get endpoints web-svc
web-svc   10.1.0.245:80,10.1.0.246:80        # 옛 Pod만 엔드포인트에 있음
```

- 새 Pod가 Ready가 되지 않으면 옛 Pod를 줄이지 않고 **롤아웃이 멈춘다.** 서비스는 옛 Pod가 계속 처리한다. 이후 `rollout undo`로 복구했다.

**Recreate 전략**

```
$ k -n t6 patch deploy web -p '{"spec":{"strategy":{"type":"Recreate","rollingUpdate":null}}}'
$ k -n t6 set image deploy/web nginx=nginx:1.27-alpine
$ k -n t6 get pod -w
web-5bd64f6ccb-944x6 1/1 Terminating
web-5bd64f6ccb-jl2tt 1/1 Terminating
...
$ k -n t6 get rs
web-7dd566fbcc 2 2 2          # 최종적으로 새 RS만 2
```

- `Recreate`는 옛 Pod를 전부 종료한 뒤 새 Pod를 만드는 전략이다 (다운타임 발생). 관찰한 구간에서는 옛 Pod가 전부 `Terminating`으로 바뀌는 것까지 보였고, 새 Pod가 만들어진 정확한 시점은 기록하지 못했다.

**Deployment 삭제**

```
$ k -n t6 delete deploy web
$ k -n t6 get rs,pod                       # 4초 뒤
(web 관련 ReplicaSet, Pod 모두 없음)        # Deployment → ReplicaSet → Pod 연쇄 삭제
```

#### 7. 확인하지 못한 부분

- **멀티 노드 동작**: 노드가 1개뿐이라 스케줄러의 노드 선택 기준(점수, 어피니티, 테인트/톨러레이션), 노드 장애 시 Pod 재배치, 노드 간 Pod 통신은 확인하지 못했다.
- **컨트롤 플레인 고가용성**: etcd 클러스터(Raft, 3장과 같은 원리)와 API 서버 다중화, 스케줄러의 리더 선출(`--leader-elect=true`가 켜져 있는 것까지만 확인)은 실험하지 않았다. 컨트롤러 매니저가 멈추면 어떻게 되는지도 확인하지 않았다.
- **API 서버의 요청 처리 단계**: 인증, 인가(`Node,RBAC`), 어드미션을 거치는 과정은 설정값만 확인했고 직접 거부/통과시키는 실험은 하지 않았다 (10장 범위).
- **kube-proxy의 다른 모드**: iptables 모드만 확인했다. IPVS, nftables 모드와 `externalTrafficPolicy`는 확인하지 못했다.
- **Service의 다른 형태**: headless Service(`clusterIP: None`), ExternalName, `sessionAffinity`는 확인하지 않았다. NodePort/LoadBalancer가 Mac에서 안 되는 정확한 원인은 못 찾았다.
- **readinessProbe와 엔드포인트의 관계**: Ready가 아닌 Pod가 엔드포인트에서 빠지는 것은 이 장에서 확인하지 않았다 (잘못된 이미지 실험에서는 새 Pod가 엔드포인트에 처음부터 없었다).
- **Deployment 세부 옵션**: `minReadySeconds`, `progressDeadlineSeconds`, `revisionHistoryLimit`, `rollout pause/resume`, `maxSurge/maxUnavailable`의 다른 조합, `change-cause` 어노테이션은 확인하지 않았다. 반올림 규칙(surge 올림, unavailable 내림)은 로그가 모순되지 않는 정도만 확인했다.
- **Recreate 전략의 새 Pod 생성 시점**: 옛 Pod 종료 이후에 새 Pod가 만들어지는지 시간 순서까지는 기록하지 못했다.
- **StatefulSet, DaemonSet, Job**: 이름만 `api-resources`에서 확인했고 동작은 보지 않았다 (13장 범위).
- **`node-monitor-grace-period=180s`**가 쿠버네티스 기본값과 어떻게 다른지, 왜 그렇게 설정되었는지는 확인하지 않았다.
