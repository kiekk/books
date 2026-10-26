### 16장. 쿠버네티스 DNS

> 정리 범위: CoreDNS 질의 구조, 쿠버네티스 도메인 네이밍 규칙
>
> 실측 환경: macOS (Apple Silicon), Docker Desktop. 이 장부터 19장까지는 **임시 kind 클러스터 하나(`t16`, v1.35.0, control-plane 1 + worker 1, 별도 kubeconfig)** 를 같이 썼다(17~19장 노트에 이어서 사용 표시). CoreDNS는 v1.13.1이다. 질의 로그를 보려고 이 클러스터의 CoreDNS ConfigMap에 `log` 플러그인을 추가하고 replicas를 1로 줄였다(질의가 한 곳에만 쌓이게 하려고). 클라이언트는 `nicolaka/netshoot` Pod(`dig`, `getent`)이고, 네임스페이스는 `t16`, `t16o`다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: 헤드리스 Service와 Pod별 DNS는 [7장](../chapter07/README.md)에서 Docker Desktop 환경으로 확인했다. 여기서는 CoreDNS의 구조와 질의가 오가는 과정, 이름 규칙 전체를 정리하고 7장과 겹치는 결과는 짧게 적었다.

#### 목차

1. 쿠버네티스 DNS 구조
2. CoreDNS의 Corefile
3. Pod의 resolv.conf와 질의 과정
4. 도메인 네이밍 규칙
5. dnsPolicy와 dnsConfig
6. Corefile 커스터마이징
7. 선택 가이드와 함정
8. 확인하지 못한 부분

---

#### 1. 쿠버네티스 DNS 구조

```
Pod ──(UDP/TCP 53)──▶ kube-dns Service (ClusterIP 10.96.0.10) ──▶ CoreDNS Pod들 ──▶ 쿠버네티스 API (Service/EndpointSlice/Pod watch)
                                                                      └─(클러스터 도메인이 아니면)─▶ 상위 DNS(노드의 /etc/resolv.conf)
```

- 클러스터 DNS 서버는 **CoreDNS**(kube-system의 Deployment)이고 Service 이름은 `kube-dns`(ClusterIP는 이 클러스터에서 `10.96.0.10`, 포트 53/UDP, 53/TCP, 9153/TCP(메트릭))다. Service 이름이 `kube-dns`로 남아 있는 것은 이전 구현의 이름을 유지한 것이다.
- CoreDNS는 **Service, EndpointSlice, Pod를 watch** 해서 메모리 상에 DNS 레코드를 만든다. 레코드가 파일로 저장되는 것은 아니다.
- 이 클러스터의 CoreDNS 구성: Deployment replicas 2(실험 때 1로 조정), 이미지 `registry.k8s.io/coredns/coredns:v1.13.1`.

---

#### 2. CoreDNS의 Corefile

kind의 기본 Corefile(ConfigMap `coredns`):

```
.:53 {
    errors
    health { lameduck 5s }
    ready
    kubernetes cluster.local in-addr.arpa ip6.arpa {
       pods insecure
       fallthrough in-addr.arpa ip6.arpa
       ttl 30
    }
    prometheus :9153
    forward . /etc/resolv.conf { max_concurrent 1000 }
    cache 30 {
       disable success cluster.local
       disable denial cluster.local
    }
    loop
    reload
    loadbalance
}
```

| 플러그인 | 역할 |
|---|---|
| `kubernetes` | `cluster.local`과 역방향(`in-addr.arpa`) 존의 질의를 **쿠버네티스 데이터로 응답**. `pods insecure`: Pod의 IP 형태 레코드 허용(존재 검증 안 함), `ttl 30` |
| `forward . /etc/resolv.conf` | 클러스터 밖의 이름은 **노드의 resolv.conf에 있는 상위 DNS로 전달** |
| `cache 30` | 응답을 최대 30초 캐시. 단 **`cluster.local` 응답은 캐시하지 않는다**(`disable success/denial cluster.local`) |
| `loop` | 질의 루프 감지 |
| `reload` | Corefile 변경 자동 반영 |
| `loadbalance` | 응답의 A 레코드 순서를 섞음 (7장에서 headless 응답이 섞이던 이유) |
| `errors`, `health`, `ready`, `prometheus` | 오류 로그, 헬스 체크(8080 `/health`), 준비 상태, 메트릭 |

