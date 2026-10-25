### 8장. 인그레스(Ingress)

> 정리 범위: 인그레스의 개념과 구조, 인그레스 컨트롤러의 동작 원리, Watch API
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드), kubectl v1.35.2, **ingress-nginx 컨트롤러 v1.12.1** (공식 `cloud/deploy.yaml`을 이 실험을 위해 설치했고, 끝나고 매니페스트째 삭제하고 이미지도 지웠다)
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`을 명시했다 (아래에서는 `k`로 줄여 적음). 실험은 `t8` 네임스페이스와 `ingress-nginx` 네임스페이스에서만 했다.

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. 인그레스 개념과 구조
2. 인그레스 컨트롤러 동작 원리
3. Watch API
4. 확인하지 못한 부분

---

#### 1. 인그레스 개념과 구조

##### 1-1. 왜 필요한가

6장의 Service만으로 외부에 노출하려면 `NodePort`나 `LoadBalancer`를 쓴다. 그런데 서비스마다 포트 또는 로드 밸런서를 하나씩 쓰게 되고, L4(IP/포트) 단위라서 **호스트 이름이나 URL 경로로 나누는 라우팅, TLS 종료** 같은 L7 기능이 없다.

**Ingress는 "HTTP(S) 요청을 호스트와 경로 기준으로 어느 Service에 보낼지"를 적은 규칙 오브젝트**다.

```
                     외부 요청 (Host: a.example.test, b.example.test, ...)
                              │
                    ┌─────────▼─────────┐
                    │ 인그레스 컨트롤러  │  ← Ingress 규칙을 읽어서 실제로 라우팅하는 프록시 (nginx 등)
                    └──┬──────┬──────┬──┘
        a.example.test │      │      │ multi.example.test/b
                  ┌────▼──┐ ┌─▼─────┐ ┌▼──────┐
                  │app-a  │ │app-b  │ │app-b  │   Service → Pod
                  └───────┘ └───────┘ └───────┘
```

- 포트 하나(80/443)로 여러 서비스를 호스트/경로로 나눠 받을 수 있다.
- **Ingress 오브젝트는 규칙일 뿐이고, 직접 트래픽을 처리하지 않는다.** 규칙을 읽고 실행하는 **인그레스 컨트롤러**를 별도로 설치해야 한다. 쿠버네티스에 기본으로 포함되어 있지 않다 (이 Docker Desktop 클러스터에는 `ingressclass`가 하나도 없었고, 컨트롤러를 설치한 뒤에야 `nginx` 클래스가 생겼다).

##### 1-2. 구조: Ingress 오브젝트의 필드

실험에 쓴 Ingress (`networking.k8s.io/v1`):

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata: {name: demo, namespace: t8}
spec:
  ingressClassName: nginx            # 어느 컨트롤러가 처리할지
  defaultBackend:                    # 어떤 규칙에도 맞지 않을 때 보낼 곳
    service: {name: app-def, port: {number: 80}}
  rules:
  - host: a.example.test             # ① 호스트 기반
    http:
      paths:
      - {path: /, pathType: Prefix, backend: {service: {name: app-a, port: {number: 80}}}}
  - host: b.example.test
    http:
      paths:
      - {path: /, pathType: Prefix, backend: {service: {name: app-b, port: {number: 80}}}}
  - host: multi.example.test         # ② 경로 기반
    http:
      paths:
      - {path: /a, pathType: Prefix, backend: {service: {name: app-a, port: {number: 80}}}}
      - {path: /b, pathType: Prefix, backend: {service: {name: app-b, port: {number: 80}}}}
```

| 필드 | 의미 |
|---|---|
| `ingressClassName` | 이 Ingress를 처리할 컨트롤러를 가리키는 `IngressClass` 이름 |
| `rules[].host` | 요청의 `Host` 헤더와 비교 (생략하면 모든 호스트) |
| `rules[].http.paths[]` | 경로 규칙. `path`, `pathType`, `backend` |
| `backend.service` | 트래픽을 보낼 **같은 네임스페이스의** Service 이름과 포트 |
| `defaultBackend` | 규칙에 안 맞는 요청을 처리할 백엔드 |
| `tls[]` | TLS 인증서가 든 Secret과 적용할 호스트 (1-5절) |

- Ingress의 backend는 **Ingress와 같은 네임스페이스의 Service**만 가리킬 수 있다 (`backend.service`가 이름만 받고 네임스페이스 필드가 없다).
- `IngressClass`: 한 클러스터에 컨트롤러가 여럿 있을 수 있어서(예: nginx, ALB) 어느 쪽이 처리할지 정하는 연결 고리다.

##### 1-3. 호스트 기반과 경로 기반 라우팅 (실측)

컨트롤러의 Service를 `kubectl port-forward`로 `localhost:18080`에 연결하고 `Host` 헤더를 바꿔 요청했다 (이 환경에서는 `LoadBalancer`가 `<pending>`이라 외부 주소가 없어서 6장에서처럼 port-forward를 썼다. 그래서 `k get ingress`의 `ADDRESS`도 비어 있다).

