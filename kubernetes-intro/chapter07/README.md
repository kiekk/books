### 7장. 서비스

> 정리 범위: 서비스 타입의 종류와 개념, headless service의 개념과 특징, kube-proxy의 개념과 모드 (현재 버전의 기본 모드)
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), **Docker Desktop 내장 쿠버네티스 v1.32.2**(단일 노드 `docker-desktop`, kube-proxy 기본 설정 = iptables). kube-proxy의 **nftables / ipvs 모드**는 별도의 임시 **kind 클러스터**(v1.35.0, control-plane 1 + worker 1, `networking.kubeProxyMode`로 모드 지정, 실험 후 삭제, 별도 kubeconfig 사용)에서 확인했다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 Docker Desktop 실험은 모두 `--context docker-desktop`을 명시했다 (`k`로 줄여 적음). 실험 네임스페이스 `t7s`와 kind 클러스터는 끝나고 삭제했다.
>
> 다른 책 `시작하세요! 도커/쿠버네티스` 6장(Service 기본, ClusterIP의 iptables 규칙)과 8장(인그레스), 13장(StatefulSet의 헤드리스 Service)과 겹치는 부분은 짧게 적고 링크했다 ([6장](../../start-docker-kubernetes/chapter06/README.md), [13장](../../start-docker-kubernetes/chapter13/README.md)).

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. 서비스 개념
2. 서비스 타입
3. Headless Service
4. kube-proxy와 모드
5. 선택 가이드와 함정
6. 확인하지 못한 부분

---

#### 1. 서비스 개념

**Pod 집합에 변하지 않는 접근 지점(가상 IP와 DNS 이름)을 부여하고 트래픽을 분산하는 오브젝트.** Pod의 IP는 재생성될 때마다 바뀌므로(5장), 클라이언트가 Pod IP를 직접 쓸 수 없다.

```
클라이언트 ──▶ Service(고정 IP/DNS) ──▶ 라벨 셀렉터에 맞는 Ready Pod들 (EndpointSlice로 관리)
```

- Service는 **셀렉터로 Pod를 고르고**, 고른 Pod의 IP 목록은 **EndpointSlice**가 유지한다 (Ready인 Pod만 포함, 11장/도커 책 11장).
- **Service의 가상 IP는 어느 네트워크 인터페이스에도 붙어 있지 않고**, 각 노드의 **kube-proxy가 만든 규칙**이 그 IP로 가는 패킷을 Pod IP로 바꿔 보낸다 (4절).
- 서비스 이름은 클러스터 DNS(CoreDNS)에 등록된다: `<서비스>.<네임스페이스>.svc.cluster.local` (4장 CoreDNS).

#### 2. 서비스 타입

##### 2-1. 한눈에 정리

| 타입 | 접근 범위 | 가상 IP(ClusterIP) | 노드 포트 | 외부 LB | 대표 용도 |
|---|---|---|---|---|---|
| **ClusterIP** (기본) | **클러스터 내부** | 있음 | 없음 | 없음 | 내부 서비스 간 통신 |
| **NodePort** | 클러스터 내부 + **노드 IP:포트** | 있음 | **모든 노드의 같은 포트** (30000~32767) | 없음 | 개발/테스트, 외부 LB의 대상 |
| **LoadBalancer** | 외부 | 있음 | (기본으로 할당) | **클라우드 LB** | 운영에서 외부 공개 |
| **ExternalName** | 내부 → 외부 이름 | **없음** | 없음 | 없음 | 외부 서비스에 클러스터 내부 이름 부여 |
| **Headless** (`clusterIP: None`) | 내부 (DNS로 Pod IP 직접) | **없음** | 없음 | 없음 | StatefulSet, 클라이언트 측 분산 (3절) |

- **포함 관계**: `LoadBalancer` ⊃ `NodePort` ⊃ `ClusterIP`. NodePort는 ClusterIP도 함께 만들고, LoadBalancer는 NodePort도 함께 만든다 (기본값).
- 그 외에 `externalIPs`(특정 노드 IP를 서비스 IP로 노출) 같은 기능이 있으나 이번에 시험하지 않았다.

##### 2-2. ClusterIP (기본)

```yaml
apiVersion: v1
kind: Service
metadata: {name: web-cip}
spec:
  selector: {app: web}
  ports:
  - {name: http, port: 80,   targetPort: http}     # targetPort는 컨테이너 포트 이름도 가능
  - {name: alt,  port: 8080, targetPort: alt}      # 한 서비스에 포트 여러 개
```

```
$ k -n t7s get svc web-cip -o wide
web-cip   ClusterIP   10.111.79.184   <none>   80/TCP,8080/TCP
$ k -n t7s get svc web-cip -o jsonpath='type={.spec.type} clusterIP={.spec.clusterIP} sessionAffinity={.spec.sessionAffinity} ipFamilies={.spec.ipFamilies} internalTrafficPolicy={.spec.internalTrafficPolicy}'
type=ClusterIP clusterIP=10.111.79.184 sessionAffinity=None ipFamilies=["IPv4"] policy=SingleStack internalTrafficPolicy=Cluster
$ k -n t7s get endpointslice -l kubernetes.io/service-name=web-cip -o jsonpath=...
10.1.2.38 10.1.2.36 10.1.2.37   ports=alt:8081 http:80                  ← 포트가 이름별로 다른 Pod 포트에 매핑
```

| 필드 | 의미 |
|---|---|
| `port` | **Service가 노출하는 포트** (클라이언트가 접속하는 포트) |
| `targetPort` | **Pod(컨테이너)의 포트** (숫자 또는 **이름**). 위에서 `http`→80, `alt`→8081 |
| `name` | 포트가 둘 이상이면 필수 |
| `clusterIP` | 비워 두면 자동 할당. 직접 지정도 가능하지만 **서비스 CIDR 범위 안**이어야 한다 |