- **`reload`의 반영 시간**: Corefile ConfigMap을 수정한 뒤 Pod에 반영되는 데 시간이 걸린다(ConfigMap 볼륨 동기화 + reload 주기). `log`를 추가한 뒤 한 CoreDNS Pod는 곧 `Reloading`됐지만 다른 Pod는 더 늦게 반영돼서, 실험은 replicas를 1로 줄인 뒤 했다. 스텁 도메인 블록을 추가했을 때는 첫 확인(약 40초 후)에서 아직 NXDOMAIN, 이후 확인한 첫 시점(15초 단위로 폴링 중 첫 시도)에 답이 나왔다. 정확한 시간은 측정하지 않았다.

---

#### 3. Pod의 resolv.conf와 질의 과정

##### 3-1. Pod의 resolv.conf (실측, 기본 `dnsPolicy: ClusterFirst`)

```
search t16.svc.cluster.local svc.cluster.local cluster.local
nameserver 10.96.0.10
options ndots:5
```

- `nameserver`: kube-dns Service의 ClusterIP.
- `search`: **`<네임스페이스>.svc.cluster.local`, `svc.cluster.local`, `cluster.local`**. 이 목록 덕분에 `web`, `web.t16`, `web.t16.svc`처럼 줄여 쓸 수 있다.
- `options ndots:5`: **이름에 점이 5개 미만이면 search 목록을 먼저 붙여 시도**한다는 뜻.

##### 3-2. 질의 흐름 (실측: CoreDNS `log` 플러그인)

클라이언트가 `getent ahosts <이름>`을 실행할 때 CoreDNS에 도달한 질의(각 이름마다 **A와 AAAA 두 번**씩 보냈다):

| 조회한 이름 | CoreDNS가 받은 이름 (순서) | 질의 수 |
|---|---|---|
| `web` | `web.t16.svc.cluster.local.` → NOERROR | **2** (A, AAAA) |
| `web.t16.svc.cluster.local` (FQDN, 점 4개) | `….t16.svc.cluster.local.` NXDOMAIN, `….svc.cluster.local.` NXDOMAIN, `….cluster.local.` NXDOMAIN, 마지막에 원래 이름 NOERROR | **8** (실패 6 + 성공 2) |
| `example.com` (점 1개) | `example.com.t16.svc.cluster.local.` NXDOMAIN, `example.com.svc.cluster.local.` NXDOMAIN, `example.com.cluster.local.` NXDOMAIN, 마지막에 `example.com.` → 상위로 전달 NOERROR | **8** |
| `example.com.` (끝에 점) | `example.com.` 만 | **2** (이후 캐시 hit로 `aa` 플래그) |
| `www.google.com` (점 2개) | search 3개 NXDOMAIN 후 `www.google.com.` | 8 이상 (응답이 느려 재시도 관찰됨) |

- **점이 5개 미만이면 search 목록을 먼저 전부 시도**하므로, 클러스터 밖의 이름(`example.com`)과 이미 완전한 이름(`web.t16.svc.cluster.local`, 점 4개)도 **불필요한 NXDOMAIN 질의가 6번** 먼저 간다. 끝에 점(`.`)을 붙이면(`example.com.`) search 없이 바로 질의한다.
- **CoreDNS 입장에서의 응답 플래그**: 클러스터 이름은 `aa`(authoritative), 상위로 전달한 이름은 `ra`가 붙는다(`qr,rd,ra`). 같은 이름을 곧바로 다시 질의하면 캐시된 응답으로 `aa,ra`(캐시 서빙)가 나왔다.
- 이름 해석 라이브러리에 따라 A/AAAA를 병렬로 보내거나 순서가 다를 수 있다. 이 실험의 클라이언트(netshoot)는 Alpine(musl) 기반이다.
- 외부 이름 질의(`example.com` AAAA)는 이 환경의 상위 DNS(`192.168.65.254`)에서 `i/o timeout`이 한 번 났다(CoreDNS `errors` 로그 `read udp … i/o timeout`). 이 환경의 상위 DNS 특성이며 CoreDNS 구조와는 무관하다.

