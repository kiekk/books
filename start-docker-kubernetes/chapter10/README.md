### 10장. 보안을 위한 인증과 인가 - ServiceAccount와 RBAC

> 정리 범위: kubectl 명령어의 내부 처리 절차, ServiceAccount / Role / ClusterRole 개념, RoleBinding
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드, API 서버 `--authorization-mode=Node,RBAC`), kubectl v1.35.2
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다. 토큰 값은 노트에 싣지 않고 claim(내용)만 디코딩해서 적었다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`을 명시했다 (아래에서는 `k`로 줄여 적음). 실험은 `t10`, `t10-b` 네임스페이스와 이름에 `t10`이 들어간 ClusterRole/ClusterRoleBinding만 썼고 끝나고 모두 지웠다.

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. kubectl 명령어의 내부 처리 절차
2. ServiceAccount
3. Role과 ClusterRole
4. RoleBinding과 ClusterRoleBinding
5. 확인하지 못한 부분

---

#### 1. kubectl 명령어의 내부 처리 절차

##### 1-1. 전체 흐름

`kubectl get pods` 한 줄은 **kubectl(클라이언트)이 하는 일**과 **API 서버가 하는 일**로 나뉜다.

```
[kubectl]                                                    [kube-apiserver]
 ① kubeconfig 읽기 (서버 주소, 인증 정보)
 ② 리소스 이름 → API 경로 변환 (discovery, 캐시)
 ③ HTTP 요청 생성 ────────────────────────────────────────▶ ④ 인증 (Authentication)  누구인가
                                                             ⑤ 인가 (Authorization)   해도 되는가 (RBAC)
                                                             ⑥ 어드미션 (Admission)   변형/검증 (기본값 채우기, 쿼터)
                                                             ⑦ 스키마 검증 (Validation)
                                                             ⑧ etcd 저장 / 조회
 ⑩ 응답을 표로 출력 ◀──────────────────────────────────────── ⑨ 응답