- 이 클러스터의 서비스 CIDR은 `10.96.0.0/12`이다 (도커 책 6장에서 확인한 apiserver 옵션 `--service-cluster-ip-range`). 범위 밖의 IP를 지정하면 거부된다.

```
$ (clusterIP: 1.2.3.4)
The Service "bad-ip" is invalid: spec.clusterIPs: Invalid value: []string{"1.2.3.4"}: failed to allocate IP 1.2.3.4: the provided IP (1.2.3.4) is not in the valid range. The range of valid IPs is 10.9...
```

**클라이언트 쪽에서 본 모습**

```
$ k -n t7s exec cli -- sh -c 'dig +short web-cip.t7s.svc.cluster.local; for i in $(seq 1 12); do curl -s http://web-cip; done | sort | uniq -c'
10.111.79.184                                        ← DNS는 ClusterIP 하나를 돌려줌
      3 web-6c79577865-5s5t9
      5 web-6c79577865-kmkbb
      4 web-6c79577865-x84hm                          ← 요청은 3개 Pod로 분산 (균등하진 않음: 확률 기반, 4절)
$ k -n t7s exec cli -- dig +short SRV _http._tcp.web-cip.t7s.svc.cluster.local
0 100 80 web-cip.t7s.svc.cluster.local.             ← 이름 붙은 포트는 SRV 레코드(_<포트이름>._<프로토콜>.<서비스>)로도 조회
```

##### 2-3. NodePort

**클러스터의 모든 노드에서 같은 포트(nodePort)를 열고, 그 포트로 들어온 트래픽을 Service로 보낸다.** 외부에서 `노드IP:nodePort`로 접속할 수 있다.

```
$ k -n t7s get svc web-np
web-np   NodePort   10.107.129.141   <none>   80:30996/TCP                ← 80(서비스 포트) : 30996(nodePort)
$ k -n t7s apply -f bad-np.yaml     # nodePort: 8080
The Service "bad-np" is invalid: spec.ports[0].nodePort: Invalid value: 8080: provided port is not in the valid range. The range of valid ports is 30000-32767
```

- nodePort는 **지정하지 않으면 30000~32767에서 자동 할당**된다 (API 서버에 `--service-node-port-range`를 따로 주지 않은 기본 범위). 범위 밖은 거부된다.
- kube-proxy가 **모든 노드의 `KUBE-NODEPORTS` 체인**에 규칙을 만든다 (4절의 규칙).

```
$ docker run --rm --net=host alpine:3.20 wget -q -T3 -O- http://127.0.0.1:30996        →  web-6c79577865-x84hm   (노드 안에서 localhost)
$ docker run --rm --net=host alpine:3.20 wget -q -T3 -O- http://192.168.65.3:30996     →  web-6c79577865-kmkbb   (노드 IP)
$ k -n t7s exec cli -- curl -s -m 3 http://192.168.65.3:30996                            →  web-6c79577865-kmkbb   (Pod에서 노드 IP:nodePort)
$ curl -s -m 3 -o /dev/null -w "%{http_code}" localhost:30996      (Mac에서)              →  000
```

- 노드(VM)와 Pod 안에서는 `노드IP:nodePort`와 `127.0.0.1:nodePort` 모두 접속된다. **Mac에서는 `localhost:nodePort`가 연결되지 않았다** (Docker Desktop이 노드 포트를 Mac으로 전달하지 않는 것으로 보인다. 이전 책 8, 9장과 같은 관찰).
- **단점**: 포트 범위가 한정되고(서비스 수 제한), **노드 IP가 클라이언트에 노출**되며, 클라이언트가 노드 장애를 직접 처리해야 한다. 그래서 운영에서는 앞단에 LoadBalancer나 인그레스(8장)를 둔다.

**externalTrafficPolicy와 클라이언트 IP (실측)**

백엔드(nginx) 접근 로그의 소스 IP를 비교했다 (kubelet 프로브 요청 `kube-probe`는 제외). 클라이언트는 `cli` Pod(10.1.2.35)다.

| 경로 | 백엔드가 본 소스 IP |
|---|---|
| Pod → **ClusterIP** | **10.1.2.35** (클라이언트 Pod IP가 보존됨) |
| Pod → `노드IP:nodePort`, **`externalTrafficPolicy: Cluster`** (기본) | **10.1.0.1** (노드 쪽 주소로 SNAT: 클라이언트 IP가 사라짐) |
| Pod → `노드IP:nodePort`, **`externalTrafficPolicy: Local`** | **10.1.2.35** (보존됨) |

- `Cluster`(기본): 다른 노드의 Pod로도 보낼 수 있어 분산은 고르지만, 홉이 늘어나 **소스 IP를 마스커레이드(SNAT)** 한다 (`KUBE-MARK-MASQ`).
- `Local`: **그 노드에 있는 Pod로만** 보내고 SNAT하지 않아 **클라이언트 IP가 보존**된다. 대신 그 노드에 Pod가 없으면 연결이 실패하므로 분산이 불균등할 수 있다. 외부 LB가 있을 때는 `healthCheckNodePort`로 Pod가 있는 노드만 고르게 한다 (문서 지식. NodePort 타입에서는 이 필드가 비어 있었고 LoadBalancer에서 쓰이는 것이다).
- 규칙이 바뀌는 것도 확인했다: `Local`일 때는 `KUBE-SVL-...`(Local 전용 체인)이 생겼고 `KUBE-EXT-...` 체인은 **노드 자신에서 온 트래픽(`--src-type LOCAL`)만 MASQ**했다. 단일 노드라 "Pod가 없는 노드에서의 실패"는 확인하지 못했다.

##### 2-4. LoadBalancer

**클라우드 제공자의 로드 밸런서를 만들어 외부 IP를 받는 타입.** ClusterIP와 NodePort도 함께 만들어지고 클라우드 LB가 노드의 nodePort로 트래픽을 보낸다.

