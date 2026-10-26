### 8장. 인그레스

> 정리 범위: 인그레스(Ingress)와 인그레스 컨트롤러의 특징, 그리고 둘의 차이
>
> 실측 환경: macOS (Apple Silicon), Docker Desktop. 인그레스 컨트롤러를 설치해야 하므로 Docker Desktop 클러스터는 건드리지 않고 **임시 kind 클러스터**(`t8ing`, v1.35.0, 노드 1개, 별도 kubeconfig)를 만들어 실험하고 삭제했다. 컨트롤러는 **ingress-nginx**(Helm 차트 4.15.1, 컨트롤러 v1.15.1)와 **Traefik**(Helm 차트, 이미지 v3.7.16) 두 가지를 같은 클러스터에 같이 설치했다. 노드의 80/443은 호스트 18080/18443에 매핑했다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: `시작하세요! 도커/쿠버네티스` [8장](../../start-docker-kubernetes/chapter08/README.md)에서 Ingress 필드 구조, `pathType`, TLS 종료, 컨트롤러 동작 순서를 다뤘다. 여기서는 그 내용을 반복하지 않고 **인그레스와 컨트롤러의 역할 차이**, **컨트롤러 사이의 동작 차이**, 이번에 새로 확인한 것을 중심으로 적었다.

실험용 백엔드는 nginx 3개(`web`, `api`, `shop`)로, 응답에 자신의 이름과 요청의 `Host`, URI, `X-Forwarded-*` 헤더, 접속 소스 IP를 돌려주도록 설정했다. 모두 `t8` 네임스페이스의 Service(80 → 8080)다.

#### 목차

1. 인그레스와 인그레스 컨트롤러는 다르다
2. 인그레스(리소스)의 특징
3. 인그레스 컨트롤러의 특징
4. ingress-nginx와 Traefik 차이 (같은 규칙으로 비교)
5. IngressClass
6. 선택 가이드와 함정
7. 확인하지 못한 부분

---

#### 1. 인그레스와 인그레스 컨트롤러는 다르다

| 구분 | 인그레스 (Ingress) | 인그레스 컨트롤러 (Ingress Controller) |
|---|---|---|
| 정체 | 라우팅 **규칙을 적은 API 오브젝트** (`networking.k8s.io/v1`) | 그 규칙을 읽어 **실제로 트래픽을 처리하는 프로그램** (Pod) |
| 하는 일 | "이 호스트/경로는 이 Service로" 선언 | Ingress를 watch 하다가 프록시 설정을 만들고 요청을 전달 |
| 쿠버네티스 기본 포함 | **포함** (API가 있다) | **미포함** — 직접 설치해야 한다 |
| 없으면 | 오브젝트만 etcd에 저장될 뿐 아무 일도 안 일어남 | 규칙이 있어도 처리할 주체가 없음 |
| 예 | `kind: Ingress` YAML | ingress-nginx, Traefik, HAProxy, Envoy 계열 등 |

이 클러스터에서 확인한 사실:

```
$ kubectl api-resources --api-group=networking.k8s.io
NAME              SHORTNAMES   APIVERSION             NAMESPACED   KIND
ingressclasses                 networking.k8s.io/v1   false        IngressClass
ingresses         ing          networking.k8s.io/v1   true         Ingress
...
```

컨트롤러를 설치하기 전부터 `Ingress` API는 있었다. 컨트롤러는 Deployment로 뜬 평범한 Pod이고, 쿠버네티스의 일부가 아니다(ingress-nginx는 `ingress-nginx` 네임스페이스에 Pod 1개, Service 2개(컨트롤러용, 웹훅용)).

컨트롤러가 없는 상태는 이 실험에서 직접 만들지 않았다. 아래 5절의 "클래스가 안 맞는 Ingress"가 같은 상황(처리할 컨트롤러가 없는 Ingress)이고, 그 결과는 `ADDRESS` 칸이 빈 채로 404였다.

---

#### 2. 인그레스(리소스)의 특징