##### 3-3. 응답 속도와 부하

`example.com`을 한 번 조회하는 데 CoreDNS가 처리하는 질의가 8개다. 트래픽이 많은 앱이 외부 호스트를 자주 조회하면 DNS 부하가 커질 수 있다. 줄이는 방법은 5절(`ndots`, FQDN 끝 점)이다.

---

#### 4. 도메인 네이밍 규칙

기본 클러스터 도메인은 `cluster.local`이다(변경 가능한 값이며 이 클러스터에서는 기본값).

##### 4-1. 이름 형식 (실측)

| 대상 | 형식 | 확인한 결과 |
|---|---|---|
| Service (ClusterIP) | `<svc>.<ns>.svc.cluster.local` | `web.t16.svc.cluster.local` → `10.96.103.206` |
| Headless Service | 같은 형식, **Ready Pod IP 전부** | `web-hl.t16.svc.cluster.local` → `10.244.1.2 10.244.1.3` |
| 특정 Pod (헤드리스 + hostname/subdomain) | `<hostname>.<subdomain>.<ns>.svc.cluster.local` | 아래 4-3 |
| Pod IP 기반 | `<ip를 -로>.<ns>.pod.cluster.local` | `10-244-1-3.t16.pod.cluster.local` → `10.244.1.3` |
| SRV | `_<포트이름>._<프로토콜>.<svc>.<ns>.svc.cluster.local` | 아래 4-4 |
| ExternalName | CNAME으로 응답 | 아래 4-5 |
| 역방향(PTR) | `<역순IP>.in-addr.arpa` | `dig -x 10.96.103.206` → `web.t16.svc.cluster.local.` |

##### 4-2. 줄여 쓰는 이름 (같은 Pod에서 `search`로 확장)

| 입력 | 결과 (client in `t16`) |
|---|---|
| `web` | `10.96.103.206` (t16의 web) |
| `web.t16`, `web.t16.svc`, `web.t16.svc.cluster.local` | 같은 주소 |
| `web.t16o` | **다른 네임스페이스**의 web (`10.96.54.120`) |
| `kubernetes.default` | `10.96.0.1` (API 서버 Service) |
| `nosuch` | 응답 없음 (NXDOMAIN) |
| `web.t16.svc.cluster.local.` (끝에 점) | 같은 주소, search 없이 바로 |

- **같은 네임스페이스는 서비스 이름만**, **다른 네임스페이스는 `<svc>.<ns>`** 로 호출한다.
- Pod의 네임스페이스가 search 목록 첫 번째이므로 `web`은 항상 **자기 네임스페이스의 web**으로 풀린다(`t16o`에도 `web`이 있지만 `t16`에서는 `t16`의 것).
- Pod IP 기반 이름은 **네임스페이스가 실제와 달라도 응답한다**: `10-244-1-3.default.pod.cluster.local`도 `10.244.1.3`으로 풀렸다(`pods insecure`: 존재와 네임스페이스를 검증하지 않음).

##### 4-3. StatefulSet/헤드리스에서의 Pod 개별 DNS (실측과 함정)

- Pod의 `spec.hostname`(기본은 Pod 이름)과 `spec.subdomain`이 있고, **그 subdomain과 같은 이름의 headless Service가 해당 Pod를 selector로 선택**해야 `<hostname>.<subdomain>.<ns>.svc.cluster.local` 레코드가 생긴다.
- 처음에 StatefulSet `db`의 `serviceName: web-hl`로 만들었지만 `web-hl`의 selector는 `app=web`이라 db Pod를 선택하지 않았다. 결과: **`db-0.web-hl.t16.svc.cluster.local`은 빈 응답**이었다. Pod는 `hostname=db-0 subdomain=web-hl`이고 `/etc/hosts`에도 `db-0.web-hl.t16.svc.cluster.local`로 적혀 있었는데도 DNS에는 없었다.
- `db-hl`(selector `app=db`)을 새로 만들어도 `serviceName`이 **StatefulSet 생성 후 변경 불가**여서(`updates to statefulset spec for fields other than … are forbidden`) 기존 db Pod의 `subdomain`은 그대로 `web-hl`이었고, `db-0.db-hl…`은 여전히 빈 응답이었다(`db-hl.t16.svc.cluster.local` 자체는 Pod IP 두 개로 풀림).
- 교훈: StatefulSet의 `serviceName`은 **그 Pod를 선택하는 headless Service의 이름**이어야 하고 한 번 정하면 못 바꾼다(재생성 필요). (7장에서 선택자가 맞는 환경으로는 `db-primary.named-hl…`이 동작했다.)