```
$ curl -H 'Host: a.example.test' localhost:18080/          → app-a ... [200]
$ curl -H 'Host: b.example.test' localhost:18080/          → app-b ... [200]
$ curl -H 'Host: a.example.test' localhost:18080/sub/      → app-a sub [200]
```

- **호스트 규칙**: 같은 IP/포트로 들어와도 `Host` 헤더에 따라 다른 Service로 갔다.
- 규칙에 없는 호스트(`zzz.example.test`)나 호스트 없이 IP로 직접 요청하면 컨트롤러가 `404`를 반환했다. 이 404는 백엔드가 아니라 **컨트롤러(nginx)가 낸 것**이다. 백엔드 nginx의 404 페이지 하단에는 `nginx/1.xx` 버전이 있고, 컨트롤러의 404에는 `nginx`만 있어서 구분할 수 있었다.
- 위 Ingress에는 `defaultBackend: app-def`를 지정했는데도 알 수 없는 호스트의 요청은 `app-def`가 아니라 컨트롤러의 기본 404로 처리되었다. 관찰한 사실만 적고, 이유(호스트 규칙이 있는 Ingress에서 `defaultBackend`가 적용되는 조건)는 확인하지 못했다.

##### 1-4. `pathType`과 경로 매칭 (실측)

`pt.example.test`에 `/exact`(Exact → app-a)와 `/pre`(Prefix → app-b)를 정의하고 경로를 바꿔 요청했다. 표의 "라우팅됨"은 백엔드의 404 페이지(`nginx/1.xx`)가 나온 경우이고, "안 됨"은 컨트롤러의 404(`nginx`)가 나온 경우다. 백엔드에는 해당 파일이 없어 404가 나왔지만 어느 쪽이 응답했는지로 라우팅 여부를 판별했다.

| 요청 경로 | 규칙 | 결과 |
|---|---|---|
| `/exact` | Exact `/exact` | 백엔드로 라우팅됨 |
| `/exact/` | Exact `/exact` | **안 됨** |
| `/exact/x` | Exact `/exact` | **안 됨** |
| `/pre` | Prefix `/pre` | 라우팅됨 |
| `/pre/` | Prefix `/pre` | 라우팅됨 |
| `/pre/sub/` | Prefix `/pre` | 라우팅됨 |
| `/prefix` | Prefix `/pre` | **안 됨** (문자열이 아니라 `/` 단위 요소로 비교) |

| pathType | 의미 |
|---|---|
| `Exact` | 경로가 정확히 일치 (뒤의 `/`도 구분) |
| `Prefix` | `/`로 나눈 경로 요소 단위의 접두어 일치. `/pre`는 `/pre/sub`는 포함하지만 `/prefix`는 포함하지 않는다 |
| `ImplementationSpecific` | 컨트롤러 구현에 맡김 (ingress-nginx는 정규식 사용 가능, 아래 rewrite 참고) |

- API 서버가 `pathType` 값을 검증한다. `Wrong`을 넣으면 `Unsupported value: "Wrong": supported values: "Exact", "ImplementationSpecific", "Prefix"`로 거부되었다.

**경로는 기본적으로 그대로 백엔드에 전달된다.** `multi.example.test/a`로 요청하면 app-a가 받는 경로도 `/a`다. 백엔드 로그에는 `GET /a HTTP/1.1" 404`가 남았고(app-a에는 `/a` 파일이 없음), 응답도 백엔드의 404였다. 경로 접두어를 떼고 전달하려면 rewrite가 필요하다.

**rewrite (ingress-nginx 어노테이션)**

```yaml
metadata:
  annotations:
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rewrite-target: /$2
spec:
  rules:
  - host: rw.example.test
    http:
      paths:
      - {path: "/a(/|$)(.*)", pathType: ImplementationSpecific, backend: ...app-a}
      - {path: "/b(/|$)(.*)", pathType: ImplementationSpecific, backend: ...app-b}
```

```
GET rw.example.test/a/      → app-a (루트 페이지)  [200]
GET rw.example.test/b/sub/  → app-b sub           [200]
GET rw.example.test/a/sub/  → app-a sub           [200]
```

- 어노테이션(`nginx.ingress.kubernetes.io/...`)은 **ingress-nginx 컨트롤러 전용 확장**이다. 다른 컨트롤러(ALB 등)에서는 쓸 수 없다. Ingress 스펙이 표준으로 제공하는 기능은 호스트/경로/TLS 정도이고 나머지는 컨트롤러별 어노테이션이나 설정에 의존한다.

##### 1-5. TLS 종료

TLS 인증서를 Secret(`kubernetes.io/tls`, 7장)으로 만들고 Ingress의 `tls`에 연결했다.

```yaml
spec:
  ingressClassName: nginx
  tls:
  - hosts: [tls.example.test]
    secretName: tls-cert
  rules: [...]
```