```
$ k -n t7s get svc web-lb
web-lb   LoadBalancer   10.97.254.82   <pending>   8088:30424/TCP                  ← EXTERNAL-IP가 <pending>
$ k -n t7s get svc web-lb -o jsonpath='nodePort={.spec.ports[0].nodePort} allocateLoadBalancerNodePorts={.spec.allocateLoadBalancerNodePorts} status={.status}'
nodePort=30424 allocateLoadBalancerNodePorts=true status={"loadBalancer":{}}        ← NodePort 자동 할당, 외부 주소 없음
$ k -n kube-system get pods | grep -ciE "cloud-controller|metallb|ccm"                →  0       ← LB를 만들어 줄 컨트롤러가 없다
$ (allocateLoadBalancerNodePorts: false)  → nodePort=[]                                  ← NodePort를 할당하지 않을 수도 있다
```

- **외부 IP를 할당하는 것은 클라우드의 `cloud-controller-manager`**(AWS/GCP 등)다 (4장). 이 환경에는 그런 컨트롤러가 없어서 **`EXTERNAL-IP`가 영원히 `<pending>`** 이다 (이전 책 8장과 같음). 온프레미스에서는 MetalLB 같은 구현을 따로 설치한다 (설치하지 않았다).
- `allocateLoadBalancerNodePorts: false`는 LB가 파드로 직접 트래픽을 보내는 환경에서 NodePort 할당을 생략하는 옵션이다 (이 환경에서 nodePort가 비어 있는 것까지 확인).
- LoadBalancer는 **서비스 하나가 LB 하나**라서 비용이 서비스 수에 비례한다. 여러 HTTP 서비스를 하나의 LB로 묶으려면 인그레스(8장)를 쓴다.

##### 2-5. ExternalName

**클러스터 내부 이름을 외부 DNS 이름으로 연결하는 타입.** 프록시나 가상 IP 없이 **DNS의 CNAME**만 돌려준다.

```
$ (type: ExternalName, externalName: example.com)
$ k -n t7s get svc ext -o wide
ext   ExternalName   CLUSTER-IP=<none>   EXTERNAL-IP=example.com
$ k -n t7s get endpointslice -l kubernetes.io/service-name=ext          →  No resources found
$ k -n t7s exec cli -- dig +noall +answer ext.t7s.svc.cluster.local
ext.t7s.svc.cluster.local. 30  IN  CNAME  example.com.
example.com.               30  IN  A      104.20.23.154  (외 1개)
$ iptables nat 규칙 중 t7s/ext 를 포함한 것: 0                              ← kube-proxy는 아무 규칙도 만들지 않는다
$ k -n t7s exec cli -- curl -s -o /dev/null -w "%{http_code}" -m 6 http://ext                       →  403   (Host: ext)
$ k -n t7s exec cli -- curl -s -o /dev/null -w "%{http_code}" -m 6 -H 'Host: example.com' http://ext  →  200
```

- ClusterIP도 엔드포인트도 없고 kube-proxy 규칙도 없다. **DNS 조회 결과가 외부 이름의 CNAME**이다.
- **함정: HTTP `Host` 헤더**. 클라이언트가 `http://ext`로 접속하면 `Host: ext`로 요청이 나가서 외부 서버가 **403**으로 거부했다. 외부 서비스가 Host 헤더를 검사하면 `Host: example.com`을 직접 지정해야 한다 (200). TLS(SNI/인증서 이름) 검증도 같은 문제가 있다 (이번에 시험하지 않았다).
- 용도: 외부 DB/SaaS 주소를 `db.prod.svc`처럼 클러스터 내부 이름으로 감싸 두고, 나중에 외부 서비스를 클러스터 안으로 옮길 때 이름을 그대로 유지하는 것.

##### 2-6. 셀렉터가 없는 Service와 EndpointSlice

셀렉터를 생략하면 Pod를 자동으로 고르지 않는다. **EndpointSlice를 직접 만들어** 임의의 IP로 연결할 수 있다.

```
$ (Service manual: selector 없음) + (EndpointSlice manual-1: 라벨 kubernetes.io/service-name=manual, endpoints: <web Pod IP>)
$ k -n t7s get svc manual                            →  manual  ClusterIP  10.102.239.241
$ k -n t7s get endpoints manual                      →  Error from server (NotFound): endpoints "manual" not found
$ k -n t7s exec cli -- sh -c 'for i in 1 2 3; do curl -s http://manual; done | sort | uniq -c'
      3 web-6c79577865-5s5t9                         ← 지정한 Pod(첫 번째 web Pod)로만 전달됨
```

- **EndpointSlice를 직접 만들면 Service가 그 주소로 동작**한다 (`kubernetes.io/service-name` 라벨로 연결). 클러스터 밖의 DB, 다른 클러스터의 주소 등을 Service로 감쌀 때 쓴다.
- 레거시 `Endpoints` 오브젝트는 **만들어지지 않았다** (`endpoints "manual" not found`). EndpointSlice가 현재의 기본이고 `kubectl get endpoints`만 보면 비어 보인다.

##### 2-7. sessionAffinity: 같은 클라이언트를 같은 Pod로

```
$ (sessionAffinity: ClientIP, timeoutSeconds: 30)
--- 설정 없는 web-cip, 12 요청:   5 / 4 / 3 (3개 Pod에 분산)
--- sessionAffinity ClientIP:    12 요청이 모두 web-6c79577865-5s5t9      ← 한 Pod로 고정
--- 35초 뒤 같은 클라이언트의 요청:    4 요청이 web-6c79577865-x84hm          ← 타임아웃 후 다른 Pod가 선택됨
$ iptables 규칙:  -A KUBE-SEP-... -p tcp -m recent --set --name KUBE-SEP-... --mask 255.255.255.255 --rsource -j DNAT --to-destination 10.1.2.37:80
```