##### 4-4. SRV 레코드

```
$ dig +short SRV _http._tcp.web.t16.svc.cluster.local
0 100 80 web.t16.svc.cluster.local.
$ dig +short SRV _http._tcp.web-hl.t16.svc.cluster.local
0 50 80 10-244-1-3.web-hl.t16.svc.cluster.local.
0 50 80 10-244-1-2.web-hl.t16.svc.cluster.local.
```

- 포트에 **이름이 있어야**(`name: http`) SRV가 생긴다. ClusterIP Service는 하나의 레코드(서비스 자신), headless는 Pod마다 하나이며 호스트 이름이 **IP 기반 이름**(`10-244-1-3.web-hl…`)이다(Pod에 hostname이 지정되지 않았기 때문).

##### 4-5. ExternalName

```
$ dig ext.t16.svc.cluster.local
ext.t16.svc.cluster.local. 30 IN CNAME example.com.
example.com.              30 IN A 172.66.147.243 / 104.20.23.154
```

CNAME만 돌려주므로 CoreDNS가 상위로 질의해 최종 A까지 따라간다. (Service로서의 동작은 7장)

---

#### 5. dnsPolicy와 dnsConfig

##### 5-1. dnsPolicy (실측)

| 정책 | resolv.conf | `web` 해석 |
|---|---|---|
| `ClusterFirst` (기본) | search 3개, nameserver `10.96.0.10`, ndots:5 | 성공 |
| `Default` | **노드(여기서는 Docker VM)의 resolv.conf**: `nameserver 192.168.65.254`, `ndots:0` | **실패** (클러스터 이름을 모름) |
| `None` + `dnsConfig` | 지정한 값만: `nameserver 1.1.1.1`, `search my.search`, `ndots:2` | **실패** (클러스터 DNS 아님) |
| `hostNetwork: true` (정책 생략) | **노드의 resolv.conf**로 동작(`spec.dnsPolicy`는 `ClusterFirst`로 저장돼 있었지만 실제 resolv.conf는 `192.168.65.254`) | 실패 |
| `hostNetwork: true` + **`ClusterFirstWithHostNet`** | 클러스터 DNS 사용 (search 3개, ndots:5) | 성공 |

- **`dnsPolicy: Default`는 "기본값"이 아니라 "노드의 DNS를 쓴다"는 뜻이다.** 기본값은 `ClusterFirst`다.
- `hostNetwork: true`인 Pod는 클러스터 DNS를 쓰려면 `ClusterFirstWithHostNet`이 필요하다.

##### 5-2. dnsConfig로 ndots 조정 (실측)

`dnsConfig.options: [{name: ndots, value: "1"}]`로 만든 Pod:

| 조회 | 결과 |
|---|---|
| `example.com` | CoreDNS에 `example.com.`만 질의(search 시도 없음) → 8개에서 **2개**로 감소 |
| `web` (점 0개) | 성공 (search 적용) |
| `web.t16` (점 1개) | **실패** (`exit code 2`) |

- `ndots`를 낮추면 외부 이름 조회는 빨라지지만 **`<svc>.<ns>` 형태의 이름이 안 풀릴 수 있다.** 이 실험의 클라이언트(musl 기반)에서는 점이 `ndots` 이상이면 search를 아예 붙이지 않았다. glibc는 절대 이름 질의가 실패한 뒤 search를 시도하는 등 동작이 다를 수 있다(확인하지 않음). 서비스를 `<svc>.<ns>.svc`처럼 길게 쓰거나 FQDN으로 쓰는 편이 안전하다.

---

#### 6. Corefile 커스터마이징 (스텁 도메인 예)

사내 도메인(`corp.example`)에 대한 응답을 따로 주는 서버 블록을 추가했다(`hosts` 플러그인 사용).