```
$ curl -H 'Host: tls.example.test' localhost:18080/          # HTTP로 요청
308 -> https://tls.example.test/                              # 자동으로 HTTPS로 리다이렉트

$ curl -k --resolve tls.example.test:18443:127.0.0.1 https://tls.example.test:18443/
app-b ... [200]                                               # 컨트롤러가 TLS를 풀고 백엔드에는 평문 HTTP
$ openssl s_client -servername tls.example.test ... | openssl x509 -noout -subject -issuer
subject=CN=tls.example.test  issuer=CN=tls.example.test       # 우리가 만든 자체 서명 인증서
$ openssl s_client -servername other.example.test ...
subject=O=Acme Co, CN=Kubernetes Ingress Controller Fake Certificate   # 매칭되는 인증서가 없는 호스트
```

- TLS 인증서를 컨트롤러 한 곳에서 관리하고 백엔드 Pod는 평문 HTTP만 신경 쓰면 된다 (TLS termination).
- `tls`가 있는 호스트는 HTTP 요청이 **308로 HTTPS 리다이렉트**되었다 (ingress-nginx의 기본 동작).
- SNI가 맞지 않는 호스트에는 컨트롤러 내장 가짜 인증서(`Fake Certificate`)를 낸다.

##### 1-6. 잘못 설정했을 때

| 상황 | 결과 (실측) |
|---|---|
| `ingressClassName`을 생략 | 컨트롤러가 **무시**. 컨트롤러 로그에 `Ignoring ingress because of error while validating ingress class ... ingress does not contain a valid IngressClass`. 요청은 404, `CLASS`는 `<none>`. (이 클러스터에는 기본 IngressClass가 없는 상태에서 확인) |
| 백엔드 Service가 없음 (`ghost`) | 요청은 **503**. `describe ingress`에 `ghost:80 (<error: services "ghost" not found>)` |
| 같은 호스트/경로를 다른 Ingress가 이미 정의 | 컨트롤러의 **어드미션 웹훅이 생성 자체를 거부**: `admission webhook "validate.nginx.ingress.kubernetes.io" denied the request: host "a.example.test" and path "/" is already defined in ingress t8/demo` |
| `pathType`에 잘못된 값 | API 서버가 거부 (위 참고) |

- 어드미션 웹훅이 있어서 **설정이 nginx에 반영되기 전에** 충돌을 막아 준다. 웹훅이 없다면 충돌하는 설정이 nginx 설정 검증에서 실패해 반영되지 않을 수 있는데, 그 경우는 확인하지 않았다.

#### 2. 인그레스 컨트롤러 동작 원리

##### 2-1. 컨트롤러는 무엇인가

**Ingress 규칙을 읽어서 실제 프록시(nginx, HAProxy, Envoy 등)의 설정으로 바꿔 주고, 그 프록시를 실행하는 Pod**다. 6장에서 본 "컨트롤러 = 원하는 상태와 현재 상태를 맞추는 반복 루프"의 한 종류인데, 맞추는 대상이 "클러스터 안 프록시의 설정"이다.

| 구분 | 인그레스 컨트롤러 | 6장 컨트롤러(replicaset 등) |
|---|---|---|
| 위치 | 클러스터 안의 **일반 Pod** (Deployment로 배포) | kube-controller-manager 프로세스 안 |
| 설치 | 사용자가 설치 | 기본 포함 |
| 감시 대상 | Ingress, Service, EndpointSlice, Secret, ConfigMap | Deployment, ReplicaSet 등 |
| 하는 일 | 프록시 설정 생성/갱신 | 오브젝트(Pod 등) 생성/삭제 |

##### 2-2. ingress-nginx 구성 (이 환경에서 확인)

```
$ k apply -f deploy.yaml                      # 공식 매니페스트 (v1.12.1)
namespace/ingress-nginx, serviceaccount, role/clusterrole(+binding), configmap/ingress-nginx-controller,
service/ingress-nginx-controller (LoadBalancer), service/ingress-nginx-controller-admission,
deployment.apps/ingress-nginx-controller, job.batch/ingress-nginx-admission-create|patch,
ingressclass.networking.k8s.io/nginx, validatingwebhookconfiguration.../ingress-nginx-admission
```

| 구성 요소 | 역할 |
|---|---|
| Deployment `ingress-nginx-controller` | 컨트롤러 Pod. 안에 컨트롤러 프로세스와 nginx가 같이 들어 있다 |
| Service `ingress-nginx-controller` | 외부에서 들어오는 80/443을 컨트롤러 Pod로 (`LoadBalancer`, 여기서는 `<pending>`) |
| Service `...-admission` | API 서버가 어드미션 웹훅을 호출하는 경로 |
| `IngressClass nginx` | `ingressClassName: nginx`가 가리키는 대상 |
| ClusterRole/Binding | 컨트롤러가 모든 네임스페이스의 Ingress/Service/Endpoints/Secret을 읽을 권한 |
| `ValidatingWebhookConfiguration` | Ingress 생성/수정 시 검증 (1-6절의 중복 거부) |
| Job `admission-create/patch` | 웹훅용 인증서를 만들어 Secret과 웹훅 설정에 넣는 일회성 작업 (둘 다 `Completed`) |