##### 2-1. L7(HTTP/HTTPS) 라우팅 규칙만 표현한다

- 라우팅 기준은 **호스트**와 **경로**다. 헤더, 쿼리, 메서드 같은 조건은 표준 Ingress에 없다(필요하면 컨트롤러별 어노테이션이나 CRD).
- HTTP/HTTPS 외의 프로토콜(TCP/UDP)은 표준 Ingress로 표현할 수 없다.
- 한 도메인 아래 여러 Service를 경로로 나누거나, 한 진입점으로 여러 호스트를 받을 수 있다. `LoadBalancer` Service를 앱마다 만드는 대신 로드밸런서 1개로 여러 앱을 받는 용도다.

##### 2-2. 호스트/경로 매칭 (실측)

`web.example.com`, `api.example.com`을 호스트로 나눈 Ingress와, `site.example.com` 한 호스트에서 경로를 나눈 Ingress(`/exact` Exact → api, `/pre` Prefix → shop, `/` Prefix → web)를 만들었다.

```
$ curl -H "Host: web.example.com" localhost:18080/        → app=web
$ curl -H "Host: api.example.com" localhost:18080/x/y     → app=api  (uri=/x/y, 경로가 그대로 전달)
$ curl -H "Host: nope.example.com" localhost:18080/       → 404 (nginx 기본 404)
$ curl localhost:18080/   (Host 가 localhost)             → 404
```

| 요청 경로 (host: site.example.com) | 응답 | 이유 |
|---|---|---|
| `/exact` | api | Exact 일치 |
| `/exact/`, `/exact/x` | web | Exact는 정확히 같을 때만. `/` Prefix로 폴백 |
| `/pre`, `/pre/`, `/pre/x` | shop | Prefix, 경로 요소 단위로 일치 |
| `/prefix`, `/pre2` | web | `/pre`는 `/prefix`와 **일치하지 않는다** (문자열이 아니라 `/` 요소 단위) |
| `/other` | web | `/` Prefix |

- 와일드카드 호스트(`*.wild.example.com`)는 `a.wild.example.com`만 일치하고 `wild.example.com`, `a.b.wild.example.com`은 일치하지 않았다(두 경우 모두 404).
- `host`를 생략한 규칙은 모든 호스트(Host 헤더가 없거나 `localhost`인 요청 포함)에 적용됐다.

##### 2-3. `pathType`은 필수다

```
$ kubectl apply -f nopt.yaml   (pathType 생략)
The Ingress "nopt" is invalid: spec.rules[0].http.paths[0].pathType: Required value: pathType must be specified
```

`v1`에서는 API 서버가 거부한다. 값은 `Exact`, `Prefix`, `ImplementationSpecific`이고, 마지막 값은 **컨트롤러가 알아서 해석**한다(아래 정규식 rewrite 예가 이 값을 쓴다).

##### 2-4. `defaultBackend`의 범위

| 구성 | `nope.example.com /` | 그 호스트의 규칙 없는 경로 |
|---|---|---|
| `defaultBackend: shop`만 (rules 없음) | **shop** | — (모든 요청이 shop) |
| `defaultBackend: shop` + `rules`(`d2.example.com`의 `/only` → api) | 404 (컨트롤러 기본) | `d2.example.com/zzz` → **shop**, `/only` → api |
| `defaultBackend` 없이 `rules`(와일드카드 호스트)만 | 404 | 404 |

rules가 함께 있을 때의 `defaultBackend`는 **그 Ingress에 적힌 호스트에서 어떤 경로에도 안 맞는 요청**에만 적용됐다. 아무 호스트에도 안 맞는 요청은 컨트롤러의 기본 404가 처리했다. (이 동작은 ingress-nginx에서 관찰한 것이고, 다른 컨트롤러는 확인하지 않았다.)

##### 2-5. 규칙이 틀려도 API 서버는 대부분 받아준다