- `sessionAffinity: ClientIP`는 **소스 IP 기준**으로 같은 Pod에 고정한다 (기본 3시간 = 10800초, `sessionAffinityConfig.clientIP.timeoutSeconds`로 변경). iptables 모드에서는 `recent` 모듈로 클라이언트 IP를 기억한다.
- 한계: **소스 IP 기준**이라 클라이언트가 NAT 뒤에 있으면 많은 사용자가 한 Pod로 몰린다. 쿠키 기반 세션 고정은 Service가 아니라 인그레스 컨트롤러 기능이다 (8장).

#### 3. Headless Service

##### 3-1. 개념

**`clusterIP: None`으로 만든 Service. 가상 IP도 로드 밸런싱도 없이, DNS가 Pod의 IP를 직접 알려 준다.**

```yaml
apiVersion: v1
kind: Service
metadata: {name: web-hl}
spec:
  clusterIP: None
  selector: {app: web}
  ports: [{name: http, port: 80, targetPort: http}]
```

| | 일반 ClusterIP Service | **Headless Service** |
|---|---|---|
| ClusterIP | 있음 (`10.111.79.184`) | **`None`** |
| DNS A 레코드 | ClusterIP **1개** | **Ready Pod의 IP 전부** |
| 로드 밸런싱 | **kube-proxy**가 분산 | 없음 (클라이언트가 고른다) |
| kube-proxy 규칙 | 있음 | **없음** |
| 대표 용도 | 일반 서비스 | StatefulSet의 개별 Pod 접근, 클라이언트 측 분산, 서비스 디스커버리 |

##### 3-2. 실측

```
$ k -n t7s get svc web-hl web-cip
web-hl    ClusterIP   CLUSTER-IP=None            80/TCP
web-cip   ClusterIP   CLUSTER-IP=10.111.79.184   80/TCP,8080/TCP
$ k -n t7s get pod -l app=web -o jsonpath='{range .items[*]}{.status.podIP} {end}'
10.1.2.37 10.1.2.36 10.1.2.38
$ k -n t7s exec cli -- sh -c 'dig +short web-hl.t7s.svc.cluster.local'
10.1.2.37 10.1.2.38 10.1.2.36                         ← Pod IP 3개 모두
$ k -n t7s exec cli -- sh -c 'dig +short web-cip.t7s.svc.cluster.local'
10.111.79.184                                         ← ClusterIP 하나
```

- **응답 순서가 바뀐다.** 첫 번째 IP를 8번 질의해서 세 보니 `.36` 4번, `.37` 3번, `.38` 1번이었다 (CoreDNS의 `loadbalance` 플러그인이 순서를 섞는다. 4장 Corefile에 `loadbalance`가 있었다). 클라이언트는 보통 첫 IP를 쓰므로 **DNS 라운드 로빈**으로 분산된다 (균등하지는 않다).

```
$ k -n t7s exec cli -- sh -c 'for i in $(seq 1 9); do curl -s -o /dev/null -w "%{remote_ip}\n" http://web-hl; done | sort | uniq -c'
      4 10.1.2.36
      5 10.1.2.37                                       ← 이름으로 접속했는데 Pod IP에 직접 연결 (ClusterIP를 거치지 않음)
$ (iptables) rules mentioning t7s/web-hl: 0   (web-cip: 20)             ← kube-proxy는 headless에 규칙을 만들지 않는다
$ k -n t7s exec cli -- dig +short SRV _http._tcp.web-hl.t7s.svc.cluster.local
0 33 80 10-1-2-38.web-hl.t7s.svc.cluster.local.
0 33 80 10-1-2-36.web-hl.t7s.svc.cluster.local.
0 33 80 10-1-2-37.web-hl.t7s.svc.cluster.local.       ← Pod별 이름(IP의 점을 하이픈으로 바꾼 형태)
```

- 클라이언트는 DNS에서 **Pod IP를 직접** 받아 연결한다. kube-proxy가 중간에서 분산하지 않으므로 **분산 방식은 클라이언트가 결정**한다(첫 IP만 쓰는 클라이언트는 사실상 한 Pod에 고정, 전체 목록을 받아 직접 고르는 클라이언트는 자체 분산). DB 클라이언트 드라이버나 gRPC가 이 방식(클라이언트 측 로드 밸런싱)을 쓴다.
- SRV 레코드에 Pod별 이름(`10-1-2-38.web-hl...`)이 나온다. 이 이름은 **IP 기반의 자동 이름**이다.

##### 3-3. Ready가 아닌 Pod는 빠진다 (`publishNotReadyAddresses`)

readiness가 실패하는 Pod(`nr1`)와 정상 Pod(`ok1`)를 같은 셀렉터에 두고 두 Service로 조회했다.

```
pods: nr1=10.1.2.40 ready=False   ok1=10.1.2.39
headless (기본):                      10.1.2.39                 ← Ready인 Pod만
headless publishNotReadyAddresses:    10.1.2.39 10.1.2.40       ← Ready가 아니어도 포함
```

- 기본은 **Ready인 Pod만** DNS에 나온다 (일반 Service의 엔드포인트와 같은 규칙). `publishNotReadyAddresses: true`를 주면 **Ready가 아니어도 DNS에 노출**한다. 클러스터링 앱(DB, ZooKeeper)이 **서로를 찾아 합류하는 동안**에는 아직 Ready가 아니므로 이 옵션이 필요하다 (문서 지식. 이번에는 DNS 응답 차이만 확인했다).

##### 3-4. 개별 Pod의 DNS 이름 (hostname + subdomain)

StatefulSet은 이 이름을 자동으로 붙여 준다 (도커 책 13장: `db-1.db.t13.svc`). 일반 Pod도 `hostname`과 `subdomain`(= 헤드리스 Service 이름)을 지정하면 같은 이름을 가진다.