**컨트롤러 Pod 안의 프로세스**

```
$ k -n ingress-nginx exec <pod> -- ps -o pid,ppid,args
  1   0  /usr/bin/dumb-init -- /nginx-ingress-controller --publish-service=ingress-nginx/ingress-nginx-controller
                --election-id=ingress-nginx-leader --controller-class=k8s.io/ingress-nginx --ingress-class=nginx
                --configmap=ingress-nginx/ingress-nginx-controller --validating-webhook=:8443 ...
  7   1  /nginx-ingress-controller --publish-service=... (같은 인자)
 24   7  nginx: master process /usr/bin/nginx -c /etc/nginx/nginx.conf
1233 24  nginx: worker process      (이후 8개 이상. 출력을 12줄에서 잘랐다)
```

- **컨트롤러 프로세스(`/nginx-ingress-controller`, PID 7)가 nginx master(PID 24)의 부모**다. 컨트롤러가 nginx를 직접 띄우고 설정을 관리하고 reload를 건다.
- 실제 트래픽은 nginx worker가 처리하고, 컨트롤러 프로세스는 설정 생성만 한다 (**컨트롤 플레인과 데이터 플레인이 같은 Pod에 있다**).
- 주요 인자: `--ingress-class=nginx`(담당 클래스), `--election-id`(리더 선출), `--publish-service`(Ingress의 ADDRESS에 표시할 Service), `--configmap`(전역 설정), `--validating-webhook`.

##### 2-3. 동작 순서

```
 ┌─ API 서버 ──────────────────────────────────────────────┐
 │  Ingress / Service / EndpointSlice / Secret / ConfigMap │
 └───┬──────────────────────────────────────▲──────────────┘
     │ ① watch (변경 통지)                   │ ⑤ 상태 기록 (Ingress status, 리더 lease)
     ▼                                      │
 컨트롤러 프로세스 ──② 내부 모델 갱신 → ③ nginx.conf 생성 → ④ nginx reload (또는 동적 갱신)
                                │
                                ▼
                   nginx worker ── 요청 처리 ──▶ Pod IP (Service의 엔드포인트)
```

**① 컨트롤러는 API 서버를 watch한다.** 3절의 Watch API를 쓴다. 폴링하지 않고 변경이 있을 때만 통지를 받는다. 컨트롤러 로그로 확인했다.

```
I1022 13:20:06  "Starting NGINX Ingress controller"
I1022 13:20:07  "Starting NGINX process"
I1022 13:20:07  attempting to acquire leader lease ingress-nginx/ingress-nginx-leader...
I1022 13:20:07  "Starting validation webhook" address=":8443"
I1022 13:20:07  "Configuration changes detected, backend reload required"
I1022 13:20:07  "Backend successfully reloaded"
I1022 13:20:30  "Found valid IngressClass" ingress="t8/demo" ingressclass="nginx"     ← Ingress를 apply한 직후
I1022 13:20:32  "Configuration changes detected, backend reload required"
I1022 13:20:32  "Backend successfully reloaded"
I1022 13:21:01  "Ignoring ingress because of error while validating ingress class" ingress="t8/noclass"
```

- Ingress 생성(13:20:30) → 컨트롤러가 인지 → 약 2초 뒤(13:20:32) nginx에 반영되었다.
- 담당 클래스(`nginx`)가 아닌 Ingress(`noclass`)는 이벤트를 받고도 **걸러냈다.**

**② ③ 설정 생성: Ingress가 nginx.conf가 된다**

```
$ k -n ingress-nginx exec <pod> -- sh -c 'wc -l /etc/nginx/nginx.conf; grep -n "server_name a.example.test" /etc/nginx/nginx.conf'
1846 /etc/nginx/nginx.conf
335:		server_name a.example.test ;
```

`a.example.test` 규칙은 이런 `server` 블록이 되었다 (일부).

```nginx
server {
    server_name a.example.test ;
    listen 80 ;  listen 443 ssl ;
    location / {
        set $namespace      "t8";
        set $ingress_name   "demo";
        set $service_name   "app-a";
        set $service_port   "80";
        set $proxy_upstream_name "t8-app-a-80";
        rewrite_by_lua_file /etc/nginx/lua/nginx/ngx_rewrite.lua;
        proxy_set_header Host                   $best_http_host;
        proxy_set_header X-Forwarded-For        $remote_addr;
        proxy_set_header X-Forwarded-Host       $best_http_host;
        proxy_set_header X-Forwarded-Proto      $pass_access_scheme;
        proxy_set_header X-Real-IP              $remote_addr;
        ...
```