- 존재하지 않는 Service를 가리키는 Ingress(`nosuch`)는 `created`로 통과했고, 요청은 **404**였다.
- Service는 있지만 Ready 엔드포인트가 0개(`shop`을 replicas 0)면 **503**이었다. 존재하지 않는 Service(404)와 엔드포인트 없음(503)이 다른 코드다.
- 즉 `kubectl apply`가 성공했다고 라우팅이 되는 것이 아니다. 검증은 컨트롤러 쪽(아래 3-4)과 실제 요청으로 한다.

##### 2-6. `status.loadBalancer`는 컨트롤러가 채운다

```
NAME         CLASS     HOSTS                  ADDRESS         PORTS     AGE
host-based   nginx     web.example.com,...    10.96.109.141   80        2m28s     ← ingress-nginx가 채움
tr-host      traefik   web.example.com,...                    80        3s        ← Traefik (Service가 <pending>이라 비어 있음)
cls-wrong    traefik   cls-wrong.example.com                  80        82s
```

`ADDRESS`는 사용자가 쓰는 값이 아니라 컨트롤러가 자기 Service의 주소(`--publish-service`, 이 실험에서는 ClusterIP)를 써 넣은 값이다. Traefik의 Service는 `LoadBalancer` + `<pending>`이라 비어 있었다. 클라우드 환경이면 로드밸런서 주소가 들어간다(이 실험에서는 확인 못 함).

---

#### 3. 인그레스 컨트롤러의 특징

##### 3-1. 컨트롤러 = 쿠버네티스 API watch + 리버스 프록시

ingress-nginx의 컨트롤러 Pod 하나는 두 가지가 같이 돈다.

1. Ingress, Service, EndpointSlice, Secret, ConfigMap, IngressClass를 watch 하는 **컨트롤러 프로세스** (`/nginx-ingress-controller`)
2. 실제 요청을 받는 **nginx** (컨트롤러가 `/etc/nginx/nginx.conf`를 생성)

```
$ kubectl -n ingress-nginx get pod ... -o jsonpath='{...args}'
["/nginx-ingress-controller","--publish-service=...","--election-id=...","--controller-class=k8s.io/ingress-nginx",
 "--ingress-class=nginx","--configmap=...","--validating-webhook=:8443",...]
```

Ingress를 만들면 컨트롤러가 이를 `nginx.conf`의 `server`/`location`으로 번역한다. 생성된 설정 일부:

```
server_name "web.example.com" ;
location "/" {
    set $ingress_name   "host-based";
    set $service_name   "web";
    set $proxy_upstream_name "t8-web-80";
    proxy_set_header Host                   $best_http_host;
    proxy_set_header X-Real-IP              $remote_addr;
    proxy_set_header X-Forwarded-For        $remote_addr;
    proxy_set_header X-Forwarded-Host       $best_http_host;
    proxy_set_header X-Forwarded-Proto      $pass_access_scheme;
    proxy_pass http://upstream_balancer;
```

##### 3-2. 트래픽은 Service(kube-proxy)를 거치지 않고 **Pod IP로 직접** 간다

Ingress는 Service를 가리키지만, 컨트롤러는 Service의 ClusterIP로 보내지 않고 **Service의 EndpointSlice에서 Pod IP를 읽어 직접** 연결한다. 컨트롤러의 접근 로그:

```
... "GET / HTTP/1.1" 200 113 ... [t8-web-80] [] 10.244.0.9:8080 ...
... "GET / HTTP/1.1" 200 113 ... [t8-web-80] [] 10.244.0.12:8080 ...   ← 다른 Pod
```

- 로그의 업스트림이 ClusterIP가 아니라 Pod IP:targetPort(`10.244.0.9:8080`)다.
- 그래서 Service의 `sessionAffinity`, kube-proxy의 분산 규칙은 이 경로에 적용되지 않고, **분산은 컨트롤러가** 한다(nginx의 `upstream_balancer`가 Lua 밸런서, `balancer_by_lua_file /etc/nginx/lua/nginx/ngx_conf_balancer.lua`).
- Service는 "Pod 목록을 알려주는 이름표" 역할이다(7장의 [Service/kube-proxy](../chapter07/README.md)).