```
$ (Pod named: hostname: db-primary, subdomain: named-hl) + (headless Service named-hl, 같은 Pod 선택)
$ dig +short db-primary.named-hl.t7s.svc.cluster.local     →  10.1.2.41     ← Pod 개별 이름
$ dig +short named-hl.t7s.svc.cluster.local                  →  10.1.2.41     ← 서비스 이름(Pod 전체)
$ dig +short 10-1-2-36.web-hl.t7s.svc.cluster.local         →  10.1.2.36     ← hostname을 안 정한 Pod: IP 기반 이름
```

- **Pod의 `subdomain`이 Service 이름과 같아야** 개별 이름이 생긴다. 이 이름은 Pod IP가 바뀌어도 같은 이름으로 새 IP를 가리킨다 (도커 책 13장에서 IP 변경 후에도 이름 유지 확인).

##### 3-5. 셀렉터 없는 headless: 외부 주소를 DNS로

```
$ (Service external-db: clusterIP None, selector 없음, port 5432) + (EndpointSlice: 192.0.2.10, 192.0.2.11)
$ dig +short external-db.t7s.svc.cluster.local              →  192.0.2.10 192.0.2.11
```

- 셀렉터 없는 headless에 EndpointSlice를 붙이면 **임의의 IP들을 클러스터 DNS 이름으로 노출**한다 (클러스터 밖의 DB 복제본 주소 등). ExternalName(CNAME)은 이름 하나만 가리킬 수 있지만 이 방식은 **IP 여러 개의 A 레코드**를 줄 수 있다.

##### 3-6. 특징 정리

| 특징 | 내용 |
|---|---|
| ClusterIP 없음 | `CLUSTER-IP=None`, kube-proxy 규칙 없음 |
| DNS가 Pod IP 전체 반환 | 순서는 섞임 (CoreDNS `loadbalance`), 일부 클라이언트는 첫 IP만 사용 |
| Ready Pod만 (기본) | `publishNotReadyAddresses: true`로 모두 |
| 개별 Pod DNS | `<hostname>.<서비스>.<ns>.svc` (StatefulSet 자동, 일반 Pod는 `hostname`+`subdomain`) |
| SRV 레코드 | 포트 이름이 있으면 Pod별로 반환 |
| 셀렉터 없음 + EndpointSlice | 임의 IP들을 DNS A 레코드로 |
| **로드 밸런싱을 하지 않음** | 분산은 클라이언트/DNS 순서에 달림. **연결 재사용(keep-alive, 커넥션 풀)** 때문에 한 Pod에 쏠릴 수 있다 |
| DNS 캐시 주의 | CoreDNS TTL 30초, 클라이언트(JVM 등)가 더 오래 캐시하면 **죽은 Pod IP**로 연결을 시도한다 (이번에 시험하지 않았다) |

#### 4. kube-proxy와 모드

##### 4-1. 개념

**각 노드에서 실행되며, Service(가상 IP)로 가는 트래픽을 실제 Pod로 보내는 네트워크 규칙을 커널에 설정하는 에이전트.** 이름과 달리 **트래픽이 kube-proxy 프로세스를 통과하지 않는다.** kube-proxy는 API 서버를 watch해서 Service/EndpointSlice 변화를 보고 **커널의 패킷 처리 규칙(iptables, ipvs, nftables)을 갱신**할 뿐이고, 실제 패킷은 커널이 처리한다.

| 항목 (이 환경 실측) | 값 |
|---|---|
| 배포 | **DaemonSet** (노드마다 하나), `hostNetwork: true`, `privileged: true`, 톨러레이션 `Exists` (모든 taint 허용) |
| 헬스 체크 | `:10256` (`/healthz`, `/livez`), 모든 인터페이스 |
| 메트릭 | `127.0.0.1:10249` |
| 규칙 동기화 | `syncPeriod: 30s`, `minSyncPeriod: 0s` (변화가 있으면 즉시 갱신, 주기적 전체 동기화는 30초) |
| 주요 설정 | `clusterCIDR: ""`, `masqueradeAll: false`, `conntrack.maxPerCore: 0` |

```
$ k -n kube-system get ds kube-proxy -o jsonpath='hostNetwork={...hostNetwork} privileged={...privileged} tolerations={...tolerations}'
hostNetwork=true privileged=true tolerations=[{"operator":"Exists"}]
$ k -n kube-system get cm kube-proxy -o jsonpath='{.data.config\.conf}'        (발췌)
mode: ""     syncPeriod: 30s     minSyncPeriod: 0s     masqueradeAll: false     clusterCIDR: ""
metricsBindAddress: 127.0.0.1:10249     healthzBindAddress: 0.0.0.0:10256
```

- 로그에 `Kube-proxy configuration may be incomplete or incorrect ... nodePortAddresses is unset; NodePort connections will be accepted on all local IPs`라는 경고가 있었다. `nodePortAddresses`를 지정하지 않으면 **모든 노드 IP에서 NodePort를 받는다**는 뜻이다 (운영에서는 특정 대역으로 제한할 수 있다).
- 로그의 `Setting route_localnet=1 to allow node-ports on localhost`는 `localhost:nodePort`가 동작하게 하려는 설정이다 (위 NodePort 실측에서 `127.0.0.1:30996`이 접속된 이유).

##### 4-2. 모드 종류와 현재 버전의 기본값

| 모드 | 방식 | 상태 (이번 실측과 문서 지식) |
|---|---|---|
| **iptables** | iptables 규칙 (nat 테이블) | **Linux 기본값**. 이 환경(v1.32)과 kind(v1.35)에서 `mode: ""`일 때 **"Using iptables Proxier"** |
| **ipvs** | 커널 IPVS (L4 로드 밸런서) + ipset/iptables 일부 | v1.35 로그에 **deprecated 경고** |
| **nftables** | nftables 규칙 | iptables의 후속. 이번에 v1.35에서 **동작 확인** |
| userspace | kube-proxy 프로세스가 직접 중개 | 오래전에 제거됨 (문서 지식. 이번에 확인하지 않음) |
| kernelspace | Windows 전용 | Windows에서는 이것만 지원 (`--help` 문구) |