- Ingress의 `host`가 `server_name`, `path`가 `location`이 된다. 어느 Ingress/Service에서 왔는지가 `set $ingress_name`, `$service_name`으로 변수에 들어간다.
- 백엔드로 전달할 때 `X-Forwarded-For/Host/Proto`, `X-Real-IP` 헤더를 붙인다.
- 어노테이션이 설정 값이 되는 것도 확인했다. `nginx.ingress.kubernetes.io/proxy-body-size=5m`을 Ingress에 붙이자 `client_max_body_size 5m`이 `nginx.conf`에 나타났다.

**④ 반영 방식: 규칙이 바뀌면 reload, 엔드포인트가 바뀌면 동적 갱신**

| 변경 | 컨트롤러 동작 (실측) |
|---|---|
| Ingress 생성/수정, 어노테이션 변경 | `nginx.conf`를 다시 만들고 **reload**. 어노테이션 변경 후 2.7초 만에 반영(폴링 시간 포함), reload 카운트 4 → 5 |
| 백엔드 Pod 수 변경 (app-a 1 → 3) | **reload 없음**. reload 카운트 4 → 4 그대로, 새 엔드포인트 즉시 반영 |

```
$ k -n t8 scale deploy/app-a --replicas=3                    # reload 카운트 4 → 4
$ k -n t8 get endpoints app-a -o jsonpath='{.subsets[0].addresses[*].ip}'
10.1.1.19 10.1.1.22 10.1.1.23
$ k -n ingress-nginx exec <pod> -- curl -s localhost:10246/configuration/backends
[{"name":"t8-app-a-80", ..., "endpoints":[{"address":"10.1.1.19","port":"80"},
                                           {"address":"10.1.1.22","port":"80"},
                                           {"address":"10.1.1.23","port":"80"}], ...}]
```

- Pod는 자주 늘고 줄기 때문에 그때마다 nginx를 reload하면 부담이 크다. ingress-nginx는 **엔드포인트 목록을 nginx 설정 파일이 아닌 컨트롤러 내부의 동적 설정(Lua)** 으로 따로 관리한다. 컨트롤러가 엔드포인트 변경을 감지하면 `localhost:10246/configuration/backends`로 보내는 방식으로 갱신하고, nginx의 Lua 코드가 그 목록으로 백엔드를 고른다. 위에서 이 엔드포인트 JSON에 3개 Pod IP가 들어 있는 것을 확인했다 (이 갱신 메커니즘의 내부 구현은 코드까지 확인하지 않았다).
- 정리하면 **라우팅 규칙(Ingress)은 설정 파일 + reload, 백엔드 목록(엔드포인트)은 동적 갱신**이다.

**⑤ 백엔드 선택: ClusterIP를 거치지 않는다**

- 컨트롤러가 가진 백엔드 목록은 **Service의 ClusterIP가 아니라 Pod IP 목록**이다 (위 JSON의 `endpoints`가 `kubectl get endpoints`의 Pod IP와 일치). 6장에서 본 kube-proxy의 iptables 분산을 거치지 않고 nginx가 직접 Pod를 고른다. 그래서 컨트롤러가 세션 어피니티, 가중치, 재시도 같은 기능을 직접 제공할 수 있다.
- 분산 결과: 3개 Pod에 `/sub/` 요청 30건을 보냈더니 Pod별 로그가 9, 10, 13건이었다 (이전 실험의 요청이 일부 포함되어 있어 정확한 분산 비율은 아니고, 한 Pod에 쏠리지 않고 고르게 분산된다는 정도만 확인했다).

**리더 선출과 status**

- 컨트롤러 로그에 `successfully acquired lease ingress-nginx/ingress-nginx-leader`, `New leader elected`가 있다. 컨트롤러 Pod를 여러 개 띄워도 **Ingress의 `status`(ADDRESS) 기록은 리더 한 개만** 한다 (트래픽 처리는 모든 Pod가 한다). 여러 Pod일 때의 동작은 실험하지 않았다.
- 이 환경에서는 Service가 `LoadBalancer <pending>`이라 ADDRESS가 비어 있다.

##### 2-4. 컨트롤러의 종류

| 컨트롤러 | 프록시 | 특징 |
|---|---|---|
| ingress-nginx | nginx | 가장 널리 쓰이는 쿠버네티스 커뮤니티 컨트롤러. 이 장의 실험 대상 |
| (클라우드) AWS Load Balancer Controller 등 | 클라우드 L7 로드 밸런서 | Ingress를 클라우드 LB 리소스로 변환 |
| Traefik, HAProxy, Envoy 기반 등 | 각자 | 구현마다 어노테이션/CRD가 다름 |

- 이 표는 문서 지식이고 ingress-nginx 외에는 설치하지 않았다.
- 후속 표준인 **Gateway API**가 Ingress의 한계(컨트롤러별 어노테이션 의존)를 보완하려고 나왔다는 정도만 적는다 (직접 확인하지 않았다).

#### 3. Watch API

##### 3-1. 개념

