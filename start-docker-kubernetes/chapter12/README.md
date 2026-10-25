### 12장. 커스텀 리소스와 컨트롤러

> 정리 범위: 명령형(imperative)과 선언적(declarative) 방식의 차이, 커스텀 리소스(CRD)
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드), kubectl v1.35.2
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`을 명시했다 (아래에서는 `k`로 줄여 적음). 실험은 `t12` 네임스페이스와 `*.t12.example.com` CRD만 썼고 끝나고 모두 지웠다.

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. 명령형 vs 선언적
2. 커스텀 리소스(CRD)
3. 확인하지 못한 부분

---

#### 1. 명령형 vs 선언적

##### 1-1. 개념

| 방식 | 사용자가 말하는 것 | 예 |
|---|---|---|
| **명령형 커맨드** | "이 동작을 해라" | `kubectl create deployment`, `scale`, `set image`, `label`, `expose`, `run` |
| **명령형 오브젝트 설정** | "이 파일의 오브젝트를 **만들어라/교체해라/지워라**" | `kubectl create -f`, `replace -f`, `delete -f` |
| **선언적 오브젝트 설정** | "이 파일의 상태가 **되도록 해라**" | `kubectl apply -f` |

- 쿠버네티스 시스템 자체는 모두 선언적이다 (6장: 원하는 상태 `spec`을 저장하면 컨트롤러가 맞춘다). 여기서 말하는 차이는 **사람이 kubectl로 어떻게 요청하느냐**다.
- 명령형은 **빠르고 간단**하지만 이력이 남지 않는다. 선언적은 **YAML 파일이 곧 상태 정의**라서 Git으로 관리하고 재현할 수 있다 (GitOps, 7장 Kustomize의 전제).

##### 1-2. 명령형 커맨드 (실측)

```
$ k -n t12 create deployment imp --image=nginx:alpine --replicas=1     →  deployment.apps/imp created
$ k -n t12 scale deploy imp --replicas=3                               →  deployment.apps/imp scaled
$ k -n t12 set image deploy/imp nginx=nginx:1.27-alpine                →  deployment.apps/imp image updated
$ k -n t12 label deploy imp tier=back                                  →  deployment.apps/imp labeled
$ k -n t12 expose deploy imp --port=80                                 →  service/imp exposed
```

- 동사 하나가 하나의 변경이다. 결과 상태를 보려면 `get`으로 다시 확인해야 한다.
- 클러스터의 현재 상태만 있고 **"원래 의도한 상태"를 기록한 파일이 없다.** 같은 명령을 다른 클러스터에서 재현하려면 명령 목록을 따로 관리해야 한다.

##### 1-3. 명령형 오브젝트 설정: create / replace

```
$ k create -f web.yaml                          →  deployment.apps/web created
$ k create -f web.yaml                          →  Error from server (AlreadyExists): ...
$ k -n t12 get deploy web -o jsonpath='{.metadata.annotations}'
{"deployment.kubernetes.io/revision":"1"}        ← last-applied 어노테이션이 없다
```

- `create -f`는 **이미 있으면 실패**한다 (`AlreadyExists`). 처음 만드는 일회성 작업이다.
- `create -f`로 만든 오브젝트에는 `kubectl.kubernetes.io/last-applied-configuration` 어노테이션이 없다.

**`replace -f`는 오브젝트를 통째로 교체한다.** 라벨 `owner=ops`와 어노테이션 `note=hello`가 붙은 Deployment를 라벨 `app=web`만 있는 파일로 `replace` 했다.

```
$ k replace -f replace.json                     →  deployment.apps/web replaced
$ k -n t12 get deploy web -o jsonpath='labels={.metadata.labels} note={.metadata.annotations.note} last-applied={...}'
labels={"app":"web"} note= last-applied-present=
```

- 파일에 없는 라벨(`owner`), 어노테이션(`note`), `last-applied`가 **모두 사라졌다.** `replace`는 파일이 곧 전체 상태다. 다른 사람이나 컨트롤러가 붙인 필드도 지워지므로 위험하다. 객체가 있어야 하고(없으면 실패) 파일에 `resourceVersion`이 있으면 충돌 검사도 한다 (여기서는 파일에서 제외하고 시도해서 검사 동작은 확인하지 않았다).

##### 1-4. 선언적: apply와 3-way merge

`apply`는 **이전에 적용한 설정(last-applied), 이번 설정(YAML), 현재 라이브 상태** 세 가지를 비교해서 패치를 만든다 (10장에서 본 `GET` 후 `PATCH`).

```
$ k apply -f web.yaml (create로 만든 오브젝트에)
Warning: resource deployments/web is missing the kubectl.kubernetes.io/last-applied-configuration annotation which is required by kubectl apply ...
deployment.apps/web configured
$ k apply -f web.yaml                           →  deployment.apps/web unchanged        ← 변경 없으면 아무것도 안 함
```

- `create`로 만든 것에 `apply`를 하면 경고가 뜨고 어노테이션이 자동으로 채워진다.
- 같은 파일을 다시 `apply` 하면 `unchanged`다 (**멱등**). 몇 번을 실행해도 결과가 같다.

**YAML에 쓴 필드는 YAML 값으로 되돌려진다**

```
$ (web.yaml: replicas: 2)    k apply -f web.yaml
$ k -n t12 scale deploy web --replicas=5        # 명령형으로 변경
live replicas=5
$ k apply -f web.yaml                           →  deployment.apps/web configured
after apply replicas=2                          ← YAML 값으로 되돌아옴
```

**YAML에서 필드를 빼면 라이브에서도 삭제된다**

```
$ (replicas가 없는 web-norep.yaml)  k -n t12 scale deploy web --replicas=4 ; k apply -f web-norep.yaml
replicas=1                                       ← 4가 유지되지 않고 기본값 1이 되었다
```

- 이전에 `replicas: 2`를 `apply` 한 적이 있어서(last-applied에 있음) **YAML에서 그 줄을 지우면 "삭제하겠다"는 뜻**이 되어 필드가 지워졌고 기본값(1)이 되었다. HPA가 `replicas`를 조절하는 Deployment에서 YAML의 `replicas`를 지우고 `apply` 하는 순간 **기본값 1로 줄어드는** 사고가 이 동작에서 나온다. HPA를 쓸 때는 처음부터 `replicas`를 YAML에 두지 않아야 한다.

**YAML에 한 번도 쓴 적 없는 필드는 건드리지 않는다**

```
$ k -n t12 label deploy web owner=ops ; k -n t12 annotate deploy web note=hello       # 명령형으로 추가
$ k apply -f web-v2.yaml        # labels에 env=dev 추가
labels={"app":"web","env":"dev","owner":"ops"}                                            ← owner 유지
$ k apply -f web.yaml           # env 줄 제거
after removing env from yaml: labels={"app":"web","owner":"ops"} note=hello               ← env는 삭제, owner/note는 유지
```

| 필드의 출처 | `apply` 시 |
|---|---|
| YAML에 있음 | YAML 값으로 맞춘다 (라이브에서 바뀌었어도) |
| 이전 `apply`에는 있었고 이번 YAML에서 빠짐 | **삭제** |
| YAML에 한 번도 없음 (다른 도구/컨트롤러/사람이 설정) | **그대로 둔다** |

- 이것이 `replace`와의 차이다. `apply`는 "내가 선언한 필드"만 관리하고 나머지는 보존한다.

##### 1-5. patch와 edit

```
# strategic merge patch (기본): 리스트를 이름 같은 키로 병합
$ k -n t12 patch deploy web -p '{"spec":{"template":{"spec":{"containers":[{"name":"nginx","image":"nginx:1.27-alpine"}]}}}}'
strategic: containers=nginx image=nginx:1.27-alpine requests={"requests":{"cpu":"10m"}}      ← 다른 필드(resources) 유지
# JSON merge patch (--type=merge): 리스트는 통째로 교체
$ k -n t12 patch deploy web --type=merge -p '{"spec":{"template":{"spec":{"containers":[{"name":"sidecar","image":"busybox"}]}}}}'
merge:     containers=sidecar                                                              ← nginx 컨테이너가 사라짐
# JSON patch (--type=json): 경로 지정
$ k -n t12 patch deploy web --type=json -p '[{"op":"replace","path":"/spec/replicas","value":3}]'
json:      replicas=3
```

| 종류 | 리스트 처리 | 용도 |
|---|---|---|
| strategic merge (기본) | 키(`name`)로 병합 | 쿠버네티스 내장 리소스 |
| JSON merge (`--type=merge`) | **통째로 교체** | 단순한 필드, **CRD** (strategic은 CRD에서 지원되지 않는다는 것은 문서 지식, 이번에는 CRD에 `--type=merge`만 사용) |
| JSON patch (`--type=json`) | 경로/연산 지정 | 정확한 한 군데 수정 |

- `kubectl patch`, `edit`, `scale`, `label`, `set image`도 모두 **명령형 변경**이다. 이 변경을 YAML에 되돌려 반영하지 않으면 다음 `apply`가 YAML 값으로 덮어쓰거나(1-4), 적어 둔 파일과 라이브가 어긋난다.

##### 1-6. 필드 소유권: managedFields

어떤 도구가 어떤 필드를 바꿨는지는 오브젝트의 `managedFields`에 기록된다 (`kubectl get`은 기본으로 숨기고 `--show-managed-fields`로 보인다).

```
$ k apply -f web.yaml ; k scale ... --replicas=4 ; k label ... owner=ops ; k set image ...      # 서로 다른 도구로 변경
$ k -n t12 get deploy web --show-managed-fields -o json   (요약)
manager=kubectl-client-side-apply  op=Update  .metadata.annotations.last-applied .metadata.labels.app .spec.selector ...
manager=kubectl                    op=Update  sub=scale  .spec.replicas
manager=kubectl-label              op=Update  .metadata.labels.owner
manager=kubectl-set                op=Update  (이미지)
manager=kube-controller-manager    op=Update  sub=status  .status.replicas, deployment.kubernetes.io/revision
```

- 각 명령이 자기 이름(`kubectl-label`, `kubectl-set`, `kubectl-client-side-apply`)으로 **필드의 주인(manager)** 이 된다. `scale`은 `scale` 서브리소스를, 컨트롤러는 `status`를 쓴다.

**서버 측 적용 (Server-Side Apply, `--server-side`)**

클라이언트 측 `apply`는 kubectl이 3-way merge를 계산하고 어노테이션에 이전 설정을 저장한다. 서버 측 적용은 **API 서버가 `managedFields`로 필드 소유권을 직접 관리**한다.

```
$ k apply --server-side -f web.yaml             →  deployment.apps/web serverside-applied
manager=kubectl  op=Apply  .metadata.labels.app .spec.selector ...        ← last-applied 어노테이션이 없다
$ k -n t12 scale deploy web --replicas=4        # 다른 매니저가 spec.replicas를 변경
$ k apply --server-side -f web.yaml             # replicas: 2 가 YAML에 있음
error: Apply failed with 1 conflict: conflict with "kubectl" with subresource "scale" using apps/v1: .spec.replicas
Please review the fields above--they currently have other managers. Here are the ways you can resolve this warning:
* If you intend to manage all of these fields, please re-run the apply command with the `--force-conflicts` flag.
* If you do not intend to manage all of the fields, please edit your manifest to remove references to the fields that should keep their current managers.
$ k apply --server-side --force-conflicts -f web.yaml     →  replicas=2   ← 강제로 소유권을 가져옴
```

- **다른 도구가 소유한 필드를 YAML이 덮어쓰려 하면 충돌**로 알려 주고 멈춘다. 클라이언트 측 `apply`는 말없이 덮어썼다 (1-4). `--force-conflicts`로 소유권을 가져올 수 있다.
- 필드를 YAML에서 빼면 그 필드의 소유권만 포기하고 **값은 유지**한다.

```
$ k apply --server-side --force-conflicts -f web-norep.yaml      # replicas 없는 YAML
$ k -n t12 scale deploy web --replicas=4 ; k apply --server-side -f web-norep.yaml
deployment.apps/web serverside-applied ; replicas=4                 ← 값 유지 (HPA와 같이 쓰기 좋다)
```

  클라이언트 측 apply(1-4)에서는 같은 상황에서 `replicas`가 기본값 1로 떨어졌다. 서버 측 적용은 YAML이 관리하지 않는 필드의 값을 건드리지 않는다. 이 비교는 같은 시나리오를 방식만 바꿔서 확인한 결과다.

##### 1-7. 선언적이라고 해서 자동 복구되는 것은 아니다

- `apply`는 **실행하는 순간**에만 YAML과 라이브를 맞춘다. YAML은 감시되지 않는다. 위 실험에서 `scale`로 바꾼 `replicas=5`는 다시 `apply` 하기 전까지 그대로 유지되었다.
- 컨트롤러가 자동으로 맞추는 것은 `spec`(원하는 상태)과 실제 상태(Pod 수 등)의 차이다. **YAML 파일과 `spec`의 차이는 아무도 맞춰 주지 않는다.** 이를 자동화하는 것이 Argo CD, Flux 같은 GitOps 도구다 (이 장에서 실험하지 않았다).
- `kubectl diff -f`로 `apply` 전에 변경 내용을 비교할 수 있다 (7장에서 확인).

##### 1-8. 정리

| 구분 | 명령형 커맨드 | 명령형 오브젝트 설정 | 선언적 (apply) |
|---|---|---|---|
| 대표 명령 | `create/scale/set/label` | `create -f`, `replace -f`, `delete -f` | `apply -f` |
| 입력 | 명령 옵션 | 파일 | 파일 (디렉터리 가능) |
| 이미 있을 때 | 동작에 따라 다름 | create는 실패, replace는 통째 교체 | 차이만 패치 (멱등) |
| 다른 도구가 바꾼 필드 | 건드리지 않음 | replace는 **지움** | **보존** (YAML에 없는 필드) |
| 이력/재현 | 없음 | 파일 | **Git으로 관리** |
| 용도 | 학습, 급한 디버깅, 일회성 | 단순한 일회성 생성 | **운영 배포의 기본** |

- 한 오브젝트에 방식을 섞으면 어긋난다. `create`로 만든 것에 `apply`하면 경고가 뜨고(1-4), `apply`로 만든 것을 `scale`/`edit`으로 바꾸면 다음 `apply`가 덮어쓴다. 한 오브젝트는 **한 방식으로** 관리하는 것이 원칙이다.

#### 2. 커스텀 리소스(CRD)

##### 2-1. 개념

**쿠버네티스 API에 새로운 리소스 종류를 추가하는 기능.** `Pod`, `Deployment`처럼 내장 리소스만 있던 API에 `Backup`, `Certificate`, `Database` 같은 **사용자 정의 종류**를 `kubectl`로 다룰 수 있게 한다.

| 용어 | 의미 |
|---|---|
| **CRD** (CustomResourceDefinition) | 새 리소스 종류의 **정의** (이름, 스키마, 범위, 버전). 그 자체가 쿠버네티스 오브젝트 |
| **CR** (Custom Resource) | CRD로 정의된 종류의 **인스턴스** (`kind: Backup`) |
| **커스텀 컨트롤러 / 오퍼레이터** | CR을 감시(watch)하며 원하는 상태에 맞춰 실제 작업을 하는 프로그램 (2-9절) |

- CRD를 만드는 순간 API 서버가 **새 REST 경로를 즉시 제공**한다. API 서버를 다시 빌드하거나 재시작하지 않는다.
- **CRD만으로는 아무 일도 일어나지 않는다.** 데이터를 저장하고 검증해 주는 API일 뿐이고, 그 데이터로 무언가를 하는 것은 컨트롤러의 일이다.

##### 2-2. CRD 정의

`Backup` 종류를 정의했다.

```yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: backups.t12.example.com          # 반드시 <plural>.<group>
spec:
  group: t12.example.com
  scope: Namespaced                      # Namespaced 또는 Cluster
  names:
    plural: backups
    singular: backup
    kind: Backup
    listKind: BackupList
    shortNames: [bk]
    categories: [all]
  versions:
  - name: v1
    served: true
    storage: true
    subresources:
      status: {}
      scale: {specReplicasPath: .spec.copies, statusReplicasPath: .status.copies}
    additionalPrinterColumns:
    - {name: Schedule, type: string, jsonPath: .spec.schedule}
    - {name: Copies, type: integer, jsonPath: .spec.copies}
    - {name: Phase, type: string, jsonPath: .status.phase}
    - {name: Age, type: date, jsonPath: .metadata.creationTimestamp}
    schema:
      openAPIV3Schema:
        type: object
        properties:
          spec:
            type: object
            required: [schedule]
            properties:
              schedule: {type: string, pattern: '^(\*|[0-9]+) (\*|[0-9]+) \* \* \*$', description: "cron-like m h * * *"}
              copies: {type: integer, minimum: 1, maximum: 5, default: 2}
              target: {type: string, enum: [s3, nfs, local], default: local}
              retentionDays: {type: integer}
              keepDays: {type: integer}
            x-kubernetes-validations:           # CEL 검증 규칙
            - rule: "!has(self.keepDays) || !has(self.retentionDays) || self.keepDays <= self.retentionDays"
              message: "keepDays must be <= retentionDays"
            - rule: "self.target == oldSelf.target"
              message: "target is immutable"
          status:
            type: object
            properties: {phase: {type: string}, copies: {type: integer}}