```

##### 1-2. ① kubeconfig

```
$ k config view --minify --context docker-desktop      # 인증서 데이터는 생략해서 표시
clusters:
- cluster: {certificate-authority-data: ..., server: https://127.0.0.1:6443}
  name: docker-desktop
contexts:
- context: {cluster: docker-desktop, user: docker-desktop}
  name: docker-desktop
users:
- name: docker-desktop
  user: {client-certificate-data: ..., client-key-data: ...}
```

- kubeconfig는 **cluster(서버 주소 + CA), user(인증 정보), context(둘의 조합)** 로 이루어진다. kubectl은 `~/.kube/config`(`-v=6` 로그에 `Config loaded from file`)를 읽고 현재 컨텍스트의 서버로 요청한다.
- 이 환경의 사용자는 **클라이언트 인증서**로 인증한다. API 서버가 보는 신원은 다음과 같다.

```
$ k auth whoami
Username  docker-for-desktop
Groups    [system:masters system:authenticated]
```

  이 사용자는 `system:masters` 그룹이라 RBAC 검사를 우회하는 슈퍼유저다 (3절의 `cluster-admin` 바인딩 참고). 아래 권한 실험을 `--as`로 다른 신원을 흉내 내어 한 이유다.

##### 1-3. ② API 경로 찾기: discovery

`pods`, `deploy` 같은 이름을 `/api/v1/namespaces/<ns>/pods`, `/apis/apps/v1/...` 같은 경로로 바꾸려면 서버가 어떤 리소스를 제공하는지 알아야 한다. kubectl이 서버에 물어보고(**discovery**) 결과를 로컬에 캐시한다.

```
$ rm -rf ~/.kube/cache/discovery/127.0.0.1_6443        # 이 클러스터의 캐시만 삭제
$ k -n t10 get pods -v=6
GET https://127.0.0.1:6443/api?timeout=32s                          200 OK     ← 코어 그룹 목록
GET https://127.0.0.1:6443/apis?timeout=32s                         200 OK     ← 그 외 API 그룹 목록
GET https://127.0.0.1:6443/api/v1/namespaces/t10/pods?limit=500     200 OK     ← 실제 조회

$ k -n t10 get pods -v=6                                # 캐시가 있을 때 (같은 명령)
GET https://127.0.0.1:6443/api/v1/namespaces/t10/pods?limit=500     200 OK     ← 이것 하나뿐
```

- 캐시가 없으면 `/api`, `/apis`로 리소스 정보를 먼저 받고, 있으면 **실제 조회 요청 하나**로 끝난다. 캐시는 `~/.kube/cache/discovery/<서버주소>/`에 있다.
- 서버에 없는 kind를 쓰면 요청을 보내기 전에 kubectl이 실패한다.

```
$ k apply -f - <<EOF   # apiVersion: v1, kind: Deployment
error: resource mapping not found for name: "x" namespace: "" from "STDIN": no matches for kind "Deployment" in version "v1"
ensure CRDs are installed first
```

##### 1-4. ③ 요청 형태: `get`은 GET, 서버가 표를 만들어 준다

```
$ k -n t10 get pods -v=8
Accept: application/json;as=Table;v=v1;g=meta.k8s.io,application/json;as=Table;v=v1beta1;g=meta.k8s.io,application/json
User-Agent: kubectl/v1.35.2 (darwin/arm64) kubernetes/fdc9d74
```

- `Accept: ...as=Table`로 **서버에게 "표 형식(Table)"으로 달라고 요청**한다. `NAME READY STATUS RESTARTS AGE` 같은 열은 서버가 정해서 보내 주고 kubectl은 그대로 출력한다. (그래서 CRD로 만든 리소스도 서버가 정의한 열로 나온다.)
- 응답 헤더에는 `X-Kubernetes-Pf-Flowschema-Uid`, `X-Kubernetes-Pf-Prioritylevel-Uid`가 있다. API 서버가 요청을 분류해서 우선순위와 공정성(API Priority and Fairness)으로 처리한다는 표시다 (이 기능의 동작은 실험하지 않았다).

##### 1-5. 명령별 HTTP 요청 (실측, `-v=6`)

| kubectl | HTTP 요청 |
|---|---|
| `get pods` | `GET /api/v1/namespaces/<ns>/pods?limit=500` |
| `get pods -w` | `GET .../pods?...` 후 `GET .../pods?resourceVersion=N&watch=true` (8장) |
| `run mini --image=...` | `POST /api/v1/namespaces/t10/pods?fieldManager=kubectl-run` → `201 Created` |
| `apply -f` (새 오브젝트) | `GET /openapi/v3` → `GET /openapi/v3/api/v1?hash=...` → `GET .../configmaps/demo` (**404**) → `GET /api/v1/namespaces/t10` → `POST .../configmaps?fieldManager=kubectl-client-side-apply&fieldValidation=Strict` (**201**) |
| `apply -f` (변경 없음) | `GET /openapi/v3` ... → `GET .../configmaps/demo` (200) → **쓰기 요청 없음**, `unchanged` |
| `apply -f` (변경 있음) | 위와 같이 GET 후 `PATCH .../configmaps/demo?fieldManager=kubectl-client-side-apply&fieldValidation=Strict` (**200**) |

`apply`의 단계:

1. **OpenAPI 스키마를 받는다** (`/openapi/v3`). 클라이언트 쪽에서 YAML을 검증하는 데 쓴다.
2. **현재 오브젝트를 `GET`** 한다. 404면 새로 만들고(`POST`), 있으면 비교한다.
3. 대상 네임스페이스가 있는지 `GET /api/v1/namespaces/t10`으로 확인한다.
4. 비교 결과 **달라진 것이 있을 때만 `PATCH`** 한다. 같으면 아무것도 쓰지 않는다 (`configmap/demo unchanged`).
5. 요청에 `fieldManager=kubectl-client-side-apply`가 붙어서 "누가 이 필드를 바꿨는지"가 오브젝트의 `managedFields`에 기록된다. `fieldValidation=Strict`는 모르는 필드를 오류로 만든다 (아래).

```
$ k -n t10 get cm demo -o jsonpath='{.metadata.annotations}'
{"kubectl.kubernetes.io/last-applied-configuration":"{\"apiVersion\":\"v1\",\"data\":{\"a\":\"2\"},\"kind\":\"ConfigMap\",\"metadata\":{\"annotations\":{},\"name\":\"demo\",\"namespace\":\"t10\"}}\n"}
```

- 클라이언트 측 `apply`는 **"마지막으로 적용한 YAML"을 오브젝트의 어노테이션에 저장**해 두고, 다음 `apply`에서 이전/현재/라이브 상태를 비교해(3-way) 패치를 만든다. `kubectl create`로 만든 오브젝트에는 이 어노테이션이 없어서 7장에서 본 경고(`missing the kubectl.kubernetes.io/last-applied-configuration annotation`)가 났다.

##### 1-6. kubectl이 채우는 것 vs 서버가 채우는 것

```
$ k -n t10 run mini2 --image=nginx:alpine --dry-run=client -o yaml
spec:
  containers: [{image: nginx:alpine, name: mini2, resources: {}}]
  dnsPolicy: ClusterFirst
  restartPolicy: Always                       # kubectl이 만든 것은 여기까지

$ k -n t10 run mini2 --image=nginx:alpine --dry-run=server -o yaml
metadata: {uid: a6ce22c5-..., ...}
spec:
  enableServiceLinks: true
  preemptionPolicy: PreemptLowerPriority
  priority: 0
  schedulerName: default-scheduler
  serviceAccount: default
  serviceAccountName: default
  terminationGracePeriodSeconds: 30
  tolerations: [node.kubernetes.io/not-ready, node.kubernetes.io/unreachable]
  volumes: [{name: kube-api-access-pxhvg, ...}]       # ServiceAccount 토큰 볼륨
$ k -n t10 get pod mini2
Error from server (NotFound): pods "mini2" not found     # 서버 dry-run은 저장하지 않는다
```

- `--dry-run=client`는 **kubectl이 로컬에서만** 만든 YAML이다 (서버에 보내지 않는다).
- `--dry-run=server`는 요청을 **API 서버까지 보내 인증/인가/어드미션/검증을 거치게 하되 etcd에는 저장하지 않는다.** 응답이 어드미션 이후의 최종 모습이다. `uid`, `serviceAccountName: default`, 톨러레이션, 스케줄러 이름, 토큰 볼륨 등 **서버(어드미션/기본값)가 채운 것**이 보인다. 7장에서 본 "모든 Pod에 자동으로 들어가는 ServiceAccount 토큰 마운트"는 이 단계에서 붙는다.

##### 1-7. ④~⑦ API 서버의 처리 순서 (실측)

**④ 인증 (Authentication): 누구인가**

```
$ curl -sk https://127.0.0.1:6443/api/v1/namespaces/t10/pods                      # 인증 정보 없음
403 Forbidden - pods is forbidden: User "system:anonymous" cannot list resource "pods" ...
$ curl -sk -H 'Authorization: Bearer not-a-real-token' https://.../pods           # 잘못된 토큰
401 Unauthorized - Unauthorized
$ curl -sk https://127.0.0.1:6443/version                                          # 인증 없이도 허용되는 경로
{ "major": "1", "minor": "32", "gitVersion": "v1.32.2", ... }
```

| 요청 | 결과 | 의미 |
|---|---|---|
| 인증 정보 없음 | **403** (`system:anonymous`) | 익명 사용자로 처리되어 인가 단계에서 거부 |
| 잘못된 토큰 | **401** | 인증 단계에서 거부 (신원을 알 수 없음) |
| `/version` | 200 | 공개된 정보 경로는 익명도 허용 |

- 401은 "누군지 모르겠다", 403은 "누군지는 알지만 권한이 없다"다. 익명 요청이 401이 아니라 403인 것은 이 클러스터가 익명 요청을 `system:anonymous`로 받아들이기 때문이다.
- 인증 방식은 클라이언트 인증서, Bearer 토큰(ServiceAccount 토큰, OIDC 등) 등이다. 이 장에서 실제로 쓴 것은 **클라이언트 인증서와 ServiceAccount 토큰**이다.

**⑤ 인가 (Authorization): 해도 되는가**

```
$ k --as=alice -n t10 get pods
Error from server (Forbidden): pods is forbidden: User "alice" cannot list resource "pods" in API group "" in the namespace "t10"
$ k --as=alice auth can-i get pods -n t10
no
```

- 거부 메시지가 **누가(User) / 무엇을(list) / 어떤 리소스(pods, API 그룹 "") / 어디서(namespace t10)** 를 모두 알려 준다. RBAC 문제를 디버깅할 때 이 문장을 그대로 읽으면 된다.
- 신원은 `--as`로 흉내 낼 수 있다 (가장, impersonation). 이 환경의 사용자는 `system:masters`라서 가능했다.

**인가가 검증보다 먼저다**

```
$ k --as=alice -n t10 apply -f - <<EOF     # 이름이 잘못된 Pod(Bad_Name)
Error from server (Forbidden): ... pods "Bad_Name" is forbidden: User "alice" cannot get resource "pods" ...
```

권한이 없는 사용자에게는 스키마 오류 대신 **403이 먼저** 나왔다 (잘못된 입력 내용을 알려 주지 않는다).

**⑥ ⑦ 어드미션과 검증**

```
# 모르는 필드 (kubectl의 fieldValidation=Strict)
Error from server (BadRequest): ... strict decoding error: unknown field "spec.containers[0].bogusField"
# 이름 규칙 위반
The Pod "Bad_Name" is invalid: metadata.name: Invalid value: "Bad_Name": a lowercase RFC 1123 subdomain must consist of ...
# 필수 필드 누락
The Pod "nocontainer" is invalid: spec.containers: Required value
# 변경할 수 없는 필드 수정
$ k -n t10 patch pod mini -p '{"spec":{"nodeName":"x"}}'
The Pod "mini" is invalid: spec: Forbidden: pod updates may not change fields other than `spec.containers[*].image`, `spec.initContainers[*].image`, `spec.activeDeadlineSeconds`, `spec.tolerations` (only additions to existing tolerations), ...
```

- Pod 업데이트에서 바꿀 수 있는 필드는 이미지, `activeDeadlineSeconds`, 톨러레이션(추가만) 등 소수이고, 나머지를 바꾸면 검증에서 거부된다. 같은 요청의 `image`만 바꾼 패치는 성공했다 (`pod/mini patched`).
- **어드미션**: 인가를 통과한 뒤 요청을 **변형**(기본값/ServiceAccount 토큰 볼륨/톨러레이션 추가: 1-6절의 서버 dry-run 결과)하거나 **검증**(7장의 ResourceQuota가 초과 요청 거부, 8장의 ingress-nginx 웹훅이 중복 Ingress 거부)한다.
- 이 클러스터의 API 서버 옵션은 `--enable-admission-plugins=NodeRestriction`(6장에서 확인)이고 이 외에 기본으로 켜지는 플러그인들이 있다. 전체 목록은 확인하지 않았다.

##### 1-8. 주의: `kubectl --token`은 kubeconfig의 클라이언트 인증서를 이기지 못한다

ServiceAccount 토큰으로 kubectl을 쓰려고 `--token`을 줬는데 **여전히 관리자 신원**이었다.

```
$ k --token="$SA_TOKEN" auth whoami
Username  docker-for-desktop
Groups    [system:masters system:authenticated]            # SA가 아니라 관리자!
$ k --token="$SA_TOKEN" -n t10 delete pod web
pod "web" deleted from t10 namespace                          # 권한 없는 SA인데도 삭제됨

# kubeconfig를 쓰지 않고 서버/CA/토큰만 지정
$ kubectl --kubeconfig=/dev/null --server=https://127.0.0.1:6443 --certificate-authority=ca.crt --token="$SA_TOKEN" auth whoami
Username  system:serviceaccount:t10:app-sa
$ ... delete pod web
Error from server (Forbidden): pods "web" is forbidden: User "system:serviceaccount:t10:app-sa" cannot delete resource "pods" ...
```

- kubeconfig의 사용자에 **클라이언트 인증서가 있으면 `--token`을 줘도 인증서로 인증**되었다. 권한 테스트를 하는 줄 알았는데 관리자로 실행되고 있었던 것이다. 다른 신원의 권한을 확인하려면 `--as`를 쓰거나, 위처럼 **kubeconfig 없이** 서버/CA/토큰만 지정해야 한다.
- 이 때문에 아래 3, 4절의 SA 토큰 실험은 `curl`로 직접 호출하거나 kubeconfig 없는 kubectl로 한 것이다.

#### 2. ServiceAccount

##### 2-1. 개념

**사용자(User)는 사람, ServiceAccount는 클러스터 안에서 실행되는 프로세스(Pod)의 신원**이다. Pod가 API 서버를 호출할 때(컨트롤러, 모니터링 에이전트, CI 등) 이 신원으로 인증한다.

| 구분 | User | ServiceAccount |
|---|---|---|
| 대상 | 사람, 외부 시스템 | Pod 안의 프로세스 |
| 쿠버네티스 오브젝트 | **없다** (인증서/OIDC 등 외부에서 관리) | **있다** (네임스페이스에 속함) |
| 이름 형식 | 인증 방식에 따름 (`docker-for-desktop`) | `system:serviceaccount:<네임스페이스>:<이름>` |
| 속한 그룹 | 인증 정보에 따름 | `system:serviceaccounts`, `system:serviceaccounts:<네임스페이스>`, `system:authenticated` |

```
$ k auth whoami --as=system:serviceaccount:t10:app-sa
Username  system:serviceaccount:t10:app-sa
Groups    [system:serviceaccounts system:serviceaccounts:t10 system:authenticated]
```

- 모든 네임스페이스에는 `default` ServiceAccount가 자동으로 생긴다 (`k -n t10 get sa` → `default`, 7장의 네임스페이스 설명과 같은 내용). Pod에 `serviceAccountName`을 쓰지 않으면 `default`로 실행된다 (1-6절의 서버 dry-run에서도 `serviceAccountName: default`).
- 새 ServiceAccount는 **아무 권한도 없다.** `default`도 마찬가지다 (`k auth can-i list pods -n t10 --as=system:serviceaccount:t10:default` → `no`). 권한 없는 SA가 할 수 있는 것은 자기 신원 확인과 공개 정보 조회 정도다.

```
$ k auth can-i --list --as=system:serviceaccount:t10:app-sa -n t10     # 바인딩이 없는 SA
selfsubjectreviews.authentication.k8s.io        [create]
selfsubjectaccessreviews.authorization.k8s.io   [create]
selfsubjectrulesreviews.authorization.k8s.io    [create]
[/api/*] [get]  [/api] [get]  [/healthz] ... (공개된 discovery/version/health 경로들)
```

##### 2-2. 토큰

**토큰 요청 API (`kubectl create token`)** 로 만든 토큰을 디코딩했다 (서명 부분은 보지 않고 payload만).

```
$ k -n t10 create sa app-sa
$ k -n t10 get sa app-sa -o jsonpath='{.secrets}'        →  (비어 있음)
$ k -n t10 get secret                                     →  No resources found      # SA마다 토큰 Secret이 자동 생성되지 않음
$ k -n t10 create token app-sa      # payload
{iss: https://kubernetes.default.svc.cluster.local, sub: system:serviceaccount:t10:app-sa,
 aud: [https://kubernetes.default.svc.cluster.local], exp - iat = 3600초,
 kubernetes.io: {namespace: t10, serviceaccount: {name: app-sa}}}
$ k -n t10 create token app-sa --duration=10m        # exp - iat = 600초
```

- 현재 방식은 **만료가 있는 JWT를 필요할 때 발급**받는 것이다. 기본 유효 시간은 1시간(3600초)이고 `--duration`으로 바꿀 수 있다. 예전처럼 SA를 만들 때 영구 토큰 Secret이 자동 생성되지 않는다 (SA의 `secrets`가 비어 있음).
- `sub`가 `system:serviceaccount:t10:app-sa`이고 `iss`/`aud`가 클러스터 주소다. API 서버가 서명과 함께 이 값들을 검증한다.

**Pod 안에서는 자동으로 토큰이 마운트된다**

```
$ k -n t10 exec sa-pod -- ls -l /var/run/secrets/kubernetes.io/serviceaccount/
ca.crt -> ..data/ca.crt    namespace -> ..data/namespace    token -> ..data/token
$ k -n t10 exec sa-pod -- mount | grep serviceaccount
tmpfs on /run/secrets/kubernetes.io/serviceaccount type tmpfs (ro,relatime,size=7922748k,...)
$ k -n t10 exec sa-pod -- cat .../namespace            →  t10
$ (Pod 안에서 토큰 payload)
sub= system:serviceaccount:t10:app-sa   ttl_s= 31536000   aud= ['https://kubernetes.default.svc.cluster.local']
bound to pod: sa-pod | serviceaccount: app-sa
$ k -n t10 exec no-mount -- ls /var/run/secrets/kubernetes.io/     (automountServiceAccountToken: false) → 디렉터리 없음 (exit 1)
```

- **`ca.crt`(API 서버를 검증할 CA), `namespace`, `token`** 세 파일이 tmpfs(읽기 전용)로 마운트된다 (7장의 Secret 볼륨과 같은 방식). 1-6절의 서버 dry-run에서 본 `kube-api-access-xxxxx` 볼륨이 이것이다.
- Pod 안의 토큰에는 **Pod 정보가 묶여(bound)** 있다 (`kubernetes.io`의 `pod.name = sa-pod`).
- Pod 안 토큰의 `exp - iat`이 **31536000초(1년)** 로 측정되었다. `kubectl create token`의 기본값(1시간)과 다르다. 이 클러스터의 API 서버 설정(토큰 유효 기간 연장 옵션일 수 있음)과 관련 있어 보이지만 확인하지 않았다.
- `automountServiceAccountToken: false`로 **토큰 마운트를 끌 수 있다.** API를 호출할 일이 없는 Pod는 끄는 것이 안전하다 (토큰이 탈취될 경로 자체를 없앤다).
- Pod 안에서 API 서버 주소는 환경 변수와 Service로 제공된다: `KUBERNETES_SERVICE_HOST=10.96.0.1`, `KUBERNETES_SERVICE_PORT=443`, 이름은 `kubernetes.default.svc`.

**Pod 안에서 API 호출**

```
$ k -n t10 exec sa-pod -- sh -c 'S=/var/run/secrets/kubernetes.io/serviceaccount; TOKEN=$(cat $S/token); \
    curl -s -o /dev/null -w "%{http_code}" --cacert $S/ca.crt -H "Authorization: Bearer $TOKEN" https://kubernetes.default.svc/<경로>'
api/v1/namespaces/t10/pods          200
api/v1/namespaces/t10/secrets       403
api/v1/namespaces/t10-b/pods        403
```

(이 SA에는 3-2절의 Role이 연결되어 있다. `t10`의 Pod 조회만 허용된 상태다.)

**토큰이 무효가 되는 경우**

| 상황 | 결과 (실측) |
|---|---|
| Pod 삭제 후 그 Pod의 토큰 사용 | 삭제 전 200 → 삭제 후 **401** (Pod에 묶인 토큰이라 Pod가 사라지면 무효) |
| ServiceAccount 삭제 | 토큰이 **약 11초 뒤** 401 (삭제 직후에는 계속 200) |
| 같은 이름 SA를 다시 만든 뒤 옛 토큰 | **401** (UID가 다른 SA라서 무효) |
| 레거시 토큰 Secret (`type: kubernetes.io/service-account-token`) 삭제 | 토큰이 **약 11초 뒤** 401 |

- SA/Secret을 지워도 **약 11초 동안 토큰이 계속 통과**했다. 방금 한 번 사용된(검증된) 토큰의 인증 결과를 API 서버가 잠깐 캐시하는 것으로 보인다 (한 번도 쓰지 않은 토큰은 SA를 지운 직후 바로 401이었다). 11초는 두 번 측정한 값이고 캐시 TTL 설정은 확인하지 않았다.
- **레거시 방식**: `type: kubernetes.io/service-account-token` Secret에 `kubernetes.io/service-account.name: app-sa` 어노테이션을 달면 컨트롤러가 토큰을 채운다.

```
$ k -n t10 get secret app-sa-token -o jsonpath='{.type} keys={.data}'
kubernetes.io/service-account-token  keys={"ca.crt":<...>,"namespace":"dDEw","token":<...>}
(payload claims) iss, sub, kubernetes.io/serviceaccount/{namespace, secret.name, service-account.name, service-account.uid}      ← exp 없음
$ curl ... -H "Authorization: Bearer <legacy token>"      →  200
```

  이 토큰은 **`exp`가 없는 영구 토큰**이다 (Secret을 지우기 전까지 유효). 유출되면 위험하므로 특별한 이유가 없으면 쓰지 않고 TokenRequest(`create token`, 프로젝션 볼륨)를 쓴다.

#### 3. Role과 ClusterRole

##### 3-1. 개념

**"무엇을 할 수 있는가"를 규칙 목록으로 적은 오브젝트.** RBAC(Role-Based Access Control)는 **허용 규칙만** 있다 (거부 규칙이 없다). 규칙이 하나도 맞지 않으면 거부다.

| 오브젝트 | 범위 | 용도 |
|---|---|---|
| **Role** | **네임스페이스** | 그 네임스페이스의 리소스에 대한 권한 |
| **ClusterRole** | **클러스터 전체** (네임스페이스 없음) | ① 클러스터 범위 리소스(nodes, pv ...) ② 비-리소스 URL(`/healthz`) ③ 여러 네임스페이스에서 재사용하는 공통 권한 템플릿 |

규칙의 구조:

```yaml
rules:
- apiGroups: [""]                          # "" = 코어 그룹(pods, services ...), "apps", "batch" ...
  resources: ["pods", "pods/log"]          # 리소스와 하위 리소스
  verbs: ["get", "list", "watch"]          # 동작
- apiGroups: [""]
  resources: ["configmaps"]
  resourceNames: ["cfg1"]                  # 특정 이름의 오브젝트만
  verbs: ["get"]
```

| 필드 | 의미 |
|---|---|
| `apiGroups` | API 그룹 (`""`=코어, `apps`, `rbac.authorization.k8s.io` ...) |
| `resources` | 리소스 종류. `pods/log`, `pods/exec`처럼 **하위 리소스는 따로 적어야 한다** |
| `verbs` | `get`, `list`, `watch`, `create`, `update`, `patch`, `delete`, `deletecollection` (+ `escalate`, `bind`, `impersonate` 등 특수 동사) |
| `resourceNames` | 이름으로 제한 |
| `nonResourceURLs` | (ClusterRole만) `/healthz` 같은 경로 |

##### 3-2. Role만으로는 아무 일도 일어나지 않는다 (실측)

`pod-reader` Role(위 규칙)을 만든 뒤 **바인딩 없이** 확인했다.

```
$ k apply -f role.yaml                     # role.rbac.authorization.k8s.io/pod-reader created
$ k auth can-i list pods -n t10 --as=system:serviceaccount:t10:app-sa
no                                          # Role이 있어도 누구에게도 연결되지 않았으므로 거부
```

권한은 **Role(규칙) + RoleBinding(누구에게)** 이 합쳐져야 생긴다 (4절).

`app-sa`에 이 Role을 RoleBinding으로 연결한 뒤의 권한 표 (`k auth can-i ... --as=system:serviceaccount:t10:app-sa`):

| 요청 | 결과 | 이유 |
|---|---|---|
| list / get / watch pods (t10) | yes | 규칙에 있음 |
| delete / create pods (t10) | no | 규칙에 없는 동사 |
| list pods (t10-b) | no | **Role은 자기 네임스페이스(t10)에만** 적용 |
| list pods -A (전체 네임스페이스) | no | 네임스페이스 범위 Role로는 불가 |
| get pods/log | yes | 하위 리소스를 따로 적어 둠 |
| create pods/exec | no | `pods`와 `pods/exec`는 별개 |
| list secrets | no | 규칙에 없음 |
| get configmap/cfg1 | yes | `resourceNames: [cfg1]` |
| get configmap/cfg2 | no | 이름이 다름 |
| list configmaps | no | `resourceNames`를 쓴 규칙은 `get`만 허용했고 `list`는 허용하지 않음 |
| get nodes | no | 클러스터 범위 리소스는 네임스페이스 Role로 불가 |

같은 결과를 **실제 토큰으로 API에 호출**해서도 확인했다 (`curl -H "Authorization: Bearer <token>"`).

```
/api/v1/namespaces/t10/pods                    200
/api/v1/namespaces/t10-b/pods                  403
/api/v1/namespaces/t10/secrets                 403
/api/v1/namespaces/t10/configmaps/cfg1         200
/api/v1/namespaces/t10/configmaps/cfg2         403
```

- `pods`에 대한 권한이 `pods/log`, `pods/exec`, `pods/portforward`까지 자동으로 포함되지 않는다. 하위 리소스가 별도 항목이라는 것이 `exec` 권한을 따로 통제할 수 있게 해 준다.
- `resourceNames`를 쓰면 `list`/`watch`/`create`로는 이름 제한이 걸리지 않는다는 점에 주의 (이름으로 제한할 수 있는 동사는 `get`, `update`, `patch`, `delete`). 여기서는 `get`만 허용해서 `list configmaps`는 `no`였다.

##### 3-3. ClusterRole의 세 가지 쓰임

**① 클러스터 범위 리소스 (nodes)**

```
$ k create clusterrole t10-node-reader --verb=get,list --resource=nodes
$ k -n t10 create rolebinding nr --clusterrole=t10-node-reader --serviceaccount=t10:app-sa     # RoleBinding으로 연결
$ k auth can-i list nodes --as=system:serviceaccount:t10:app-sa        →  no      # 네임스페이스 바인딩으로는 불가
$ k create clusterrolebinding t10-nr --clusterrole=t10-node-reader --serviceaccount=t10:app-sa  # ClusterRoleBinding으로 연결
$ k auth can-i list nodes --as=system:serviceaccount:t10:app-sa        →  yes
```

**② 여러 네임스페이스에서 재사용 + RoleBinding으로 범위 축소**

```
$ k create clusterrole t10-deploy-viewer --verb=get,list --resource=deployments.apps
$ k -n t10-b create rolebinding dv --clusterrole=t10-deploy-viewer --serviceaccount=t10:app-sa
   # 바인딩은 t10-b에 있고, 대상 SA는 t10의 SA
$ list deployments -n t10-b   →  yes
$ list deployments -n t10     →  no
$ list deployments -A         →  no
```

- ClusterRole을 **RoleBinding으로 연결하면 그 RoleBinding이 있는 네임스페이스에서만** 권한이 생긴다. 공통 권한 템플릿(예: "deployment 읽기")을 ClusterRole 하나로 만들어 네임스페이스마다 연결할 수 있다.
- 이 실험에서 **바인딩이 위치한 네임스페이스(t10-b)가 권한 범위**이고, 주체(SA)가 어느 네임스페이스에 있는지는 상관없었다.

**③ 모든 네임스페이스에 적용 (ClusterRoleBinding)**

```
$ k create clusterrolebinding t10-dv-all --clusterrole=t10-deploy-viewer --serviceaccount=t10:app-sa
$ list deployments -A / -n kube-system   →  yes / yes
$ create deployments -n t10              →  no       # 규칙에 없는 동사는 여전히 no
```

##### 3-4. 기본 제공 ClusterRole

```
$ k get clusterrole --no-headers | wc -l            →  68
$ k get clusterrole --no-headers | grep -c '^system:'  →  61
$ k get clusterrole cluster-admin -o jsonpath='{.rules}'
[{"apiGroups":["*"],"resources":["*"],"verbs":["*"]},{"nonResourceURLs":["*"],"verbs":["*"]}]
$ k get clusterrolebinding cluster-admin -o jsonpath='{.subjects}{" -> "}{.roleRef.name}'
[{"apiGroup":"rbac.authorization.k8s.io","kind":"Group","name":"system:masters"}] -> cluster-admin
```

- 68개 중 61개가 `system:`으로 시작하는 시스템 컴포넌트용(스케줄러, kubelet, 컨트롤러 등)이고 나머지가 사용자용 `cluster-admin`, `admin`, `edit`, `view` 등이다.
- `cluster-admin`은 모든 리소스, 모든 동사, 모든 URL이다. `system:masters` 그룹이 이것에 바인딩되어 있어, 1-2절의 이 환경 사용자(`system:masters` 그룹)가 모든 권한을 갖는다.

**`view`, `edit`, `admin`의 차이** (각 ClusterRole을 한 네임스페이스에 RoleBinding으로 연결한 SA로 확인)

| 요청 (t10) | view | edit | admin |
|---|---|---|---|
| list pods / deployments / services | yes | (yes) | (yes) |
| get pods/log | yes | | |
| **list secrets** | **no** | **yes** | **yes** |
| create pods/exec | no | | |
| create pods | no | yes | yes |
| create roles | | no | **yes** |
| create rolebindings | | no | **yes** |
| create resourcequotas | | no | no |
| update namespaces | | no | no |

(빈 칸은 이번 실험에서 확인하지 않은 항목이고, 괄호 항목은 `view`가 읽기 권한을 갖는 것에서 비롯되는 값으로 직접 확인하지는 않았다.)

- **`view`는 읽기 전용이고 Secret은 못 읽는다.** Secret까지 읽을 수 있는 `edit`/`admin`과 큰 차이다.
- `edit`는 Pod/Deployment/Secret을 읽고 쓰지만 Role/RoleBinding은 만들 수 없다. `admin`은 네임스페이스 안의 RBAC까지 관리하지만 `resourcequotas` 변경과 네임스페이스 자체의 수정은 못 한다 (쿼터와 네임스페이스는 `cluster-admin` 영역).

**집계(aggregation) ClusterRole**

```
$ k get clusterrole view -o jsonpath='{.aggregationRule.clusterRoleSelectors[*].matchLabels}'
{"rbac.authorization.k8s.io/aggregate-to-view":"true"}            # edit: aggregate-to-edit, admin: aggregate-to-admin
```

- `view`/`edit`/`admin`은 규칙을 직접 가지는 대신 **라벨이 붙은 다른 ClusterRole의 규칙을 합쳐서** 만들어진다. 새 CRD를 만들 때 `aggregate-to-view: "true"` 라벨을 단 ClusterRole을 함께 만들면 기본 `view`에도 그 권한이 들어간다.

```
$ k apply -f - <<EOF     # ClusterRole t10-aggregate-to-view (라벨 aggregate-to-view=true), widgets.t10.example.com get/list
$ (4초 뒤) k get clusterrole view ...   →  {"apiGroups":["t10.example.com"],"resources":["widgets"],"verbs":["get","list"]}   # view에 규칙이 합쳐짐
$ k auth can-i --list -n t10 --as=system:serviceaccount:t10:viewer
widgets.t10.example.com      [get list]                           # view를 바인딩한 SA의 권한에 반영
$ k auth can-i list widgets.t10.example.com -n t10 --as=...viewer
Warning: the server doesn't have a resource type 'widgets' in group 't10.example.com'
no                                                                 # (아래 주의)
```

- 집계가 실제로 동작해서 몇 초 안에 `view`에 새 규칙이 합쳐졌고 `can-i --list`에도 나타났다.
- 주의: **서버에 없는 리소스 종류(CRD가 없는 `widgets`)에 `can-i`를 하면 `no`가 나온다.** 위 경우 규칙은 있지만 `can-i`가 `no`를 답했다. 권한 점검 도구의 한계이므로 `--list`로 규칙을 직접 보는 것이 정확하다.

#### 4. RoleBinding과 ClusterRoleBinding

##### 4-1. 개념

**"누구(subjects)에게 어떤 Role(roleRef)을 줄지"를 연결하는 오브젝트.**

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata: {name: read-pods, namespace: t10}
subjects:                                  # 누구에게
- kind: ServiceAccount
  name: app-sa
  namespace: t10
roleRef:                                   # 어떤 Role을
  kind: Role                               # Role 또는 ClusterRole
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io
```

| 오브젝트 | 범위 | 연결할 수 있는 Role |
|---|---|---|
| **RoleBinding** | **자기 네임스페이스** | Role 또는 ClusterRole |
| **ClusterRoleBinding** | **클러스터 전체** | ClusterRole만 |

| 조합 | 결과 (3절 실측) |
|---|---|
| Role + RoleBinding | 그 네임스페이스에서만 |
| ClusterRole + RoleBinding | **바인딩의 네임스페이스에서만** (3-3의 ②) |
| ClusterRole + ClusterRoleBinding | 모든 네임스페이스 + 클러스터 범위 리소스 |
| ClusterRole(nodes 등) + RoleBinding | 클러스터 범위 리소스는 **효과 없음** (3-3의 ①: `no`) |

```
$ k -n t10 create rolebinding read-pods --role=pod-reader --serviceaccount=t10:app-sa
$ k -n t10 get rolebinding read-pods -o jsonpath='{.roleRef} {.subjects}'
{"apiGroup":"rbac.authorization.k8s.io","kind":"Role","name":"pod-reader"} [{"kind":"ServiceAccount","name":"app-sa","namespace":"t10"}]
```

##### 4-2. subjects 종류

| kind | 예 | 비고 |
|---|---|---|
| `ServiceAccount` | `app-sa` (namespace 필수) | Pod의 신원 |
| `User` | `bob` | 인증 방식이 알려 주는 사용자 이름 (쿠버네티스 오브젝트 없음) |
| `Group` | `system:serviceaccounts:t10` | 인증 정보에 포함된 그룹 |

```
# User 추가: roleRef는 못 바꾸지만 subjects는 바꿀 수 있다
$ k -n t10 patch rolebinding read-pods --type=json -p '[{"op":"add","path":"/subjects/-","value":{"kind":"User","name":"bob","apiGroup":"rbac.authorization.k8s.io"}}]'
$ k auth can-i list pods -n t10 --as=bob      →  yes

# 그룹으로 바인딩: t10의 모든 ServiceAccount에게 view
$ k -n t10 create rolebinding all-sas-read --clusterrole=view --group=system:serviceaccounts:t10
t10의 SA (editor)            list services →  yes
다른 네임스페이스(t10-b)의 SA list services →  no
```

- 2-1절의 `system:serviceaccounts:<네임스페이스>` 그룹을 쓰면 **한 네임스페이스의 모든 SA에게 한 번에** 권한을 줄 수 있다.
- 사용자(User)나 그룹은 쿠버네티스 안에 오브젝트가 없다. 바인딩은 **이름이 일치하는지만** 본다.

##### 4-3. 주의할 동작 (실측)

**① `roleRef`는 바꿀 수 없다**

```
$ k -n t10 patch rolebinding read-pods --type=merge -p '{"roleRef":{"name":"other"}}'
The RoleBinding "read-pods" is invalid: roleRef: Invalid value: ...: cannot change roleRef
```

다른 Role을 가리키려면 **삭제 후 다시 만들어야 한다.** 권한 상승을 막고 바인딩의 의미가 바뀌는 것을 방지한다.

**② 없는 Role을 참조하는 바인딩도 만들 수 있다**

```
$ k -n t10 create rolebinding ghost --role=nope --user=carol      # Role nope는 없음
rolebinding.rbac.authorization.k8s.io/ghost created
$ k -n t10 apply -f role-nope.yaml                                 # 나중에 Role 생성
$ k auth can-i list configmaps -n t10 --as=carol                   →  yes   # Role이 생기자 즉시 유효
```

바인딩 시점에는 Role의 존재를 검사하지 않는다. Role 이름의 오타는 오류 없이 만들어지고 권한이 없는 상태로 남으므로 주의.

**③ 권한은 합쳐지고(union), 거부 규칙은 없다**

```
# app-sa에 pod-reader(RoleBinding read-pods) + cm-writer(create/update configmaps, RoleBinding cmw)
list pods          yes       (pod-reader)
create configmaps  yes       (cm-writer)
update configmaps  yes       (cm-writer)
delete configmaps  no        (어디에도 없음)
```

- 한 주체에 바인딩이 여러 개면 **허용이 합쳐진다.** 특정 권한만 빼는 "거부" 규칙은 없다.
- 권한을 회수하려면 **그 권한을 주는 모든 경로**를 없애야 한다.

```
$ k -n t10 delete rolebinding read-pods
$ k auth can-i list pods -n t10 --as=system:serviceaccount:t10:app-sa        →  yes     # 아직 yes!
   (남은 바인딩: all-sas-read(view, 그룹 system:serviceaccounts:t10), nr(...) 등이 pods 조회를 계속 허용)
$ (그룹 바인딩 등 관련 바인딩을 모두 삭제한 뒤)
$ k auth can-i list pods -n t10 --as=system:serviceaccount:t10:app-sa        →  no
```

  `read-pods`를 지웠는데도 `yes`였던 것은 그룹 바인딩(`all-sas-read`)이 같은 권한을 주고 있었기 때문이다. 권한 문제를 추적할 때는 `kubectl get rolebinding,clusterrolebinding -A -o wide`로 해당 주체가 걸린 모든 바인딩을 확인해야 한다.

**④ 권한 상승 방지 (privilege escalation prevention)**

`role-manager` Role(roles, rolebindings 의 create/get/list)만 가진 SA `rbac-admin`이 다른 Role을 만들려고 했다.

```
$ k --as=system:serviceaccount:t10:rbac-admin -n t10 create role want-pods --verb=get --resource=pods
Error from server (Forbidden): roles.rbac.authorization.k8s.io "want-pods" is forbidden: user "system:serviceaccount:t10:rbac-admin"
  (groups=[...]) is attempting to grant RBAC permissions not currently held:
{APIGroups:[""], Resources:["pods"], Verbs:["get"]}

$ ... create role want-rbac --verb=get --resource=roles.rbac.authorization.k8s.io
role.rbac.authorization.k8s.io/want-rbac created                     # 자신이 가진 권한(roles get)만 담은 Role은 가능

$ ... create rolebinding take-admin --clusterrole=admin --serviceaccount=t10:rbac-admin
Error from server (Forbidden): ... is attempting to grant RBAC permissions not currently held:
{APIGroups:[""], Resources:["bindings"], Verbs:["get" "list" "watch"]}
{APIGroups:[""], Resources:["configmaps"], ...}
...                                                                    # admin이 가진, 내가 갖지 못한 권한 목록
```

- **Role/RoleBinding을 만들 권한이 있어도, 자신이 가지지 않은 권한은 남에게 줄 수 없다.** 스스로 `admin`을 연결해서 권한을 올리는 길도 막힌다. 거부 메시지는 부족한 권한 목록을 모두 보여 준다.
- 이 제한을 풀려면 특수 동사가 필요하다.

```
# role-manager에 escalate(roles), bind(clusterroles) 동사를 추가한 뒤
$ ... create role want-pods --verb=get --resource=pods                 →  role.../want-pods created
$ ... create rolebinding take-view --clusterrole=view --serviceaccount=t10:rbac-admin   →  rolebinding.../take-view created
```

| 동사 | 의미 |
|---|---|
| `escalate` | 자신이 가지지 않은 권한을 담은 Role/ClusterRole을 만들거나 수정 |
| `bind` | 자신이 가지지 않은 권한이라도 해당 Role/ClusterRole을 바인딩 |

  `escalate`와 `bind`는 사실상 **권한을 올릴 수 있는 열쇠**이므로 일반 사용자/SA에 주면 안 된다.

##### 4-4. 권한 점검 도구

| 명령 | 용도 |
|---|---|
| `k auth can-i <verb> <resource> [-n ns] [--as=...]` | 특정 동작이 가능한지 |
| `k auth can-i --list [-n ns] [--as=...]` | 주체가 가진 권한 목록 |
| `k auth whoami` | 현재 인증된 신원 |
| 거부 메시지 | `User "X" cannot <verb> resource "<res>" in API group "<g>" in the namespace "<ns>"` |

- `--as`, `--as-group`으로 다른 신원을 흉내 낼 수 있다 (실험에서 `--as-group=system:serviceaccounts:...`로 그룹도 지정했다. 실제 SA에는 그룹이 자동으로 붙지만 가장할 때는 직접 지정해야 한다).
- 서버에 없는 리소스에 대한 `can-i`는 `no`가 나올 수 있다 (3-4절).

##### 4-5. 최소 권한 원칙 정리

| 권고 | 근거 |
|---|---|
| 앱마다 **전용 ServiceAccount** | `default`를 공유하면 권한도 공유 |
| API를 안 쓰는 Pod는 `automountServiceAccountToken: false` | 토큰 노출 경로 제거 (2-2절) |
| 가능한 한 **Role + RoleBinding**(네임스페이스 범위) | ClusterRoleBinding은 모든 네임스페이스에 적용 |
| `resourceNames`, 특정 동사로 좁게 | 3-2절 표 |
| `secrets` 읽기 권한 주의 | `view`도 못 읽는 이유 (3-4절) |
| `escalate`, `bind`, `impersonate`, `*` 와일드카드 지양 | 권한 상승 열쇠 |
| 토큰은 만료되는 것을 쓴다 | 영구 토큰 Secret은 유출 시 취소 수단이 Secret 삭제뿐 |

#### 5. 확인하지 못한 부분

- **인증 방식의 다른 종류**: OIDC, 웹훅 토큰 인증, 정적 토큰 파일, 인증서 서명 요청(CSR)으로 사용자 인증서 발급은 실험하지 않았다. User/Group이 인증서의 CN/O에서 오는 것은 문서 지식이다 (이 환경 사용자는 `docker-for-desktop`/`system:masters`임을 `whoami`로만 확인).
- **어드미션 플러그인 전체 목록과 순서**: `NodeRestriction`만 확인했고 기본 활성 플러그인(ServiceAccount, DefaultTolerationSeconds 등)이 1-6절의 변경을 만든다고 추정했다. 각 변경이 어느 플러그인에서 오는지는 확인하지 않았다. 변형 어드미션 → 스키마 검증 → 검증 어드미션의 정확한 순서도 확인하지 않았다.
- **Node 인가자**(`--authorization-mode=Node,RBAC`의 `Node`): kubelet이 자기 노드 Pod의 Secret만 읽는 동작은 실험하지 않았다.
- **감사 로그(audit)**: 요청 처리 이력을 확인하지 않았다.
- **API Priority and Fairness**: 응답 헤더의 존재만 확인했다.
- **Pod 안 토큰 31536000초(1년)의 이유**와 kubelet이 토큰을 갱신(rotation)하는 시점은 확인하지 않았다.
- **토큰 인증 캐시 TTL(약 11초)**: 두 번 측정한 값이고 설정으로 확인하지 않았다. 한 번도 쓰지 않은 토큰은 SA 삭제 직후 401이었던 것도 1회 관찰이다.
- **`kubectl --token`이 인증서에 밀리는 규칙**: 이 환경에서 관찰한 사실이고 kubectl의 인증 우선순위 규칙 자체는 확인하지 않았다.
- **`kubectl auth reconcile`, `SubjectAccessReview` API 직접 호출, ClusterRole의 `nonResourceURLs` 직접 작성, `impersonate` 권한 제한**은 실험하지 않았다.
- **RBAC 오브젝트가 etcd에 저장되는 방식**과 권한 변경이 API 서버에 반영되는 지연: `can-i`는 바인딩 생성 직후 바로 반영되었으나 지연 자체를 측정하지 않았다.
- **ClusterRole 집계 컨트롤러가 규칙을 합치는 시간**: 4초 안에 반영된 것만 확인했다.