**리소스의 변경 사항을 서버가 클라이언트에게 계속 밀어 주는(stream) 쿠버네티스 API**다. 일반 조회(List)는 "지금 상태"를 한 번 주고 끝나지만, Watch는 연결을 열어 둔 채 **오브젝트가 바뀔 때마다 이벤트를 전달**한다. 컨트롤러, 스케줄러, kubelet, 인그레스 컨트롤러가 모두 이 방식으로 변경을 감지한다 (6장에서 "각자 API 서버를 watch한다"고 한 그 메커니즘).

- 요청: 같은 List URL에 `?watch=true`를 붙인다. `kubectl get -w`가 이것이다.
- 폴링과 비교하면 변경이 없을 때 요청이 없고, 변경이 생기면 바로 통지된다. API 서버와 etcd의 부하가 훨씬 적다.

##### 3-2. HTTP 응답 형태

`kubectl proxy`(`localhost:18001`)로 확인했다.

```
$ curl -sv "http://127.0.0.1:18001/api/v1/namespaces/t8/pods?watch=true" -o /dev/null
< HTTP/1.1 200 OK
< Content-Type: application/json
< Transfer-Encoding: chunked
```

- **상태 코드 200과 `Transfer-Encoding: chunked`** 로 응답이 끝나지 않고 이어진다. 이벤트가 생길 때마다 **JSON 한 줄**(`{"type":..., "object":...}`)이 청크로 전달된다.
- 이벤트 형식: `{"type": "ADDED|MODIFIED|DELETED|BOOKMARK|ERROR", "object": {...}}`

##### 3-3. 이벤트 종류

| type | 의미 |
|---|---|
| `ADDED` | 오브젝트가 생겼다 (또는 Watch 시작 시점에 이미 존재) |
| `MODIFIED` | 오브젝트가 바뀌었다 (spec뿐 아니라 status, 라벨도 포함) |
| `DELETED` | 오브젝트가 삭제되었다 (`object`에는 삭제 직전 상태) |
| `BOOKMARK` | "여기까지 봤다"는 `resourceVersion` 표시. 오브젝트 변경이 아님 |
| `ERROR` | 오류. 예: `410 Expired` |

**실측: Pod 하나의 생성 → 라벨 → 삭제**

List로 얻은 `resourceVersion=2075997`부터 Watch를 열고, 그동안 `k run w1`, `k label pod w1 demo=yes`, `k delete pod w1`을 실행했다.

```
ADDED    Pod w1 rv=2076003 phase=Pending
MODIFIED Pod w1 rv=2076004 phase=Pending          # 스케줄러가 노드 배정
MODIFIED Pod w1 rv=2076006 phase=Pending          # kubelet이 컨테이너 생성 중
MODIFIED Pod w1 rv=2076011 phase=Running
MODIFIED Pod w1 rv=2076012 phase=Running labels=yes   # 라벨 추가
MODIFIED Pod w1 rv=2076015 phase=Running labels=yes   # 삭제 요청 → deletionTimestamp 설정
MODIFIED Pod w1 rv=2076017 phase=Succeeded labels=yes # 컨테이너 종료 (SIGTERM로 정상 종료)
MODIFIED Pod w1 rv=2076021 phase=Succeeded labels=yes
DELETED  Pod w1 rv=2076022 phase=Succeeded labels=yes
```

- Pod 하나의 한 생애에 **이벤트가 9개** 발생했다. 단계마다 스케줄러, kubelet 등 다른 컴포넌트가 `status`/`spec`을 갱신해서 `MODIFIED`가 여러 번 온다. 6장에서 본 "컴포넌트들이 각자 자기 몫만 쓰고 다음 컴포넌트가 그 변경을 본다"를 이벤트 흐름으로 직접 본 것이다.
- 각 이벤트의 `resourceVersion`이 계속 커진다 (아래).
- 위 로그의 각 `MODIFIED`가 정확히 어떤 컴포넌트의 변경인지는 `managedFields`까지는 확인하지 않아서 주석은 일반적인 순서를 적은 것이다.

##### 3-4. resourceVersion과 List-then-Watch 패턴

**`resourceVersion`**: etcd의 변경 순번에서 나온 값으로, 오브젝트와 List 응답에 붙는다. "이 시점"을 나타내는 표식이다. 클라이언트는 이 값으로 **빠뜨림 없이 이어서** 변경을 받는다.

```
$ curl "$A/api/v1/namespaces/t8/pods"                                   # List: items 5개, metadata.resourceVersion=2075977
$ curl "$A/api/v1/namespaces/t8/pods?watch=true&resourceVersion=2075977"  # 그 시점 이후의 변경만
```

`kubectl get -w`도 같은 순서로 요청한다 (`-v=6`).

```
GET .../pods?labelSelector=app%3Dapp-b&limit=500&timeout=4s                          200   (① List)
GET .../pods?labelSelector=app%3Dapp-b&resourceVersion=2076098&timeout=4s&watch=true       (② 그 resourceVersion부터 Watch)
```