**현재 버전의 기본 모드는 iptables다.** 확인한 근거:

```
$ k -n kube-system get cm kube-proxy -o jsonpath='{.data.config\.conf}' | grep ^mode
mode: ""                                                              ← 비어 있으면 기본값 사용
$ k -n kube-system logs ds/kube-proxy        (Docker Desktop, v1.32.2)
"Using iptables proxy"
"Using iptables Proxier"
$ kc -n kube-system exec ds/kube-proxy -- kube-proxy --help | grep proxy-mode        (kind, v1.35.0)
--proxy-mode ProxyMode   Which proxy mode to use: on Linux this can be 'iptables' (default), 'ipvs', or 'nftables'. On Windows the only supported value is 'kernelspace'. ...
```

- **v1.32(Docker Desktop)와 v1.35(kind) 모두 `iptables`가 기본**이다 (v1.35 `--help`의 `'iptables' (default)`).
- 모드는 kube-proxy 설정(`mode`) 또는 `--proxy-mode`로 바꾼다. 이번 실험은 kind의 `networking.kubeProxyMode: "nftables" | "ipvs"`로 모드를 지정해서 클러스터를 새로 만들었다.
- **ipvs는 deprecated**: v1.35.0에서 ipvs 모드로 띄우자 로그에 `The ipvs proxier is now deprecated and may be removed in a future release. Please use 'nftables' instead.`가 찍혔다. 앞으로는 nftables가 대안이다.
- nftables 모드의 도입 시점/GA 버전은 이번에 확인하지 않았다 (v1.35.0에서 `Using nftables Proxier`로 정상 동작하는 것까지 확인).

##### 4-3. iptables 모드: 규칙 구조 (실측)

```
PREROUTING / OUTPUT ──▶ KUBE-SERVICES ──(ClusterIP:port 매칭)──▶ KUBE-SVC-xxxx ──(확률 분기)──▶ KUBE-SEP-xxxx ──▶ DNAT → Pod IP:port
                                  └─(마지막 규칙) KUBE-NODEPORTS ──(nodePort 매칭)──▶ KUBE-EXT-xxxx ──▶ KUBE-SVC-xxxx
POSTROUTING ──▶ KUBE-POSTROUTING (KUBE-MARK-MASQ가 표시한 패킷을 SNAT/마스커레이드)
```

```
$ iptables-save -t nat | grep t7s/web-cip     (노드에서, 주석 생략)
-A KUBE-SERVICES -d 10.111.79.184/32 -p tcp --dport 80   -j KUBE-SVC-RKE2WK4J6DKWIVHM          ← Service 포트마다 체인
-A KUBE-SERVICES -d 10.111.79.184/32 -p tcp --dport 8080 -j KUBE-SVC-AV2XTC756LWXUM3N
-A KUBE-SEP-DMYNZC74BXQTHIXV -s 10.1.2.36/32 -j KUBE-MARK-MASQ                                   ← Pod가 자기 자신을 호출하는 경우 마스커레이드
-A KUBE-SEP-DMYNZC74BXQTHIXV -p tcp -j DNAT --to-destination 10.1.2.36:80                        ← 엔드포인트마다 체인
-A KUBE-SVC-SWOS2UUIQAHHHCSS -m statistic --mode random --probability 0.33333333349 -j KUBE-SEP-QQQL276IY...   ← 3개면 1/3, 1/2, 나머지
$ (NodePort)
-A KUBE-NODEPORTS -p tcp --dport 30996 -j KUBE-EXT-SWOS2UUIQAHHHCSS
-A KUBE-NODEPORTS -d 127.0.0.0/8 -p tcp --dport 30996 -m nfacct --nfacct-name localhost_nps_accepted_pkts -j KUBE-EXT-SWOS2UUIQAHHHCSS
-A KUBE-EXT-SWOS2UUIQAHHHCSS -m comment --comment "masquerade traffic for t7s/web-np:http external destinations" -j KUBE-MARK-MASQ
```

- **Service 포트마다 `KUBE-SVC` 체인, 엔드포인트마다 `KUBE-SEP` 체인**이 생긴다. 분산은 `statistic --mode random --probability`로 **확률 기반**이다 (6장에서 본 대로 균등하지 않을 수 있다).
- 체인의 구조: ① `KUBE-SERVICES`에서 **목적지 IP:포트를 규칙 하나씩 순서대로(선형)** 비교 ② 맞으면 `KUBE-SVC` ③ 확률로 `KUBE-SEP` 선택 ④ DNAT. **NodePort 규칙은 `KUBE-SERVICES`의 마지막 규칙**(`KUBE-NODEPORTS`)이다.

**서비스 수가 늘어나면 (실측)**

Service 300개(각각 엔드포인트 3개)를 추가하고 노드의 nat 규칙을 셌다.

| | 추가 전 | 300개 추가 후 |
|---|---|---|
| nat 테이블 줄 수 | 212 | **4,412** |
| `KUBE-SVC-*` 체인 | 11 | 311 |
| `KUBE-SEP-*` 체인 | - | 926 |
| `KUBE-SERVICES` 규칙 수 | - | **312** |
| 규칙 동기화 횟수 | 117 | 133 (16회 증가) |
| 동기화 평균 시간 | 약 39ms (누적 4.60초 / 117회) | 추가 중 16회는 평균 약 66ms ((5.65-4.60)/16) |
| API 적용 시간 | - | 300개 적용에 2초 |