##### 3-3. Pod 개수가 바뀌어도 nginx를 reload 하지 않는다

```
$ kubectl scale deploy web --replicas=3
reload count 2 -> 2          ← "Backend successfully reloaded" 로그 수, 변화 없음
endpoints: ["10.244.0.9"] ["10.244.0.12"] ["10.244.0.13"]
6 requests → 3 Pod에 분산 (접근 로그의 upstream 주소로 확인)
```

- **Ingress 규칙이 바뀔 때**는 reload가 일어난다 (`Configuration changes detected, backend reload required` → `Backend successfully reloaded`, `RELOAD` 이벤트).
- **엔드포인트(Pod)만 바뀔 때**는 reload 없이 Lua 쪽 상태만 갱신한다. 롤링 업데이트가 잦아도 연결이 끊기지 않는 이유다.

##### 3-4. 어드미션 웹훅으로 잘못된 Ingress를 거부한다 (ingress-nginx)

```
$ kubectl apply -f dup.yaml      # web.example.com + / 가 이미 host-based 에 있음
Error ... admission webhook "validate.nginx.ingress.kubernetes.io" denied the request:
host "web.example.com" and path "/" is already defined in ingress t8/host-based

$ kubectl apply -f snip.yaml     # nginx.ingress.kubernetes.io/server-snippet 어노테이션
Error ... denied the request: nginx.ingress.kubernetes.io/server-snippet annotation cannot be used.
Snippet directives are disabled by the Ingress administrator
```

- 같은 호스트+경로 중복을 `apply` 시점에 막는다. 2-5에서 본 "API 서버는 다 받아준다"를 컨트롤러가 웹훅으로 보완한 것이다.
- 설정 스니펫(`*-snippet`)은 이 차트 기본값에서 거부됐다. nginx 설정 임의 삽입을 막기 위한 것이다.
- 웹훅이 있다는 것은 **컨트롤러가 죽어 있으면 해당 클래스 Ingress를 만들 때 웹훅 호출이 실패할 수 있다**는 뜻이다(이 실험에서는 확인하지 않음).

##### 3-5. 컨트롤러가 직접 하는 일 (Ingress 규칙 밖의 기능)

| 기능 | 실측 |
|---|---|
| **TLS 종료** | `tls` 섹션 + `kubernetes.io/tls` Secret. 클라이언트와는 HTTPS, 백엔드와는 HTTP. 백엔드는 `xfp=https`를 받았다 |
| **SNI** | `secure.example.com`으로 접속하면 해당 인증서(`CN=secure.example.com`), 다른 SNI는 컨트롤러 기본 인증서(`CN=Kubernetes Ingress Controller Fake Certificate`) |
| **HTTP→HTTPS 리다이렉트** | TLS가 설정된 호스트는 기본으로 `308 Permanent Redirect` (`Location: https://secure.example.com`, 포트는 안 붙음). `ssl-redirect: "false"` 어노테이션으로 끄면 HTTP로 200 |
| **URL 재작성** | `nginx.ingress.kubernetes.io/rewrite-target: /$2` + 경로 `"/app(/|$)(.*)"`(`ImplementationSpecific`)이면 `/app` → `/`, `/app/a/b?x=1` → 백엔드에는 `/a/b?x=1`, `/other`는 404 |
| **헤더 추가** | `X-Forwarded-For/Host/Proto`, `X-Real-IP` 자동 추가 |

- 재작성 없이 `/pre/x`를 보내면 백엔드는 `uri=/pre/x`를 받는다. **경로 접두사는 기본으로 제거되지 않는다.**
- 이런 기능은 **표준 Ingress 필드가 아니라 컨트롤러별 어노테이션**이다. 컨트롤러를 바꾸면 안 통한다(4절).

##### 3-6. 클라이언트 IP

```
$ curl -H "Host: web.example.com" -H "X-Forwarded-For: 1.2.3.4" localhost:18080/
app=web ... xff=192.168.65.1 xri=192.168.65.1 src=10.244.0.7
```