- **List로 현재 상태를 받고, 그 `resourceVersion`으로 Watch를 시작한다.** 이 패턴으로 List와 Watch 사이에 일어난 변경을 놓치지 않는다. 컨트롤러는 이 패턴을 로컬 캐시(informer, 아래)와 함께 쓴다.
- `resourceVersion` 없이 Watch를 열면 현재 존재하는 오브젝트를 **합성된 `ADDED` 이벤트**로 먼저 보내고, 이후 변경을 보낸다.

```
$ curl "$A/api/v1/namespaces/t8/pods?watch=true"           # resourceVersion 없이
ADDED Pod app-a-... rv=2075890
ADDED Pod app-a-... rv=2075887
ADDED Pod app-a-... rv=2075750
ADDED Pod app-b-... rv=2075755
ADDED Pod app-def-... rv=2075746                           # 5개가 모두 ADDED (실제로는 이미 있던 것)
```

- 이 `ADDED`의 `rv`는 서로 다르고 오래된 값이다 (오브젝트가 마지막으로 바뀐 시점). 즉 이 `ADDED`는 "방금 생겼다"가 아니라 "Watch 시작 시점에 이 오브젝트가 있다"는 뜻이다.

**너무 오래된 resourceVersion → 410 Gone(Expired)**

```
$ curl -i "$A/api/v1/namespaces/t8/pods?watch=true&resourceVersion=1"
HTTP/1.1 200 OK
{"type":"ERROR","object":{"kind":"Status","status":"Failure","message":"The resourceVersion for the provided watch is too old.","reason":"Expired","code":410}}

$ curl "$A/api/v1/namespaces/t8/pods?resourceVersion=1&resourceVersionMatch=Exact"       # 일반 List도
410 Expired  The resourceVersion for the provided list is too old.
```

- API 서버는 **최근 변경 이력만** 보관한다. 이력에서 밀려난 `resourceVersion`으로 Watch를 열면 거부된다. **Watch는 HTTP 200으로 열린 뒤 스트림 안에서 `ERROR` 이벤트(`code: 410`)로** 알려 준다 (HTTP 상태 코드로 오지 않는다). 일반 List는 상태 코드 410으로 응답한다.
- 클라이언트(컨트롤러)는 이 오류를 받으면 **List부터 다시 하고 새 `resourceVersion`으로 Watch를 재시작(re-list)** 해야 한다. 연결이 끊겼다가 오래 뒤에 다시 붙을 때가 이 경우다 (이 재시작 동작은 직접 재현하지 않았다).

##### 3-5. 선택자와 범위

Watch는 List와 같은 필터를 쓴다.

```
?watch=true&labelSelector=app%3Dapp-b                              → ADDED Pod app-b-... 만
?watch=true&fieldSelector=metadata.name%3Dapp-def-784f5f46bf-dpfn2 → ADDED Pod app-def-... 만
/api/v1/services?watch=true                                        → 모든 네임스페이스의 Service (kubernetes, kube-dns, ingress-nginx-controller ...)
```

- 네임스페이스 하나, 클러스터 전체, 라벨/필드 선택자로 대상을 좁힐 수 있다. 인그레스 컨트롤러가 전체 네임스페이스의 Ingress를 한 번에 감시하는 방식이 클러스터 범위 URL의 Watch다.

##### 3-6. 북마크, 타임아웃, 초기 이벤트

| 파라미터 | 동작 (실측) |
|---|---|
| `timeoutSeconds=25` | 25초 뒤 서버가 연결을 닫는다. 클라이언트는 마지막 `resourceVersion`으로 다시 연결한다 (`kubectl get -w --request-timeout=4`처럼 `timeout=4s`로 전달) |
| `allowWatchBookmarks=true` | 서버가 `BOOKMARK` 이벤트를 보낼 수 있게 허용. 25초 관찰에서는 BOOKMARK가 오지 않았다 (보내는 주기는 서버가 정하며 관찰한 시간 안에는 없었다) |
| `sendInitialEvents=true` + `resourceVersionMatch=NotOlderThan` | 현재 오브젝트를 `ADDED`로 모두 보낸 뒤, **초기 목록의 끝을 알리는 `BOOKMARK`** 를 보낸다 |

```
$ curl "$A/api/v1/namespaces/t8/pods?watch=true&sendInitialEvents=true&resourceVersionMatch=NotOlderThan&allowWatchBookmarks=true"
ADDED    Pod app-a-...
ADDED    Pod app-a-...
ADDED    Pod app-a-...
ADDED    Pod app-b-...
ADDED    Pod app-def-...
BOOKMARK Pod  k8s.io/initial-events-end=true       # 초기 목록 끝 표시
```

- 이 방식(WatchList, 스트리밍 목록)은 **List 없이 Watch만으로** 초기 상태를 받는 방법이다. 큰 클러스터에서 List 응답이 커지는 문제를 줄이려고 만들어졌다. 이 클러스터(v1.32)에서 동작하는 것을 확인했다.
- 일반 `?watch=true`(`resourceVersion` 없음)에서는 마지막 `ADDED` 뒤에 "초기 목록 끝" 표시가 없어서 클라이언트가 어디까지가 초기 상태인지 알 수 없다.