```

| 필드 | 의미 |
|---|---|
| `metadata.name` | `<복수형>.<그룹>` 형식 고정 |
| `group` | API 그룹 (도메인 형식으로 충돌 방지) |
| `scope` | `Namespaced`(네임스페이스에 속함) / `Cluster`(7장의 nodes처럼 전역) |
| `names` | plural(REST 경로), kind, shortNames, categories |
| `versions[]` | 버전별 `served`(API로 제공) / `storage`(etcd에 저장하는 버전, **정확히 하나**) / 스키마 |
| `schema.openAPIV3Schema` | 필드의 타입과 검증 규칙 (v1 CRD에서는 필수) |
| `subresources` | `status`, `scale` |
| `additionalPrinterColumns` | `kubectl get`에 보일 열 |

```
$ k apply -f crd.yaml                           →  customresourcedefinition.apiextensions.k8s.io/backups.t12.example.com created
$ k get crd backups.t12.example.com -o jsonpath='{.status.conditions[*].type}={.status.conditions[*].status}'
NamesAccepted Established=True True            ← 이름이 받아들여지고 API가 제공되는 상태(Established)
$ k api-resources --api-group=t12.example.com
NAME      SHORTNAMES   APIVERSION           NAMESPACED   KIND
backups   bk           t12.example.com/v1   true         Backup
$ k explain backup.spec
  copies <integer>   keepDays <integer>   retentionDays <integer>   schedule <string> -required- (cron-like m h * * *)   target <string> enum: s3, nfs, local