- 클라이언트가 보낸 `X-Forwarded-For: 1.2.3.4`는 **버려지고** 컨트롤러가 본 소스 IP(`192.168.65.1`, Docker Desktop VM 쪽 게이트웨이)로 **덮어써졌다**. 기본 설정(`use-forwarded-headers` 꺼짐)에서 앞단 신뢰하지 않는 동작이다.
- 백엔드가 본 TCP 소스(`src`)는 클라이언트가 아니라 **컨트롤러 Pod IP**(`10.244.0.7`)다. 백엔드는 헤더로만 실제 클라이언트를 알 수 있다.
- 앞에 클라우드 LB나 프록시가 있으면 이 값이 LB 주소가 된다. 이 환경에서는 확인하지 못했다.

---

#### 4. ingress-nginx와 Traefik 차이 (같은 규칙으로 비교)

같은 클러스터에 두 컨트롤러를 설치하고 같은 호스트/경로 Ingress를 `ingressClassName`만 바꿔 만들었다(Traefik은 `kubectl port-forward svc/tr-traefik 18090:80`으로 접속).

| 항목 | ingress-nginx | Traefik (v3.7.16) |
|---|---|---|
| 데이터 플레인 | nginx (컨트롤러 + nginx 한 Pod) | Traefik 자체 프록시(Go) |
| 클래스 이름 / controller 값 | `nginx` / `k8s.io/ingress-nginx` | `traefik` / `traefik.io/ingress-controller` |
| 설치 직후 Service | NodePort(설정) | `LoadBalancer` (`<pending>`) |
| 호스트/경로 라우팅 | 동일 | 동일 |
| `Prefix` 경로 `/pre`로 `/prefix`, `/pre2` 요청 | **web** (요소 단위, 스펙대로) | **shop** (문자열 접두사로 일치) — **기본값이 스펙과 다름** |
| 위 옵션 변경 | — | `providers.kubernetesIngress.strictPrefixMatching=true`로 켜면 `/prefix`, `/pre2` → web (스펙대로) |
| `Exact` 경로 `/exact` 요청 | **api** (Exact 일치) | **web** (Exact 규칙 대신 `/` Prefix로 갔다. 이 설정에서 이유는 확인하지 못함) |
| `/exact/` 요청 | web | web (동일) |
| 알 수 없는 호스트 | HTML 404 (`nginx`) | 텍스트 `404 page not found` |
| 엔드포인트 없음 | 503 | 503 |
| 클라이언트 `X-Forwarded-For` | 덮어씀 | 덮어씀 (둘 다 기본 설정) |
| nginx 어노테이션 `rewrite-target` | 동작 (`/app/a/b` → `/a/b`) | **무시됨**, `/app/a/b` 요청이 404 (경로를 정규식이 아닌 문자열로 취급) |
| 설정 변경 반영 | nginx.conf 재생성 + reload (엔드포인트 변화는 reload 없음) | (reload 개념 없음 — 이 실험에서 관측하지 않음) |
| 잘못된 Ingress 사전 거부 | 어드미션 웹훅 | (이 차트 기본 구성에서 웹훅은 확인하지 못함) |
| 확장 방식 | 어노테이션 | 어노테이션 + 전용 CRD(`IngressRoute` 등, 차트 설치 시 `--providers.kubernetescrd` 활성) |

Traefik의 `strictPrefixMatching` 옵션은 `traefik --help`에서 `--providers.kubernetesingress.strictprefixmatching (Default: "false")`로 확인했고, 켠 뒤 `/prefix`, `/pre2`가 web으로 바뀌는 것까지 확인했다.

**같은 Ingress YAML이라도 컨트롤러에 따라 결과가 다를 수 있다.** 이 실험에서는 `Prefix`(`/prefix`, `/pre2`)와 `Exact`(`/exact`) 두 가지가 달랐다. 이식성을 믿고 컨트롤러를 바꾸면 경로 매칭이나 어노테이션 기능이 달라진다.