##### 3-7. 컨트롤러는 Watch를 어떻게 쓰는가 (개념)

```
API 서버 ──List+Watch──▶ [Reflector] ──이벤트──▶ [로컬 캐시(Store)] ──▶ [이벤트 핸들러] ──▶ [작업 큐] ──▶ 컨트롤러 로직
```

- 컨트롤러는 **클라이언트 라이브러리의 informer(Reflector + 로컬 캐시)** 를 통해 List-then-Watch를 하고 결과를 메모리에 캐시한다. 조회는 캐시에서 하므로 API 서버에 매번 묻지 않는다.
- 이벤트가 오면 작업 큐에 해당 오브젝트의 키를 넣고, 컨트롤러가 큐에서 꺼내 **원하는 상태와 현재 상태를 비교해 맞춘다** (reconcile).
- 이 구조(informer, 작업 큐)는 12장의 커스텀 컨트롤러와 연결되는 내용이다. 여기서는 개념 설명이고 코드로 직접 확인하지 않았다.
- 인그레스 컨트롤러에서 이 구조의 결과를 확인한 것이 2-3절의 로그다. Ingress 생성 → `Found valid IngressClass` → `Configuration changes detected` → `Backend successfully reloaded`가 이어졌다. 컨트롤러가 실제로 어떤 리소스를 watch하는지(Ingress, Service, EndpointSlice, Secret, ConfigMap)는 요청 로그나 코드로 확인하지 않았다.

##### 3-8. API 서버와 etcd 쪽

- 오브젝트는 etcd에 `/registry/<리소스>/<네임스페이스>/<이름>` 키로 저장된다 (이 장에서도 `/registry/ingress/t8/demo` 등 6개를 확인했다. 6장과 같은 형식).
- `resourceVersion`은 etcd의 변경 순번에서 오고, API 서버가 etcd의 watch를 받아 **자체 캐시**에 이력을 쌓은 뒤 여러 클라이언트의 Watch에 나눠 보낸다고 알려져 있다. 위 `410 Expired`는 그 이력에서 밀려난 값 때문에 났다. 이 내부 구조(watch cache 크기, etcd 연결 수)는 API 서버 메트릭을 보려 했으나 해당 지표를 얻지 못해 확인하지 못했다.

#### 4. 확인하지 못한 부분

- **`LoadBalancer`/`NodePort`를 통한 실제 외부 접근**: 이 환경에서는 `EXTERNAL-IP`가 `<pending>`이라 `port-forward`로 컨트롤러 Service에 연결해 확인했다. Ingress의 `ADDRESS`가 채워지는 것은 보지 못했다.
- **호스트가 있는 Ingress에서 `defaultBackend`가 적용되는 조건**: 알 수 없는 호스트가 `defaultBackend`가 아니라 컨트롤러 기본 404로 처리된 이유는 확인하지 못했다.
- **기본 IngressClass 설정** (`ingressclass.kubernetes.io/is-default-class` 어노테이션): `ingressClassName`을 생략한 Ingress를 컨트롤러가 무시하는 것만 확인했고, 기본 클래스를 지정하면 달라지는지는 하지 않았다.
- **컨트롤러 Pod 여러 개일 때**: 리더 선출 이후의 status 기록 분담, 장애 시 리더 전환은 실험하지 않았다.
- **어드미션 웹훅이 없을 때**: 충돌 설정이 nginx에 어떻게 반영/거부되는지 확인하지 않았다.
- **Lua 동적 갱신의 내부 구현**: 엔드포인트 변경이 reload 없이 반영되고 `10246/configuration/backends`에 Pod IP가 들어 있는 것까지만 확인했다. 컨트롤러 코드는 읽지 않았다.
- **컨트롤러가 watch하는 리소스 목록과 연결 수**: API 서버의 `apiserver_registered_watchers` 같은 메트릭을 조회했으나 값을 얻지 못했다. 로그와 동작으로만 간접 확인했다.
- **Watch의 410 이후 재시작**: 오류 이벤트는 재현했으나 클라이언트가 re-list하는 과정, 연결 끊김 후 재연결 동작은 재현하지 않았다.
- **`BOOKMARK` 주기**: 25초 관찰에서는 오지 않았다. 서버가 보내는 주기와 조건은 확인하지 못했다.
- **컨트롤러별 차이와 Gateway API**: ingress-nginx 외의 컨트롤러(Traefik, 클라우드 LB 컨트롤러 등)와 Gateway API는 설치/실행하지 않았다.
- **ingress-nginx 프로젝트의 유지보수 상태**: 이 노트의 실험은 v1.12.1에서 했고, 이후 버전의 동작이나 프로젝트 지원 상황은 확인하지 않았다.
- **Watch가 필요한 이벤트의 정확한 컴포넌트 귀속**: Pod 이벤트 9개 중 각각을 누가 갱신했는지 `managedFields`로 확인하지 않았다.