- 서비스가 늘면 **규칙 수가 서비스 × 엔드포인트에 비례해서 커지고**, `KUBE-SERVICES`에서 ClusterIP를 찾는 것은 **선형 탐색**이다. 이 환경의 300개 규모에서는 동기화가 66ms 수준이었지만 **수천 개 규모에서는 iptables 규칙 갱신(전체를 다시 쓰는 방식)과 패킷당 선형 탐색이 병목**이 되는 것이 iptables 모드의 알려진 한계이고 ipvs/nftables가 나온 이유다 (이 큰 규모는 시험하지 않았다. 300개까지만 확인).
- 서비스를 삭제하니 규칙이 정리되어 nat 줄 수가 81까지 줄었다 (실험 시작 전 212는 실험용 Service가 이미 있던 상태였다).

##### 4-4. nftables 모드 (kind v1.35.0 실측)

```
$ kc -n kube-system get cm kube-proxy -o jsonpath='{.data.config\.conf}' | grep ^mode      →  mode: nftables
$ kc -n kube-system logs ds/kube-proxy | grep Using                                          →  "Using nftables Proxier"
$ docker exec <노드> nft list table ip kube-proxy | grep default/web
10.96.111.214 . tcp . 80 : goto service-PFISGYVC-default/web/tcp/,                ← ClusterIP . 프로토콜 . 포트 → 서비스 체인 (맵 조회)
chain service-PFISGYVC-default/web/tcp/ {
    numgen random mod 2 vmap { 0 : goto endpoint-5V4SALOI-default/web/tcp/__10.244.1.2/80, 1 : goto endpoint-A7VVRXZS-default/web/tcp/__10.244.1.3/80 }
$ docker exec <노드> sh -c 'iptables-save -t nat | grep -c "KUBE-SVC"'                       →  0     ← iptables nat에는 kube-proxy 규칙이 없다
```

- nftables는 **`ClusterIP . 프로토콜 . 포트` 키로 맵(vmap)을 조회**해서 서비스 체인으로 바로 간다 (iptables의 선형 탐색과 다름). 엔드포인트 선택도 **`numgen random mod N vmap`**(난수로 N개 중 하나를 맵에서 선택)이다.
- iptables nat 테이블에는 kube 규칙이 **하나도 없다**(0). 규칙이 별도의 nftables 테이블 `kube-proxy`에 들어 있다.
- 이 환경에서 조회 성능/규모 비교는 하지 않았다 (구조 차이만 확인).

##### 4-5. ipvs 모드 (kind v1.35.0 실측)

```
$ kc -n kube-system logs ds/kube-proxy | grep -iE "Using|deprecated"
"Using ipvs Proxier"
"The ipvs proxier is now deprecated and may be removed in a future release. Please use 'nftables' instead."
$ docker exec <노드> ip -br addr show kube-ipvs0
kube-ipvs0   DOWN   10.96.0.10/32 10.96.0.1/32 10.96.10.244/32          ← 모든 ClusterIP가 더미 인터페이스에 바인딩됨
$ docker exec <노드> sh -c 'iptables-save -t nat | grep -c "KUBE-SVC"'     →  0                    ← 서비스별 iptables 체인이 없다
$ cat /proc/net/ip_vs  (해석)
TCP 10.96.10.244:80  scheduler=rr
    -> 10.244.1.3:80  forward=Masq weight=1
    -> 10.244.1.2:80  forward=Masq weight=1
TCP 10.96.0.10:53  scheduler=rr  -> 10.244.0.3:53, 10.244.0.2:53  (Masq, weight 1)      ← CoreDNS
TCP 10.96.0.1:443  scheduler=rr  -> 192.168.16.2:6443                                    ← kubernetes Service(API 서버)
```

- ipvs는 **ClusterIP를 `kube-ipvs0` 더미 인터페이스에 붙이고**, 커널의 IPVS가 **가상 서버(ClusterIP:포트)와 실제 서버(Pod IP:포트) 테이블**로 분산한다. 서비스별 iptables 체인이 필요 없고(0), 조회는 해시 테이블이다.
- **스케줄러(분산 알고리즘) 선택이 가능**하다. 이번 설정에서는 기본값(`scheduler: ""`)이 **`rr`(round-robin)** 으로 보였다. iptables의 확률 분산과 달리 순서대로 돌아가는 방식이다 (균등성 비교는 시험하지 않았다). 그 외에 `lc`(least connection) 등이 있다는 것은 문서 지식.
- 전달 방식은 `Masq`(NAT)였다. 이 환경에서 `ipvsadm`이 노드 이미지에 없어서(`ipvsadm -Ln` 출력 없음) 위 표는 `/proc/net/ip_vs`를 직접 해석한 것이다.

##### 4-6. 모드 비교

| 항목 | iptables (기본) | ipvs | nftables |
|---|---|---|---|
| 서비스 조회 방식 | 규칙 **선형** 탐색 (이번 실측: `KUBE-SERVICES` 312개) | 커널 **해시 테이블** | **맵(vmap)** 조회 |
| 엔드포인트 선택 | `statistic random` 확률 분기 | 스케줄러 선택 (`rr` 등) | `numgen random vmap` |
| 규칙 갱신 | iptables 규칙 전체를 다시 쓰는 방식 (이번: 300개 규모에서 약 66ms/회) | 가상/실제 서버 단위 갱신 | 규칙 집합 갱신 (이번에 비교 못 함) |
| 서비스 IP가 놓이는 곳 | 어디에도 안 붙음 (규칙으로만) | **`kube-ipvs0` 더미 인터페이스** | 어디에도 안 붙음 |
| 상태 (v1.35) | **기본값** | **deprecated 경고** | 동작 (iptables 후속) |
| 필요한 것 | iptables | 커널 IPVS 모듈 + ipset (이 VM에서 사용 가능했음) | nftables 지원 커널 |
| 특징 | 가장 널리 쓰임, 대규모에서 느려짐 | 대규모에 유리, 다양한 스케줄러 | 대규모에 유리, iptables 대체 방향 |

