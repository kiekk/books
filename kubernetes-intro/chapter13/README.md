### 13장. 인증과 권한 관리

> 정리 범위: Role/ClusterRole, RoleBinding/ClusterRoleBinding 개념, Role에 적용할 수 있는 주요 값(rules의 필드)
>
> 실측 환경: macOS (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2, kubectl v1.35.2. 모든 명령에 `--context docker-desktop`을 명시했다(`K`는 `kubectl --context docker-desktop`, `k`는 거기에 `-n t13`을 붙인 것). 권한을 시험할 대상은 네임스페이스 `t13`의 ServiceAccount `dev`, `ops`와 `u-view`/`u-edit`/`u-admin`이고, 실험이 끝난 뒤 네임스페이스와 직접 만든 ClusterRole/ClusterRoleBinding을 삭제했다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> **주의(실험 방법)**: 이 PC의 kubeconfig 사용자는 **클라이언트 인증서(관리자)**로 접속한다. 이때 `kubectl --token=<SA 토큰>`을 줘도 인증서가 먼저 쓰여 **관리자로 동작**했다(처음 시도에서 `get deploy` 같은 막혀야 할 요청이 통과해서 알았다). 그래서 이 문서의 "실제 호출" 결과는 `--kubeconfig=/dev/null --server=… --certificate-authority=… --token=…`으로 **토큰만으로** 호출한 것이고, `auth can-i`는 `--as=system:serviceaccount:t13:dev` 가장(impersonation)으로 확인했다.

#### 목차

1. 인증과 인가 한눈에 보기
2. Role, ClusterRole
3. RoleBinding, ClusterRoleBinding
4. 조합 규칙: ClusterRole + RoleBinding
5. Role에 적용할 수 있는 주요 값 (rules 필드)
6. 기본 제공 ClusterRole과 집계(aggregation)
7. 권한 상승 방지
8. 확인하는 방법
9. 선택 가이드와 함정
10. 확인하지 못한 부분

---

#### 1. 인증과 인가 한눈에 보기

API 서버가 요청을 처리하는 순서는 **인증(Authentication, 누구인가) → 인가(Authorization, 해도 되는가) → 어드미션 컨트롤(Admission, 내용 검사/변형)**이다. 이 장의 Role/RoleBinding은 **인가(RBAC)** 단계다.

##### 1-1. 인증된 신원 확인 (실측)

```
$ kubectl --token=<dev의 토큰> ... auth whoami
Username  system:serviceaccount:t13:dev
Groups    [system:serviceaccounts system:serviceaccounts:t13 system:authenticated]
Extra     authentication.kubernetes.io/credential-id [JTI=…]
```

- ServiceAccount는 사용자 이름이 `system:serviceaccount:<네임스페이스>:<이름>`이고 그룹 `system:serviceaccounts`, `system:serviceaccounts:<네임스페이스>`, `system:authenticated`에 자동으로 속한다.
- 토큰은 `kubectl create token dev`로 만들었다(단기 토큰, 약 909자). 사람 사용자(User)와 그룹은 쿠버네티스에 오브젝트로 존재하지 않고 인증서/OIDC 등 외부에서 온다(이 실험에서는 인증서 방식 외 확인하지 않음).

##### 1-2. RBAC의 기본 원칙

- **허용(allow)만 있고 거부(deny)는 없다.** 규칙을 합친 합집합이 권한이다.
- 아무 규칙도 없으면 거부다:

```
$ K auth can-i get pods -n t13 --as=system:serviceaccount:t13:dev
no
```

---

#### 2. Role, ClusterRole

| 구분 | Role | ClusterRole |
|---|---|---|
| 범위 | **네임스페이스** 안의 리소스 | **클러스터 전체** (네임스페이스 없는 리소스, 비리소스 URL, 모든 네임스페이스) |
| 네임스페이스 | 속함 (`metadata.namespace`) | 없음 |
| 내용 | `rules`의 목록 | `rules`의 목록 (+ `aggregationRule`) |
| 대표 용도 | 앱/팀별 권한 | 노드, PV, 네임스페이스 같은 클러스터 리소스, 여러 네임스페이스 공통 권한 |

##### 2-1. Role로는 클러스터 범위 리소스를 줄 수 없다 (실측)

`nodes`(클러스터 범위)에 `get, list`를 주는 **Role** `bad-node`를 만들어 `dev`에 바인딩했다. 만들어지긴 했지만:

```
$ K auth can-i get nodes --as=...:dev
Warning: resource 'nodes' is not namespace scoped
no
```

- Role/RoleBinding은 네임스페이스 리소스에만 의미가 있다. 노드 같은 클러스터 리소스는 **ClusterRole + ClusterRoleBinding**이어야 한다.

---

#### 3. RoleBinding, ClusterRoleBinding

**Role/ClusterRole(무엇을 할 수 있나)을 subject(누가)에 연결**한다. subject는 `User`, `Group`, `ServiceAccount` 세 종류다.

| 구분 | RoleBinding | ClusterRoleBinding |
|---|---|---|
| 범위 | **한 네임스페이스** 안에서 권한 부여 | **클러스터 전체**에서 권한 부여 |
| 참조 가능한 `roleRef` | Role(같은 네임스페이스) 또는 ClusterRole | ClusterRole만 |

##### 3-1. Role + RoleBinding (실측)

`pod-reader`(pods에 `get`, `list`) Role을 `dev`에 바인딩한 뒤, 토큰만으로 호출했다.

| 호출 (dev 토큰) | 결과 |
|---|---|
| `get pods -n t13` | 성공 |
| `get pods -n t13b` | **Forbidden** (`cannot list resource "pods" … namespace "t13b"`) |
| `get pods -A` | **Forbidden** (cluster scope) |
| `get deploy -n t13` (Role에 deployments 없음, 이때는 별도 Role `multi`로 허용돼 성공) | 아래 5절 참고 |
| `get secret -n t13` | **Forbidden** |
| `get nodes` | **Forbidden** (cluster scope) |

- `can-i` 결과: `get`/`list` 허용, `watch`/`delete`는 `no`. `kubectl get pods`는 목록 조회라서 `list`가 필요하다. `watch`가 필요한 도구(`-w`, 컨트롤러, 대시보드)는 `watch`도 줘야 한다.

##### 3-2. ClusterRole + ClusterRoleBinding (실측)

`node-reader`(nodes get/list/watch)와 `pod-all`(pods get/list)을 `ops`에 ClusterRoleBinding으로 연결했다.

```
$ (ops 토큰) get nodes                  → 성공
$ (ops 토큰) get pods -A                → 성공
$ (ops 토큰) get pods -n t13b           → 성공 (No resources found)
$ (ops 토큰) get pods -n kube-system    → 성공
$ (ops 토큰) get secret -n t13          → Forbidden (권한 없음)
```

모든 네임스페이스에서 pods를 읽을 수 있고, 클러스터 리소스인 nodes도 읽혔다. 줄 때 **범위가 넓다**는 점이 RoleBinding과의 차이다.

##### 3-3. subject로 그룹과 사용자

```
$ k create rolebinding grp --clusterrole=view --group=system:serviceaccounts:t13
→ ops 가 (자신의 바인딩과 별개로) 이 그룹을 통해 deployments list 허용: yes
$ k create rolebinding alice-rb --clusterrole=edit --user=alice
→ alice: create deployments (t13) yes / get pods (t13b) no
```

- 그룹에 바인딩하면 그 그룹의 모든 구성원(여기서는 `t13`의 모든 ServiceAccount)이 권한을 얻는다.
- `alice`는 쿠버네티스에 만든 적 없는 이름이지만 바인딩만으로 `--as=alice`가 권한을 가졌다. **User는 오브젝트가 아니라 문자열 이름**이기 때문이다(실제 로그인할 수 있는지는 인증 수단에 달려 있음).

##### 3-4. `roleRef`는 불변

```
$ k patch rolebinding dev-cmedit --type merge -p '{"roleRef":{"name":"pod-all", ...}}'
The RoleBinding "dev-cmedit" is invalid: roleRef: Invalid value: ...: cannot change roleRef
```

연결 대상 Role을 바꾸려면 **삭제 후 재생성**해야 한다(`subjects`는 수정 가능).

##### 3-5. 없는 Role을 가리키는 바인딩

```
$ k create rolebinding ghost --role=nosuch --serviceaccount=t13:dev
rolebinding.rbac.authorization.k8s.io/ghost created           ← 생성은 성공
$ K auth can-i get secrets -n t13 --as=...:dev
no - RBAC: role.rbac.authorization.k8s.io "nosuch" not found
```

- API 서버는 존재하지 않는 Role을 참조하는 바인딩도 받아준다. 나중에 같은 이름의 Role이 생기면 그때부터 효력이 생긴다(이 부분은 실험하지 않았다).
- `can-i`의 사유(reason)에 not found가 표시되어 단서가 된다.

---

#### 4. 조합 규칙: ClusterRole + RoleBinding

**ClusterRole을 RoleBinding으로 연결하면 그 네임스페이스 안에서만** 권한이 적용된다. 공통 권한 템플릿을 ClusterRole로 한 번 정의하고 네임스페이스별로 재사용하는 방식이다.

```
$ K create clusterrole cm-edit --verb='*' --resource=configmaps
$ k create rolebinding dev-cmedit --clusterrole=cm-edit --serviceaccount=t13:dev      # RoleBinding (t13)
$ K auth can-i list configmaps -n t13  --as=...:dev   → yes
$ K auth can-i list configmaps -n t13b --as=...:dev   → no
```

| 조합 | 효력 범위 |
|---|---|
| Role + RoleBinding | 그 네임스페이스 |
| ClusterRole + RoleBinding | **그 네임스페이스만** (RoleBinding이 속한 곳) |
| ClusterRole + ClusterRoleBinding | **클러스터 전체** |
| Role + ClusterRoleBinding | 불가 (ClusterRoleBinding은 ClusterRole만 참조) |

---

#### 5. Role에 적용할 수 있는 주요 값 (rules 필드)

```yaml
rules:
- apiGroups: [""]               # 어떤 API 그룹 ("" = core)
  resources: [pods, pods/log]   # 어떤 리소스 (서브리소스는 pods/log 형태)
  resourceNames: [app]          # (선택) 특정 이름의 오브젝트만
  verbs: [get, list]            # 어떤 동작
- nonResourceURLs: ["/healthz"] # (ClusterRole 전용) 리소스가 아닌 URL
  verbs: [get]
```

##### 5-1. verbs

| verb | HTTP 대응 | 비고 (실측) |
|---|---|---|
| `get` | GET (단건) | `get pods/p1` |
| `list` | GET (컬렉션) | `kubectl get pods`, **`list`만 줘도 모든 항목의 내용이 읽힌다**(11장 Secret 참고) |
| `watch` | GET ?watch | Role에 get,list만 있으면 `watch`는 `no` |
| `create` | POST | |
| `update` | PUT | `kubectl replace`, 이 실험에서 `replace`는 통과 |
| `patch` | PATCH | `kubectl patch`, `apply` 등 |
| `delete` | DELETE | |
| `deletecollection` | DELETE (컬렉션) | 이번 실험에서는 다루지 않음 |
| `*` | 전부 | |
| `escalate`, `bind`, `impersonate` | 특수 | 7절 |

**`update`와 `patch`는 별개다**: `configmaps/app`에 `update`만 허용한 Role로 `kubectl patch cm app`을 했더니 `cannot patch resource "configmaps"`로 거부됐고, 같은 신원으로 `kubectl replace -f`(update)는 **성공**했다. `can-i update configmap/app`이 `yes`여도 `patch` 방식의 도구는 막힌다.

##### 5-2. apiGroups와 resources

- core API 그룹은 빈 문자열 `""`(pods, configmaps, secrets, services …), 나머지는 `apps`(deployments), `batch`(jobs), `rbac.authorization.k8s.io` 등이다.
- 같은 이름을 쓰는 그룹이 달라서 `deployments`는 `apiGroups: ["apps"]`로 써야 한다. `kubectl auth can-i get deployments.apps`처럼 확인한다.
- `resources: ["*"]`(core 그룹): 실험에서 `get secrets`도 허용됐다(`wild` ClusterRole). **`*`에는 secrets가 포함된다.**

##### 5-3. 서브리소스 (`pods/log`, `pods/exec`, `deployments/scale`)

| Role 규칙 | 실측 |
|---|---|
| `pods/log`: get | `kubectl logs` 성공 |
| `pods/exec`: **get** 또는 **create** | **둘 중 하나만 줘도** `kubectl exec`가 성공 (get만 줬을 때도, create만 줬을 때도). kubectl 1.35의 exec가 WebSocket(GET)과 SPDY(POST) 중 되는 쪽을 쓰는 것으로 보이나 내부 동작은 확인하지 않았다 |
| `deployments/scale`: patch | `kubectl scale deploy` 성공 (`deployments` 본체 `patch`와는 별개 리소스) |

- 서브리소스는 `pods`와 별개로 권한을 줘야 한다. `pods`에 `get`이 있어도 `pods/exec`가 없으면 exec는 안 된다(이 실험의 Role에는 항상 exec 규칙을 함께 뒀으므로 "exec 규칙이 없을 때 거부"는 직접 확인하지 않았다).
- **`kubectl auth can-i` 사용 함정**: `can-i create pods/exec`처럼 `/`를 쓰면 `리소스/이름`으로 해석돼 **`pods`의 `exec`라는 이름의 객체**를 묻는 것이 된다(실험에서 이 형태는 규칙이 있는데도 `no`였다). 서브리소스는 **`--subresource=exec`** 옵션으로 확인해야 한다(`yes`).

##### 5-4. resourceNames

```
rules: [{apiGroups: [""], resources: [configmaps], resourceNames: [app], verbs: [get, update]}]
```

| 호출 | 결과 |
|---|---|
| `get cm app` | 성공 |
| `get configmap/other` | 불가 (`no`) |
| `list configmaps` / `get cm` (목록) | **Forbidden** |
| `create configmaps` with `resourceNames: [newcm]` + `verbs: [create]` | **`can-i`가 `no`, 실제 `create cm newcm`도 Forbidden** |

- `resourceNames`는 이름을 아는 `get`/`update`/`delete` 등에는 쓸 수 있지만 **`list`로는 목록 전체를 막아야 한다**(목록 조회는 이름을 지정하지 않으므로 거부됨).
- `create`에는 `resourceNames`로 제한할 수 없다: 생성 요청에는 대상 이름이 아직 없으므로, `resourceNames`를 단 `create` 규칙은 **어떤 생성도 허용하지 못했다**(위 실측).

##### 5-5. nonResourceURLs와 와일드카드

```
$ K auth can-i get /healthz  --as=...:ops   → yes   (ClusterRole wild: nonResourceURLs ["/healthz","/metrics*"])
$ K auth can-i get /metrics                  → yes   (접미사 * 패턴)
$ K auth can-i get /version / /livez         → yes   (기본 역할이 부여하는 공개 정보 URL일 수 있음, 원인 미확인)
```

- `nonResourceURLs`는 **ClusterRole에서만** 쓸 수 있고 `/healthz`, `/metrics` 같은 API 서버의 리소스가 아닌 경로를 다룬다. 끝에 `*`를 붙여 접두사 패턴으로 쓴다.
- 리소스, 동사, apiGroups 모두 `*`를 쓸 수 있다. cluster-admin이 그 예다:

```
$ K get clusterrole cluster-admin -o jsonpath='{.rules}'
[{"apiGroups":["*"],"resources":["*"],"verbs":["*"]},{"nonResourceURLs":["*"],"verbs":["*"]}]
```

---

#### 6. 기본 제공 ClusterRole과 집계(aggregation)

##### 6-1. 사용자용 기본 ClusterRole (RoleBinding으로 `t13`에 바인딩해 확인)

| 요청 (t13) | view | edit | admin |
|---|---|---|---|
| `get secrets` | **no** | yes | yes |
| `list pods` | yes | yes | yes |
| `create pods` | no | yes | yes |
| `create deployments.apps` | no | yes | yes |
| `create roles`, `create rolebindings` | no | **no** | **yes** |
| `get resourcequotas` | yes | yes | yes |
| `create resourcequotas` | no | no | no |
| `get namespaces` | yes | yes | yes |
| `delete namespaces` | no | no | no |
| `get nodes` | no | no | no |
| `create serviceaccounts/token` | no | yes | yes |

- **view**: 읽기만, 단 **Secret은 못 읽는다**(Secret은 자격 증명 탈취 경로가 되므로).
- **edit**: 대부분 리소스 읽기/쓰기, Secret 읽기 가능, **Role/RoleBinding은 못 만든다**.
- **admin**: 네임스페이스 안 거의 전부 + Role/RoleBinding 관리(네임스페이스 안 권한 위임 가능). ResourceQuota 변경이나 네임스페이스 삭제는 못 한다.
- **cluster-admin**: 모든 것(위 5-5).
- 네 가지 모두 ClusterRole이므로 ClusterRoleBinding으로 주면 전 네임스페이스에, RoleBinding으로 주면 한 네임스페이스에 적용된다(4절).

##### 6-2. 집계(aggregationRule)

`view`, `edit`, `admin`은 `aggregationRule`로 **레이블이 붙은 다른 ClusterRole의 규칙을 자동으로 합친다**.

```
$ K get clusterrole view -o jsonpath='{.aggregationRule}'
{"clusterRoleSelectors":[{"matchLabels":{"rbac.authorization.k8s.io/aggregate-to-view":"true"}}]}
```

- 레이블 `rbac.authorization.k8s.io/aggregate-to-view: "true"`를 단 ClusterRole `agg-viewers-of-widgets`(events, `example.com`의 widgets)를 만들자 몇 초 뒤 **`view`의 규칙에 widgets가 추가**됐다(포함 개수 1 확인). CRD를 설치할 때 기본 역할에 권한을 합치는 표준 방법이다.
- 직접 집계 ClusterRole `monitoring`(`team=monitoring` 레이블 선택)을 만들면 `mon-pods`, `mon-nodes`의 규칙이 합쳐졌고(`pods get/list`, `nodes get/list`), `monitoring`의 `rules`를 **직접 수정해도(secrets get) 곧바로 집계 결과로 덮어써졌다**.

---

#### 7. 권한 상승 방지

RBAC은 **자기가 가진 권한보다 큰 권한을 남에게 줄 수 없게** 막는다. `dev`에게 Role/RoleBinding 생성 권한만 주고 시험했다.

```
(dev는 pods get/list 만 가진 상태에서 roles/rolebindings create 권한 보유)
$ create role grant-secrets --verb=get --resource=secrets
Forbidden: user "system:serviceaccount:t13:dev" ... is attempting to grant RBAC permissions not currently held:
  {APIGroups:[""], Resources:["secrets"], Verbs:["get"]}
$ create role grant-pods --verb=get --resource=pods     → created   (자기가 가진 권한이므로)
$ create rolebinding self --role=pod-reader ...         → created   (자기가 가진 권한 범위)
```

- 자신이 이미 가진 권한 범위 안에서만 Role을 만들고 바인딩할 수 있다.
- 예외 동사: Role에 **`escalate`**(권한 상승 허용) 동사가 있으면 가진 것보다 큰 Role을 만들 수 있고(실험: `escalate`를 추가하자 `grant-secrets`가 created), **`bind`**는 가진 것보다 큰 Role을 바인딩할 수 있게 한다(이 실험에서는 `bind` 단독 효과를 따로 확인하지 않았다). 그래서 `roles`/`rolebindings` 쓰기 권한 자체가 사실상 위험한 권한이다.

---

#### 8. 확인하는 방법

| 목적 | 명령 |
|---|---|
| 내가 할 수 있나 | `kubectl auth can-i get pods -n ns` |
| 다른 주체로 확인 | `kubectl auth can-i … --as=system:serviceaccount:ns:sa` (그룹은 `--as-group`) |
| 서브리소스 | `kubectl auth can-i create pods --subresource=exec` |
| 가진 권한 전체 | `kubectl auth can-i --list -n ns --as=…` |
| 누구로 인식되나 | `kubectl auth whoami` |
| 바인딩 보기 | `kubectl get rolebinding -o wide` (ROLE, USERS, GROUPS, SERVICEACCOUNTS 열) |

`--list` 예(`ops`): `bindings`, `configmaps`, `endpoints`, `events` … `[get list watch]`처럼 리소스별 허용 verb와 `[/healthz]` 같은 비리소스 URL, 그리고 모든 인증된 주체에 기본 부여되는 `selfsubjectreviews` 등이 함께 나온다.

---

#### 9. 선택 가이드와 함정

| 하고 싶은 것 | 방법 |
|---|---|
| 한 네임스페이스 앱에 최소 권한 | Role + RoleBinding |
| 여러 네임스페이스에 같은 권한 | ClusterRole 하나 + 네임스페이스별 RoleBinding |
| 노드, PV, 네임스페이스 같은 클러스터 리소스 | ClusterRole + ClusterRoleBinding |
| 개발자에게 읽기 전용 | `view` ClusterRole(Secret 제외) |
| 팀 관리자(네임스페이스 안 위임) | `admin` + RoleBinding |
| CRD 권한을 기본 역할에 합침 | 집계 레이블 `aggregate-to-view/edit/admin` |
| 특정 ConfigMap만 | `resourceNames` (list, create에는 못 씀) |
| 특정 사람이 아닌 팀 단위 | Group을 subject로 |

함정:

- `list`/`watch`만 줘도 **내용이 노출**된다(Secret 포함). Secret에는 `get`뿐 아니라 `list`도 신중하게.
- `resources: ["*"]`는 secrets를 포함한다. 와일드카드는 최소한으로.
- `update`와 `patch`는 별개 동사다. 도구가 어느 쪽을 쓰는지에 따라 거부될 수 있다.
- `kubectl auth can-i`로 서브리소스를 확인할 때는 `--subresource`.
- `--token`만 줘도 kubeconfig의 **클라이언트 인증서가 우선**일 수 있어 권한 시험이 무의미해질 수 있다(관리자로 통과). 시험할 때는 `--as`나 토큰 전용 kubeconfig를 쓴다.
- 없는 Role을 가리키는 바인딩도 만들어진다. 오타가 조용히 남는다(`can-i`의 reason으로 확인).
- `roleRef`는 못 바꾼다.
- `roles`/`rolebindings`의 `create`, `escalate`, `bind` 권한은 권한 상승 경로다.

---

#### 10. 확인하지 못한 부분

- **인증(Authentication) 수단 전반**: 클라이언트 인증서 발급(CSR), OIDC, 웹훅 토큰, EKS의 `aws-auth`/Access Entry와 IAM 연동은 실험하지 않았다. 사람 User의 실제 생성/로그인 흐름도 확인하지 못했다.
- **ServiceAccount 토큰 상세**: Pod에 자동 주입되는 projected 토큰, 만료/갱신, `automountServiceAccountToken`은 확인하지 않았다. (11장에서도 일부만 다룸)
- **어드미션 컨트롤러**(PodSecurity, ValidatingAdmissionPolicy 등)는 다루지 않았다.
- **`bind`, `impersonate`, `deletecollection` 동사**의 실제 동작, `escalate` 없이 `bind`만 있을 때의 결과는 확인하지 않았다.
- 존재하지 않던 Role을 **나중에 같은 이름으로 만들면** 기존 바인딩이 살아나는지 확인하지 않았다.
- `exec`에서 `pods/exec` 규칙이 **전혀 없을 때** 거부되는지는 실험의 모든 Role이 exec 규칙을 가지고 있어 직접 확인하지 못했다.
- `/version`, `/livez`가 허용된 이유(기본 ClusterRole `system:public-info-viewer` 등으로 추정)는 확인하지 않았다.
- `ClusterRole`의 `aggregationRule`로 합쳐진 결과의 갱신 지연 시간(몇 초 안)은 정확히 재지 않았다.
- **RBAC 감사/로그**(감사 로그로 누가 무엇을 했는지), `kubectl-who-can` 같은 도구, **EKS에서의 RBAC 운영**은 확인하지 못했다.
- **Role의 `rules`에서 apiGroups/resources 조합의 전체 목록**(`kubectl api-resources` 기반)은 정리하지 않았다.