```

- CRD를 만들면 `api-resources`에 즉시 나타나고 `kubectl explain`이 **스키마의 `description`과 enum을 그대로** 보여 준다. 스키마가 곧 API 문서가 된다.

##### 2-3. 커스텀 리소스 만들기와 기본값

```
$ cat <<EOF | k apply -f -
apiVersion: t12.example.com/v1
kind: Backup
metadata: {name: nightly, namespace: t12}
spec: {schedule: "0 2 * * *"}
EOF
backup.t12.example.com/nightly created
$ k -n t12 get backup nightly -o jsonpath='{.spec}'
{"copies":2,"schedule":"0 2 * * *","target":"local"}          ← 스키마의 default가 채워졌다
$ k -n t12 get bk                                               ← shortName + printer columns
NAME      SCHEDULE    COPIES   PHASE   AGE
nightly   0 2 * * *   2                0s
$ k -n t12 get all | grep backup
backup.t12.example.com/nightly   0 2 * * *   4           11s      ← categories: [all] 이면 get all에 포함
```

- 스키마의 `default`가 **저장될 때** 채워진다 (`copies: 2`, `target: local`). 이 기본값은 API 서버가 처리하므로 컨트롤러가 없어도 적용된다.
- `additionalPrinterColumns`로 `kubectl get`의 열을 정의하고, `shortNames`로 `bk`로 줄여 쓸 수 있다.
- `categories: [all]`을 주면 `kubectl get all`에도 나온다 (기본은 안 나온다).

##### 2-4. 스키마 검증 (OpenAPI v3)

| 요청 | 결과 (실측) |
|---|---|
| `schedule` 누락 | `spec.schedule: Required value` |
| `copies: 9` (max 5) | `spec.copies in body should be less than or equal to 5` |
| `target: ftp` | `spec.target: Unsupported value: "ftp": supported values: "s3", "nfs", "local"` |
| `schedule: "every day"` | `spec.schedule in body should match '^(\*|[0-9]+) (\*|[0-9]+) \* \* \*$'` |
| `copies: "two"` | `spec.copies in body must be of type integer: "string"` |

- 타입, 범위(`minimum/maximum`), enum, 정규식(`pattern`), 필수(`required`)를 API 서버가 검증한다. 내장 리소스의 검증(10장)과 같은 단계에서 일어난다.
- 위 일부 오류에는 `<nil>: Invalid value: "null": some validation rules were not checked because the object was invalid; correct the existing errors to complete validation`이 덧붙었다. 스키마 오류가 있으면 CEL 규칙(2-5절)은 검사하지 않았다는 안내다.

**알 수 없는 필드**

```
$ k apply -f unk.yaml       # spec.bogus: 1
Error from server (BadRequest): ... strict decoding error: unknown field "spec.bogus"
$ k apply --validate=false -f unk.yaml
backup.t12.example.com/unk created
$ k -n t12 get backup unk -o jsonpath='{.spec}'
{"copies":2,"schedule":"0 2 * * *","target":"local"}              ← bogus는 저장되지 않고 버려짐
```

- kubectl의 기본(`fieldValidation=Strict`, 10장)은 모르는 필드를 오류로 거부한다. 검증을 끄면(`--validate=false`) **스키마에 없는 필드는 에러 없이 버려진다 (pruning).** 오타를 낸 필드가 조용히 사라지는 것이 CRD의 함정이다.
- 스키마를 정의하지 않고 아무 필드나 받으려면 `x-kubernetes-preserve-unknown-fields: true`를 쓴다 (2-8절에서 확인).

##### 2-5. CEL 검증 규칙 (`x-kubernetes-validations`)

스키마만으로 표현할 수 없는 **필드 간 관계**나 **변경 규칙**을 CEL(Common Expression Language) 식으로 검증한다. 웹훅 없이 CRD 정의 안에서 처리된다.

```
# 교차 필드 규칙: keepDays <= retentionDays
$ (retentionDays: 7, keepDays: 30)
The Backup "cel" is invalid: spec: Invalid value: "object": keepDays must be <= retentionDays