- 실제 지연/처리량/규모 한계 벤치마크는 하지 않았다. 위 표의 "대규모 유리"는 **구조 차이(선형 vs 해시/맵)** 에서 오는 일반적인 설명이고 이 환경의 측정값이 아니다.
- CNI 플러그인에 따라 kube-proxy 자체를 대체하는 경우도 있다 (예: Cilium의 kube-proxy 대체, eBPF). 이번에 시험하지 않았다.

#### 5. 선택 가이드와 함정

| 상황 | 선택 |
|---|---|
| 클러스터 안 서비스끼리 통신 | **ClusterIP** |
| 임시로 노드 IP로 접속 / 외부 LB의 대상 | **NodePort** |
| 클라우드에서 외부로 공개 (TCP/UDP) | **LoadBalancer** |
| 여러 HTTP 서비스를 하나의 진입점으로 | **인그레스**(8장) + ClusterIP |
| 외부 서비스에 내부 이름 부여 | **ExternalName** (또는 EndpointSlice) |
| StatefulSet의 개별 Pod 접근, 클라이언트 측 분산 | **Headless** |
| 클러스터 밖 IP를 Service로 | **셀렉터 없는 Service + EndpointSlice** |

| 함정 | 근거 |
|---|---|
| `LoadBalancer`의 `EXTERNAL-IP`가 `<pending>` | LB를 만드는 컨트롤러(클라우드/MetalLB)가 없는 환경 (2-4절) |
| NodePort는 **모든 노드**에서 열린다 | 포트가 노출 범위를 넓힌다. `nodePortAddresses`로 제한 가능 (경고 로그) |
| `externalTrafficPolicy: Cluster`는 클라이언트 IP가 사라진다 | 실측: `10.1.0.1`로 SNAT. 클라이언트 IP가 필요하면 `Local` |
| ExternalName에서 HTTP `Host`/TLS 이름이 안 맞는다 | 실측: `http://ext`는 403, `Host: example.com`이면 200 |
| Headless는 로드 밸런싱을 해 주지 않는다 | 클라이언트가 첫 IP만 쓰면 한 Pod로 쏠림, 연결 재사용도 주의 |
| 셀렉터 없는 Service는 Endpoints 오브젝트가 안 보인다 | EndpointSlice를 확인 (`kubectl get endpointslice`) |
| `sessionAffinity`는 소스 IP 기준 | NAT 뒤 클라이언트는 한 Pod로 몰림, 타임아웃 후 다른 Pod 선택 (실측) |
| 서비스가 수천 개일 때 iptables 모드 | 규칙 수와 선형 탐색이 커짐 (300개에서 nat 4,412줄 실측). ipvs(deprecated)보다 nftables 검토 |

#### 6. 확인하지 못한 부분

- **Service 타입 전체**: `externalIPs`, 듀얼 스택(`ipFamilyPolicy: PreferDualStack`), `internalTrafficPolicy: Local`, `loadBalancerSourceRanges`, 실제 클라우드 LoadBalancer의 동작(이 환경은 `<pending>`)과 MetalLB는 시험하지 않았다. `healthCheckNodePort`는 LoadBalancer + `Local`에서 쓰이는 것으로 NodePort 타입에서는 비어 있는 것만 확인했다.
- **externalTrafficPolicy: Local의 실패 조건**: 단일 노드라 "Pod가 없는 노드로 오는 연결이 실패/드롭되는 것"을 확인하지 못했다. 외부에서 들어오는 요청(진짜 외부 클라이언트)의 소스 IP도 시험하지 못했다 (Pod에서 노드 IP로 접속하는 경로로만 확인).
- **ExternalName의 TLS/SNI**, 외부 이름이 바뀔 때 DNS 캐시 동작은 시험하지 않았다. `example.com`이 403을 돌려준 정확한 이유(Host 헤더 불일치로 추정)는 확인하지 않았다.
- **Headless**: 클라이언트(JVM, gRPC)의 DNS 캐시/재조회, 연결 재사용으로 인한 쏠림, `publishNotReadyAddresses`가 클러스터링 앱에 쓰이는 실제 시나리오, 헤드리스 + 셀렉터 없음에서 포트 이름이 SRV에 나오는지는 확인하지 않았다. DNS에서 응답 순서를 섞는 것은 CoreDNS의 `loadbalance` 플러그인 때문이라고 추정했고 플러그인을 끄고 비교해 보지는 않았다.
- **kube-proxy 모드**: iptables와 ipvs/nftables의 **성능 비교**(지연, CPU, 동기화 시간)는 하지 못했다 (iptables만 300개 규모의 규칙 수/동기화 시간을 측정했고 nftables/ipvs는 구조만 확인했다). nftables의 도입/GA 버전, ipvs 제거 시점, `userspace` 모드 제거는 문서 지식이다. kube-proxy를 대체하는 CNI(Cilium eBPF)는 확인하지 않았다.
- **ipvs 세부**: `ipvsadm`/`ipset`이 노드 이미지에 없어서 `/proc/net/ip_vs`로만 확인했다. 스케줄러 변경(`scheduler`), `strictARP`, 연결 타임아웃은 시험하지 않았다. 기본 스케줄러가 `rr`인 것은 `scheduler: ""` 설정에서 관찰한 값이다.
- **Docker Desktop 클러스터**는 kube-proxy 로그에서 `Using iptables proxy`를 확인했고, `iptables`가 기본임은 v1.35 `--help`로 확인했다. 그 사이 버전(v1.33, v1.34)의 기본값은 확인하지 못했다.
- **패킷 단위 확인**: 규칙은 `iptables-save`/`nft list`/`/proc/net/ip_vs`로 확인했고, 실제 패킷이 어떤 규칙을 지나가는지(카운터 증가, `conntrack`)는 추적하지 않았다.
- **세션 어피니티와 외부 트래픽 정책이 동시에 켜진 경우**, `conntrack` 항목 수/타임아웃 영향은 확인하지 않았다.