---

#### 5. IngressClass

Ingress가 **어느 컨트롤러가 처리할지**를 정하는 연결 고리. 클러스터 범위(`namespaced: false`) 리소스다.

```
$ kubectl get ingressclass
NAME      CONTROLLER                      PARAMETERS   AGE
nginx     k8s.io/ingress-nginx            <none>       3m8s
traefik   traefik.io/ingress-controller   <none>       12s
```

- 컨트롤러는 자기 `--controller-class` 값과 일치하는 `IngressClass`를 가진 Ingress만 처리한다.

##### 5-1. 클래스가 없거나 안 맞을 때 (실측)

| Ingress의 `ingressClassName` | nginx 응답 | Traefik 응답 |
|---|---|---|
| `nginx` | 처리 | 무시 |
| (없음, 기본 클래스도 없는 상태) | **404** (아무도 처리 안 함, `ADDRESS` 빈 칸) | (Traefik 쪽 응답은 확인하지 않음) |
| `traefik` | 404 (무시) | **처리 (api로 라우팅)** |

같은 클러스터에서 컨트롤러 두 개가 서로의 Ingress를 건드리지 않는다. 클래스 이름이 안 맞으면 오류도 없이 조용히 무시된다(`apply`는 성공, 요청만 404).

##### 5-2. 기본(default) IngressClass

```
$ kubectl annotate ingressclass nginx ingressclass.kubernetes.io/is-default-class=true
```

- **이미 만들어진** 클래스 없는 Ingress(`cls-none`)는 어노테이션을 붙인 뒤에도 `ingressClassName`이 `<none>`인 채 404였다. 기본 클래스는 **생성 시점에 API 서버가 채워 넣는** 값이다.
- 기본 클래스를 단 뒤 **새로 만든** 클래스 없는 Ingress(`nocls3`)는 `ingressClassName`이 `nginx`로 자동 채워졌고 라우팅됐다.
- 두 클래스에 기본 어노테이션을 모두 붙인 상태에서 클래스 없는 Ingress(`nocls2`)를 만들었더니 거부되지 않고 **`traefik`이 채워졌다**. 어떤 규칙으로 하나가 골라졌는지는 확인하지 못했다(기본 클래스를 둘 이상 두지 말 것).
- ingress-nginx는 `--watch-ingress-without-class` 플래그를 쓰면 클래스 없는 Ingress도 처리하는데, 이 실험에서는 플래그를 켜지 않았다(설정 안 됨을 확인).

---

#### 6. 선택 가이드와 함정

| 질문 | 답 (이 실험 기준) |
|---|---|
| Ingress만 만들었는데 안 된다 | 컨트롤러가 설치됐는지, `ingressClassName`이 클래스와 맞는지 먼저. 틀려도 `apply`는 성공하고 `ADDRESS`만 빈다 |
| 404와 503 | 404는 규칙이나 서비스를 못 찾음(존재하지 않는 Service 포함), 503은 규칙은 맞는데 Ready 엔드포인트가 없음 |
| Service를 안 거치나? | 컨트롤러는 EndpointSlice의 Pod IP로 직접 간다. Service의 `sessionAffinity`와 kube-proxy 분산은 적용 안 됨 |
| 롤링 업데이트 중 reload? | Pod 변경은 reload 없음, Ingress 규칙 변경은 reload |
| 어노테이션 이식성 | 컨트롤러별 어노테이션은 다른 컨트롤러에서 **무시된다**(Traefik이 `rewrite-target`을 무시해 404) |
| `Prefix` 동작 | 컨트롤러에 따라 다르다(Traefik 기본은 문자열 접두사). 스펙대로 쓰려면 옵션 확인 |
| 실제 클라이언트 IP | 기본 설정에서 `X-Forwarded-For`는 컨트롤러가 본 소스로 덮어써진다. 앞단 LB가 있으면 신뢰 설정 필요 |
| TLS 호스트의 HTTP 요청 | 기본 308 리다이렉트. 포트가 붙지 않아 비표준 포트에서는 그대로 쓰기 어렵다 |