```
corp.example:53 {
    errors
    cache 30
    hosts { 10.9.9.9 db.corp.example
            fallthrough }
}
```

```
$ dig +short db.corp.example         → 10.9.9.9              (반영 후)
$ dig other.corp.example             → status: SERVFAIL      (hosts에 없고 이 블록에는 forward가 없음)
```

- 반영 전에는 `NXDOMAIN`이었고 `reload`가 끝난 뒤 답이 나왔다(정확한 지연 시간은 잴 수 없었음, 15초 단위 확인에서 첫 시도부터 응답).
- 도메인별로 다른 상위 DNS로 보내려면 `forward corp.example 10.0.0.2` 형태를 쓰는 것이 일반적이다(이 실험에서는 시험하지 않음). EKS 등 관리형 환경에서는 애드온이 Corefile을 덮어쓸 수 있어 직접 수정이 유지되는지 확인이 필요하다(확인 안 함).

---

#### 7. 선택 가이드와 함정

| 질문 | 답 |
|---|---|
| 같은 네임스페이스의 서비스 호출 | `http://web` |
| 다른 네임스페이스의 서비스 | `http://web.other` 또는 `web.other.svc.cluster.local` |
| 외부 도메인이 느리다/DNS 부하 | 끝에 점(`example.com.`), `ndots` 조정, 로컬 캐시 (NodeLocal DNSCache 등은 이번에 시험 안 함) |
| StatefulSet의 Pod별 이름 | headless Service(이름 = `serviceName`, selector가 Pod와 일치) |
| 호스트 네트워크 Pod에서 서비스 이름 | `dnsPolicy: ClusterFirstWithHostNet` |
| 클러스터 DNS를 안 쓰는 Pod | `dnsPolicy: None` + `dnsConfig` |
| SRV로 포트까지 | 포트에 `name` 지정 |
| 클러스터 내 레코드가 캐시되나 | CoreDNS는 `cluster.local` 응답을 캐시하지 않음(`ttl 30`은 클라이언트가 지킬 TTL) |

함정:

- **점 4개짜리 FQDN도 `ndots:5` 때문에 search를 먼저 거친다**(실측: 8개 질의).
- Pod DNS 이름(`pod.cluster.local`)은 존재 검증 없이 응답한다.
- StatefulSet `serviceName`은 만든 뒤 못 바꾼다. 선택자가 안 맞으면 Pod 개별 DNS가 안 생긴다(조용히).
- CoreDNS의 Corefile 변경은 바로 안 먹는다(reload 지연, replicas별로 다를 수 있음).
- 외부 질의의 상위 DNS 타임아웃은 앱의 DNS 지연으로 나타난다.

---

#### 8. 확인하지 못한 부분

- **NodeLocal DNSCache**, CoreDNS의 `autopath`(search 반복 줄이기), CoreDNS 확장(`cluster-proportional-autoscaler`) 같은 운영 최적화는 시험하지 않았다.
- **glibc 기반 이미지**에서의 `ndots`/search 동작 차이는 확인하지 못했다(netshoot은 musl).
- **다른 클러스터 도메인**(`cluster.local` 외) 설정, `kubelet --cluster-domain`/`--cluster-dns`는 확인하지 않았다.
- **CoreDNS 장애/스케일** 시의 질의 실패, 부하 시험, `conntrack` 경합으로 인한 DNS 5초 지연 같은 알려진 문제는 재현하지 않았다.
- **IPv6/듀얼스택**의 AAAA 응답은 `NOERROR` 응답으로만 확인했고 내용은 보지 않았다.
- **EKS의 CoreDNS(애드온)**, 다른 클라우드의 상위 DNS 구성은 확인하지 못했다.
- 스텁 도메인의 `forward` 방식과 `rewrite` 플러그인은 시험하지 않았다. `hosts` 블록의 **정확한 reload 지연 시간**도 측정하지 않았다.
- `StatefulSet`의 **정상 케이스**(selector가 맞는 headless Service로 `db-0.<svc>`가 풀리는 것)는 이 클러스터에서 다시 확인하지 못했다(7장에서 다른 환경으로 확인).
- ExternalName 뒤 상위 서버 응답의 TTL, CNAME 체인 길이 제한은 확인하지 않았다.