# 전환 규칙(transition rule): oldSelf를 참조 → 업데이트에서만 평가, target은 변경 불가
$ k -n t12 patch backup nightly --type=merge -p '{"spec":{"target":"s3"}}'
The Backup "nightly" is invalid: spec: Invalid value: "object": target is immutable
$ k -n t12 patch backup nightly --type=merge -p '{"spec":{"copies":4}}'
backup.t12.example.com/nightly patched                                  ← 다른 필드 변경은 허용
```

- `self`는 현재 값, `oldSelf`는 이전 값이다. `oldSelf`를 쓰면 **생성 시에는 평가하지 않고 업데이트에서만** 검사하는 규칙이 된다 (불변 필드 구현).
- 규칙에 `message`를 달면 오류 메시지로 쓰인다.

##### 2-6. 서브리소스: status와 scale

**status 서브리소스**

```
$ k -n t12 patch backup nightly --type=merge -p '{"status":{"phase":"Ready"}}'
backup.t12.example.com/nightly patched (no change)
$ k -n t12 get backup nightly -o jsonpath='phase=[{.status.phase}]'
phase=[]                                                              ← 메인 리소스로는 status를 못 쓴다 (무시됨)
$ k -n t12 patch backup nightly --subresource=status --type=merge -p '{"status":{"phase":"Ready","copies":2}}'
$ k -n t12 get bk nightly
nightly   0 2 * * *   4     Ready   12s
$ k -n t12 patch backup nightly --subresource=status --type=merge -p '{"spec":{"copies":5}}'
$ k -n t12 get backup nightly -o jsonpath='spec.copies=[{.spec.copies}]'
spec.copies=[4]                                                        ← status 서브리소스로는 spec을 못 바꾼다 (무시됨)
```

- `subresources.status`를 켜면 **`spec`은 메인 리소스로, `status`는 `/status` 서브리소스로만** 쓸 수 있다. 사용자가 `status`를 임의로 쓰지 못하게 하고, **컨트롤러만 status를 갱신**하게 권한을 나눌 수 있다 (10장 RBAC에서 `resource/status`를 따로 줄 수 있다). 6장의 `Deployment.status`가 컨트롤러에 의해 갱신되는 것과 같은 구조다.

**scale 서브리소스**

```
$ k -n t12 scale backup nightly --replicas=3                  →  backup.t12.example.com/nightly scaled
$ k -n t12 get backup nightly -o jsonpath='spec.copies={.spec.copies}'     →  3                   ← specReplicasPath
$ k get --raw /apis/t12.example.com/v1/namespaces/t12/backups/nightly/scale
Scale {'replicas': 3} {'replicas': 2}                           ← spec.replicas ← .spec.copies, status.replicas ← .status.copies
```

- `scale` 서브리소스를 정의하면 **`kubectl scale`이 CR에서도 동작**하고 HPA 같은 도구가 이 리소스를 스케일 대상으로 쓸 수 있다.

##### 2-7. REST 경로와 etcd 저장

```
$ k get --raw /apis/t12.example.com/v1/namespaces/t12/backups/nightly
{"apiVersion":"t12.example.com/v1","kind":"Backup", ... "spec":{"copies":3,"schedule":"0 2 * * *","target":"local"}}
```

- 경로는 **`/apis/<group>/<version>/namespaces/<ns>/<plural>/<name>`** 이다 (내장 리소스와 같은 형식, 코어 그룹만 `/api/v1`). 6~8장의 Watch API(`?watch=true`)도 CR에 그대로 쓸 수 있다 (이 장에서는 컨트롤러 데모에서 폴링 방식으로 썼고 Watch는 직접 시험하지 않았다).

```
$ etcdctl get /registry/t12.example.com/backups/t12/nightly --print-value-only
{"apiVersion":"t12.example.com/v1","kind":"Backup","metadata":{"annotations":{"kubectl.kubernetes.io/last-applied-configuration":"..."},"creationTimestamp":"...","generation":3,"managedFields":[...
$ etcdctl get /registry/apiextensions.k8s.io/customresourcedefinitions/ --prefix --keys-only | grep backups
/registry/apiextensions.k8s.io/customresourcedefinitions/backups.t12.example.com
```

- CR은 etcd에 **`/registry/<group>/<plural>/<namespace>/<name>`** 키로 저장된다 (6장의 `/registry/pods/<ns>/<name>`과 같은 규칙).
- 내장 리소스는 protobuf라 etcd에서 사람이 읽을 수 없었지만(6, 7장), **CR은 JSON 그대로 저장**되어 읽을 수 있었다.
- CRD 자체도 오브젝트라서 `/registry/apiextensions.k8s.io/customresourcedefinitions/<name>`에 저장된다.

##### 2-8. 버전, 범위, 스키마 없는 CRD

**여러 버전**

```
$ (v1에 이어 v1beta1을 served: true, storage: false로 추가, conversion 기본값)
$ k get crd backups.t12.example.com -o jsonpath='versions={.spec.versions[*].name} storage={...storage==true.name} storedVersions={.status.storedVersions} conversion={.spec.conversion.strategy}'
versions=v1 v1beta1 storage=v1 storedVersions=["v1"] conversion=None
$ k get --raw /apis/t12.example.com/v1beta1/namespaces/t12/backups/nightly
read as t12.example.com/v1beta1 5                                       ← 같은 객체를 v1beta1으로도 읽을 수 있다
```

- 여러 버전을 동시에 `served`로 두되 etcd에 저장하는 `storage` 버전은 **하나**다. 요청한 버전으로 변환해서 응답한다.
- `conversion.strategy: None`(기본)은 **`apiVersion` 문자열만 바꿔 줄 뿐 필드는 변환하지 않는다.** 버전 간 스키마가 다르면 웹훅 변환이 필요하다 (이번에 시험하지 않았다. 위 실험은 두 버전의 스키마가 같다).
- `api-resources`에는 버전이 여럿이어도 한 줄(`v1`)로 나왔다.

**CRD 수정 제약**

```
$ (storage: true 를 두 버전에 지정)
status.storedVersions: Invalid value: ["v1"]: must have the storage version v1beta1
$ (이미 etcd에 저장된 버전 v1을 제거)
status.storedVersions[0]: Invalid value: "v1": must appear in spec.versions
$ (kind: Backup → Snapshot)
spec.names.kind: Invalid value: "Snapshot": field is immutable
```

- `status.storedVersions`는 **지금까지 etcd에 저장된 적 있는 버전 목록**이라서, 저장된 버전은 스토리지를 이전하기 전에는 `spec.versions`에서 뺄 수 없다. `kind` 같은 이름은 바꿀 수 없다. 저장 버전을 바꾸려면 기존 객체를 새 버전으로 다시 저장하는 마이그레이션을 거치고 `storedVersions`를 정리해야 한다 (마이그레이션은 이번에 하지 않았다).

**cluster 범위와 스키마 없는 CRD**

```
$ (scope: Cluster, schema에 x-kubernetes-preserve-unknown-fields: true)
$ k apply -f policy.yaml    # spec: {anything: {goes: [1,2,3]}}
policy.t12.example.com/global created
$ k get policy global -o jsonpath='{.spec}'
{"anything":{"goes":[1,2,3]}}                                           ← 구조 검증 없이 그대로 저장
$ k apply -f policy-ns.yaml   # cluster 범위인데 namespace: t12 지정
policy.t12.example.com/ns1 created                                       ← 오류 없이 생성, 목록은 global ns1 (전역)
```

- `scope: Cluster`인 CR은 네임스페이스에 속하지 않는다 (`k get policy` → `global`, `ns1`). `metadata.namespace`를 지정해도 오류 없이 만들어졌고 전역 리소스가 되었다.
- `x-kubernetes-preserve-unknown-fields: true`는 그 위치의 하위 구조를 검증/버림 없이 통째로 저장한다. 유연하지만 **검증과 문서화를 포기**하는 선택이다.

##### 2-9. 컨트롤러 (CR + reconcile loop)

CRD만으로는 `Backup`이 아무 일도 하지 않는다. 컨트롤러가 있어야 한다. 6장의 컨트롤러 개념을 그대로 CR에 적용한 최소 컨트롤러를 만들었다. 2초마다 Backup을 읽고, **각 Backup에 대응하는 ConfigMap**(`backup-<이름>`)을 만들거나 갱신하고, Backup의 `status`를 `Reconciled`로 쓴다. ConfigMap에는 `ownerReferences`로 Backup을 소유자로 지정했다 (Python 약 25줄, `kubectl`을 호출하는 폴링 방식. 실제 컨트롤러는 Watch를 쓴다).

```
$ python3 controller.py 40 &
23:11:12 reconcile nightly -> configmap/backup-nightly serverside-applied
$ k -n t12 get cm | grep backup           →  backup-nightly
$ k -n t12 get bk
nightly   0 2 * * *   3   Reconciled   32s                                ← status.phase를 컨트롤러가 채움
$ k -n t12 patch backup nightly --type=merge -p '{"spec":{"copies":5}}'     # 원하는 상태 변경
23:11:18 reconcile nightly -> configmap/backup-nightly serverside-applied
$ k -n t12 get cm backup-nightly -o jsonpath='{.data}'
{"copies":"5","schedule":"0 2 * * *"}                                      ← 컨트롤러가 실제 상태를 맞춤
$ (Backup weekly 생성)  →  23:11:25 reconcile weekly   ... weekly   0 3 * * *   2   Reconciled
$ k -n t12 delete backup weekly           →  ConfigMap backup-weekly 도 함께 삭제 (ownerReferences → 가비지 컬렉터)
```

- CR(원하는 상태)을 바꾸자 컨트롤러가 **실제 리소스(ConfigMap)** 를 맞췄고 결과를 `status`에 기록했다. **사용자 정의 API + 이를 맞추는 컨트롤러 = 오퍼레이터** 패턴이다.
- CR을 삭제하면 `ownerReferences`로 연결된 ConfigMap이 가비지 컬렉터에 의해 지워졌다 (ReplicaSet이 Pod를 소유하는 6장의 관계와 같다). weekly 삭제 후 4초 뒤 `backup-weekly`가 사라진 것을 확인했다.
- 컨트롤러 안에서 server-side apply(`--field-manager=backup-controller`)를 썼다 (1-6절의 필드 소유권 개념의 실제 사용).
- 실제 컨트롤러는 Watch API(8장)와 로컬 캐시(informer)를 쓰고 Go의 controller-runtime/kubebuilder, operator-sdk 등의 프레임워크로 작성한다 (이 환경에서 직접 시험하지 않았다).

##### 2-10. RBAC와 CRD

```
$ k auth can-i list backups.t12.example.com -n t12 --as=system:serviceaccount:t12:crd-reader        →  no        # 권한 없음
$ k -n t12 create role bk-read --verb=get,list,watch --resource=backups.t12.example.com
$ k -n t12 create rolebinding bk-read --role=bk-read --serviceaccount=t12:crd-reader
$ k auth can-i list backups.t12.example.com -n t12 --as=...crd-reader                               →  yes
$ k auth can-i delete backups.t12.example.com -n t12 --as=...crd-reader                             →  no

# 기본 ClusterRole view는 커스텀 리소스를 포함하지 않는다
$ k -n t12 create rolebinding v --clusterrole=view --serviceaccount=t12:crd-viewer
$ k auth can-i list backups.t12.example.com -n t12 --as=...crd-viewer                               →  no
$ (ClusterRole에 라벨 rbac.authorization.k8s.io/aggregate-to-view: "true" 와 backups get/list/watch 규칙)
$ k auth can-i list backups.t12.example.com -n t12 --as=...crd-viewer                               →  yes     # 4초 뒤 집계됨
```

- CR도 일반 리소스와 같이 RBAC로 통제한다. API 그룹은 CRD의 `group`(`t12.example.com`), 리소스는 `plural`(`backups`)이다.
- 10장에서 본 **집계(aggregation)** 의 실제 쓰임이다. CRD를 배포하는 쪽이 `aggregate-to-view/edit/admin` 라벨을 단 ClusterRole을 함께 배포하면 기본 `view/edit/admin`에 CR 권한이 자동으로 합쳐진다.
- 참고: `k auth can-i get backups.t12.example.com/status`가 `yes`로 나왔지만, 이 문법은 `리소스/이름`이라 **이름이 `status`인 Backup**에 대한 질문이다. `/status` 서브리소스의 권한이 아니므로 이 결과로 서브리소스 권한을 판단하면 안 된다 (서브리소스 권한 실험은 하지 않았다).

##### 2-11. CRD 삭제

```
$ k delete crd backups.t12.example.com
before: CRs=2 etcd keys=2          # CR 2개 (nightly, extra), etcd 키 2개
customresourcedefinition.apiextensions.k8s.io "backups.t12.example.com" deleted
after:  etcd keys=0                # CR의 etcd 키가 모두 삭제됨
$ k -n t12 get backups
Error from server (NotFound): Unable to list "t12.example.com/v1, Resource=backups": the server could not find the requested resource
$ (ConfigMap backup-nightly 등) → 가비지 컬렉터가 함께 삭제
```

- **CRD를 지우면 그 종류의 모든 CR이 함께 삭제된다.** 네임스페이스를 지우면 안의 리소스가 다 지워지는 것(7장)과 같은 파급이 클러스터 전체에서 일어난다. 운영에서 CRD 삭제는 매우 위험하다.
- CR에 소유된 ConfigMap도 소유자 삭제에 따라 정리되었다.

##### 2-12. 정리

| 구분 | 내용 |
|---|---|
| CRD | 새 리소스 종류의 정의. 만들면 API 경로가 즉시 열린다 |
| CR | 그 종류의 인스턴스. etcd에 JSON으로 저장 |
| 스키마 | 타입/범위/enum/pattern/required/default 검증, 알 수 없는 필드는 거부 또는 버림 |
| CEL | 필드 간 관계, 불변 필드(`oldSelf`) |
| 서브리소스 | `status`(권한 분리), `scale`(kubectl scale/HPA) |
| 컨트롤러 | CR을 감시해서 실제 상태를 맞춤 (CRD만으로는 동작 없음) |
| RBAC | `group`/`plural`로 통제, 집계 라벨로 기본 역할에 합침 |
| 주의 | CRD 삭제 시 CR 전체 삭제, 버전 변환/스토리지 마이그레이션 |

#### 3. 확인하지 못한 부분

- **클라이언트 측 apply의 3-way merge 상세**: 리스트 필드(`containers`, `ports` 등)의 병합 키와 `retainKeys`, `$patch` 지시자는 확인하지 않았다. `apply --prune`/`--applyset`, `kubectl diff`와 `apply`의 차이, `kubectl edit`의 동작은 이번에 다루지 않았다.
- **서버 측 적용의 상세**: 리스트 소유권(`listType`, `listMapKeys`), 같은 값을 다른 매니저가 설정할 때의 공동 소유, 충돌 해결 정책의 나머지 경우, 서버 측 적용과 `last-applied` 어노테이션이 섞였을 때의 마이그레이션은 확인하지 않았다.
- **GitOps 도구**(Argo CD, Flux): 개념만 언급했고 설치/실행하지 않았다.
- **CRD 변환 웹훅**(스키마가 다른 버전 간 변환), 스토리지 버전 마이그레이션, 폐기 버전 경고(`deprecated`, `deprecationWarning`)는 확인하지 않았다.
- **어드미션 웹훅/ValidatingAdmissionPolicy와 CR**: CEL 규칙(`x-kubernetes-validations`) 외에는 확인하지 않았다. CEL 비용 제한, 식 컴파일 오류가 CRD 생성에 미치는 영향은 시험하지 않았다.
- **CR의 finalizer, `deletionTimestamp`, `ownerReferences.blockOwnerDeletion`의 상세**와 foreground/background 삭제 차이는 확인하지 않았다 (컨트롤러 데모에서 background GC만 확인).
- **커스텀 컨트롤러 구현**: Go의 controller-runtime, kubebuilder, operator-sdk, informer/작업 큐, 리더 선출, Watch 기반 reconcile은 직접 구현하지 않았다. 데모 컨트롤러는 2초 폴링을 쓴 단순한 구현이다.
- **CRD와 API 집계 계층(APIService)의 차이**: CRD만 다뤘고 별도의 확장 API 서버는 다루지 않았다.
- **서브리소스 RBAC**: `backups/status`, `backups/scale`에 대한 권한 분리는 `can-i`의 해석 문제로 직접 확인하지 못했다 (2-10절 참고).
- **CR의 `kubectl apply`/strategic merge**: CRD에는 strategic merge patch가 지원되지 않는다는 점은 문서 지식이고 이번 실험에서는 `--type=merge`만 사용했다.
- **크기와 규모**: CR 개수/크기가 etcd와 API 서버에 주는 부하, CRD 스키마 크기 제한은 확인하지 않았다.
- **Watch API로 CR 이벤트를 직접 받는 실험**은 하지 않았다 (경로와 서브리소스, 컨트롤러 폴링만 확인).
- **`scope: Cluster` CRD의 `api-resources` NAMESPACED 열**: 출력 파싱 실수로 값을 제대로 읽지 못했다. `k get policy`가 네임스페이스 없이 `global`, `ns1`을 보여 준 것으로 전역 리소스임을 확인했다.