##### 6-1. ingress-nginx는 지금 유지보수가 끝났다 (현재 시점 주의)

쿠버네티스 SIG Network와 보안 대응 위원회가 2025-11-11 공식 블로그에서 **Ingress NGINX 은퇴**를 발표했고, 최선(best-effort) 유지보수는 **2026년 3월**까지였다. 그 뒤로는 릴리스, 버그 수정, 보안 업데이트가 없다. 기존 설치는 계속 동작하고 설치 산출물도 남아 있다. 권장 경로는 **Gateway API** 또는 다른 Ingress 컨트롤러다. (출처: [Ingress NGINX Retirement: What You Need to Know](https://kubernetes.io/blog/2025/11/11/ingress-Nginx-retirement/), [SUSE 가이드](https://documentation.suse.com/cloudnative/rancher-manager/latest/en/cluster-admin/kubernetes-resources/load-balancer-and-ingress-controller/guide-to-ingress-retirement.html) — 검색 결과 요약을 확인한 것이고 본문 전체를 읽지는 않았다.)

- 이 실험의 ingress-nginx 4.15.1 / v1.15.1이 설치와 동작은 정상이었다. 은퇴 사실은 "동작 여부"가 아니라 **앞으로 보안 패치를 받지 못한다**는 뜻이다.
- 새로 구축한다면 ingress-nginx를 선택 기준에서 제외하는 편이 맞다. 이 책의 예제가 ingress-nginx 중심이라 학습용으로 쓰는 것과 운영 선택은 구분해야 한다.
- 이 실험 클러스터에는 Gateway API CRD가 설치돼 있지 않았다(`kubectl get crd | grep gateway` 결과 없음). Gateway API 자체는 실험하지 않았다.

---

#### 7. 확인하지 못한 부분

- **클라우드 로드밸런서와 함께 쓸 때의 동작**: `ADDRESS`에 LB 주소가 들어가는 것, 클라이언트 IP 보존(`externalTrafficPolicy`, proxy protocol)은 이 환경에서 못 봤다.
- **컨트롤러 Pod가 여러 개일 때의 분산, HA, `--election-id` 리더 선출**의 의미.
- **웹훅 장애 시 영향**: 컨트롤러가 죽어 있을 때 Ingress 생성이 실패하는지(`failurePolicy`)는 확인하지 않았다.
- **Traefik에서 `Exact` 경로(`/exact`)가 `/` Prefix에 밀린 이유**(우선순위 규칙 추정만 가능하고 확인 못 함), Traefik의 reload/어드미션 웹훅 여부, 전용 CRD(`IngressRoute`, `Middleware`)의 동작은 실험하지 않았다.
- **다른 컨트롤러**(HAProxy, Envoy 계열(Contour/Emissary), AWS ALB 컨트롤러, GKE 등)는 설치하지 않았다. 표의 비교는 ingress-nginx와 Traefik 둘뿐이다.
- **Gateway API**: CRD 미설치 상태만 확인했고 동작은 보지 못했다.
- **기본 IngressClass가 둘일 때 하나를 고르는 규칙**: 실험에서 `traefik`이 선택됐지만 이유는 모른다.
- **`defaultBackend`의 다른 컨트롤러 동작**, 그리고 컨트롤러 버전별 차이.
- **성능과 대용량 설정(Ingress 수천 개) 시의 reload 비용**, 연결 유지(keep-alive), WebSocket, gRPC, 타임아웃, 요청 크기 제한 같은 운영 튜닝.
- 이 실험은 노드 1개 kind(포트 매핑)에서 했으므로 멀티노드 클러스터에서 컨트롤러를 DaemonSet/`hostNetwork`로 배치하는 방식은 확인하지 않았다.
- **ingress-nginx 은퇴 정보**는 웹 검색 결과(공식 블로그 제목과 요약, 벤더 문서)로 확인했고, 블로그 원문 전체를 읽어 확인한 것은 아니다.
