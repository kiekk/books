### 19장. 사용자 정의 자원

> 정리 범위: CRD와 Aggregated API의 특징, 선언적 API의 형태(구조) 정의
>
> 실측 환경: 16~18장과 같은 **임시 kind 클러스터 `t16`**(v1.35.0, control-plane 1 + worker 1, 별도 kubeconfig)을 이어서 썼다. CRD는 직접 만든 `widgets.example.com`(Widget)이고, Aggregated API의 실제 예로는 17장에서 설치한 metrics-server(`v1beta1.metrics.k8s.io`)를 사용했다. 선언적 API의 컨트롤러 동작은 이 PC에서 도는 **간단한 셸 루프**(3초 주기로 Widget을 읽어 ConfigMap을 맞추는 스크립트)로 흉내 냈다. 실제 컨트롤러/오퍼레이터 구현(client-go, controller-runtime, kubebuilder)은 만들지 않았다. 네임스페이스는 `t16`이고, 실험 뒤 클러스터는 삭제했다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: 컨트롤러가 "원하는 상태와 현재 상태"를 맞추는 방식은 [6장](../chapter06/README.md), `kubectl apply`/레이블/애너테이션은 [9장](../chapter09/README.md), RBAC은 [13장](../chapter13/README.md), metrics-server는 [17장](../chapter17/README.md)에서 다뤘다.

#### 목차

1. 쿠버네티스 API 확장 방법 두 가지
2. CRD (CustomResourceDefinition)
3. Aggregated API
4. CRD vs Aggregated API
5. 선언적 API의 형태 정의
6. 선택 가이드와 함정
7. 확인하지 못한 부분

---

#### 1. 쿠버네티스 API 확장 방법 두 가지

쿠버네티스 API에 새 종류의 오브젝트를 추가하는 방법은 둘이다.

| | CRD | Aggregated API (API 서버 확장) |
|---|---|---|
| 개념 | API 서버가 **선언만 보고** 새 리소스를 대신 처리 | **별도의 API 서버**를 만들어 메인 API 서버 뒤에 붙임 |
| 코드 작성 | 필요 없음(YAML) | **API 서버 코드를 직접 구현** |
| 저장소 | 메인 API 서버의 etcd | **자체 저장소 사용 가능** (etcd가 아니어도 됨) |
| 등록 | `CustomResourceDefinition` | `APIService` (+ 백엔드 Service/Pod) |
| 예 | Argo CD `Application`, cert-manager `Certificate`, 이 문서의 `Widget` | **metrics-server**(`metrics.k8s.io`), 서비스 카탈로그 등 |

---

#### 2. CRD (CustomResourceDefinition)

##### 2-1. 정의와 등록 (실측)

```yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata: {name: widgets.example.com}          # <plural>.<group> 형식이어야 한다
spec:
  group: example.com
  scope: Namespaced
  names: {plural: widgets, singular: widget, kind: Widget, listKind: WidgetList, shortNames: [wg], categories: [all]}
  versions:
  - name: v1
    served: true
    storage: true
    subresources: {status: {}, scale: {specReplicasPath: .spec.size, statusReplicasPath: .status.ready}}
    additionalPrinterColumns: [{name: Size, type: integer, jsonPath: .spec.size}, ...]
    schema: {openAPIV3Schema: {...}}
```

```
$ kubectl apply -f crd.yaml
$ kubectl get crd widgets.example.com -o jsonpath='{.status.conditions...}'
NamesAccepted=True Established=True        ← 등록 완료 조건
$ kubectl api-resources --api-group=example.com
widgets   wg   example.com/v1   true   Widget
$ kubectl get --raw /apis/example.com/v1 → ['widgets', 'widgets/status', 'widgets/scale']
```

- CRD를 만든 **즉시 새 REST 엔드포인트가 생기고** `kubectl get widgets`, `kubectl get wg`, `-o yaml`, `describe`, watch, RBAC이 모두 동작한다.
- `categories: [all]`로 `kubectl get all`에 Widget이 함께 표시됐다.
- `additionalPrinterColumns`로 `kubectl get`의 열(SIZE, COLOR, PHASE, AGE)을 정의했다.
- CRD를 등록하면 **자동 관리되는 APIService(`v1.example.com`, SERVICE `Local`, 레이블 `kube-aggregator.kubernetes.io/automanaged: "true"`)가 생긴다.** 즉 CRD도 내부적으로는 같은 집계 계층에 등록되지만 백엔드가 `Local`(메인 API 서버 자신)이다.

##### 2-2. 스키마 검증 (OpenAPI v3, 실측)

| 입력 (Widget `spec`) | 결과 |
|---|---|
| `{size: 3}` | 생성됨. **기본값 적용**: `{"color":"red","size":3}` (`color` default red) |
| `size` 없음 | `spec.size: Required value` |
| `size: "three"` | `spec.size in body must be of type integer: "string"` |
| `size: 0` | `should be greater than or equal to 1` (`minimum`) |
| `size: 11` | `should be less than or equal to 10` (`maximum`) |
| `color: pink` | `Unsupported value: "pink": supported values: "red", "green", "blue"` (`enum`) |
| `name: ABC` | `should match '^[a-z]+$'` (`pattern`) |
| `spec` 자체 없음 | `spec: Required value` |
| `name: forbidden` | **CEL 규칙**: `name must not be 'forbidden'` |

- 스키마 위반이 있으면 **CEL 규칙은 평가되지 않는다**(오류 메시지에 `some validation rules were not checked because the object was invalid`).
- **CEL(`x-kubernetes-validations`)**: `self`로 현재 값, `oldSelf`로 이전 값을 참조한다. 이 실험의 변환(전이) 규칙 `self.size >= oldSelf.size || self.color != 'blue'`로 파란 Widget의 size를 줄이는 수정(5 → 2)은 `blue widgets cannot shrink`로 거부됐고, 늘리는 수정(5 → 7)은 통과했다. 변경 불가 필드 같은 규칙을 웹훅 없이 선언할 수 있다.
- **미정의 필드(`spec.bogus`)**: kubectl(v1.35)의 기본 동작에서 **서버 쪽 엄격 검증으로 거부**됐다(`strict decoding error: unknown field "spec.bogus"`). `--validate=false`로 검증을 끄면 **생성되지만 `bogus`는 저장 전에 제거(pruning)** 됐다(`{"color":"red","size":1}`만 남음). 스키마에 없는 필드를 보존하려면 `x-kubernetes-preserve-unknown-fields: true`를 써야 한다(문서 지식, 시험하지 않음).

##### 2-3. status 서브리소스와 generation (실측)

`subresources.status`를 켜면 `spec`과 `status`의 쓰기 경로가 분리된다.

| 시도 | 결과 |
|---|---|
| 메인 리소스에 `status.phase=HACK` patch | `patched (no change)`, 값은 비어 있음 (**무시됨**) |
| `--subresource=status`로 `status.phase=Ready, ready=3` + `spec.size=9` patch | `phase=Ready`만 반영, **`spec.size`는 그대로 3** (status 쪽에서는 spec 무시) |
| `spec.note` 변경 | `metadata.generation` 1 → 2, `resourceVersion` 증가 |
| `status.ready` 변경 | `generation` **2 유지**, `resourceVersion`만 증가 (4452 → 4453) |

- `metadata.generation`은 **spec이 바뀔 때만** 증가하고, `resourceVersion`은 어떤 변경이든 증가한다. 컨트롤러는 `status.observedGeneration`에 "내가 처리한 generation"을 써서 반영 여부를 표시한다(5절).
- 사용자(spec)와 컨트롤러(status)의 권한도 `widgets`와 `widgets/status`로 나눌 수 있다(2-5).

##### 2-4. scale 서브리소스

`scale: {specReplicasPath: .spec.size, statusReplicasPath: .status.ready}`를 정의하면 `kubectl scale widget w1 --replicas=4`가 동작해 `spec.size`가 4가 되고 **HPA의 대상으로도 쓸 수 있다**(HPA는 scale 서브리소스를 통해 동작, 18장). 이번 실험에서는 HPA와 연결하지는 않았다.

##### 2-5. 삭제, 소유자, 권한 (실측)

- **finalizer**: `finalizers: [example.com/cleanup]`을 가진 Widget을 삭제하면 `deletionTimestamp`만 찍히고 남아 있다가, finalizer를 제거하자 사라졌다(컨트롤러가 정리 작업 후 제거하는 방식).
- **ownerReference와 가비지 컬렉션**: Widget을 `ownerReferences`로 가리키는 ConfigMap `owned`는 **Widget 삭제 후 5초 안에 같이 삭제**됐다.
- **RBAC**: CR에도 Role/RoleBinding을 쓴다. 리소스는 `widgets.example.com`(그룹 포함)으로 지정한다. `list`만 준 계정에서 `list widgets.example.com` yes, `create` no, `get widgets --subresource=status` no였다.
- **CRD를 삭제하면 모든 CR이 함께 삭제**된다(문서 지식, 이번에 시험하지 않음). 운영에서 CRD 삭제는 데이터 삭제와 같다.

##### 2-6. 다중 버전 (실측)

`v1`(storage)과 `v2`(served, storage 아님)을 모두 정의하고 `conversion: {strategy: None}`으로 적용했다.

```
$ kubectl get --raw /apis/example.com
versions: ['example.com/v2', 'example.com/v1']   preferred: example.com/v2
$ kubectl get widget.v1.example.com blue  → apiVersion example.com/v1  {"color":"blue","size":7}
$ kubectl get widget.v2.example.com blue  → apiVersion example.com/v2  {"color":"blue","size":7}
status.storedVersions=["v1"]
```

- `strategy: None`은 **apiVersion 문자열만 바꿔서** 보여 준다(스키마가 같을 때만 안전). 스키마가 달라지면 변환 웹훅(`strategy: Webhook`)이 필요하다(시험하지 않음).
- 저장 버전(`storage: true`) 하나만 허용되고, 이미 저장된 버전(`status.storedVersions`)을 `spec.versions`에서 지우려 하면 거부된다: `must have exactly one version marked as storage version` 및 `v1 was previously a storage version, and must remain in spec.versions until a storage migration ensures no data remains persisted in v1`. 버전 제거에는 저장 데이터 마이그레이션이 선행돼야 한다.
- 이 실험에서 **먼저 `preferredVersion`이 `v2`로 바뀌었다**(v2를 추가하자 우선 버전이 v2가 됨).

---

#### 3. Aggregated API

메인 API 서버(`kube-apiserver`) 안의 **집계 계층(aggregator)** 이 특정 API 그룹의 요청을 다른 서버(Service)로 **프록시**한다.

##### 3-1. 등록: APIService (실측, metrics-server)

```
$ kubectl get apiservice v1beta1.metrics.k8s.io -o yaml
spec:
  group: metrics.k8s.io
  version: v1beta1
  groupPriorityMinimum: 100
  versionPriority: 100
  insecureSkipTLSVerify: true
  service: {name: ms-metrics-server, namespace: kube-system, port: 443}
$ kubectl get apiservice
v1.example.com   Local                          True      ← CRD(자동 등록)
v1beta1.metrics.k8s.io   kube-system/ms-metrics-server   True      ← 외부 서비스 백엔드 (이것 하나만 Local 아님)
(그 외 22개는 Local = 메인 API 서버가 직접 제공하는 내장 그룹)
```

- `service`가 있으면 그 Service로 프록시하고, `Local`이면 메인 API 서버 자신이 처리한다. CRD 그룹은 `Local`이다.
- 프록시 시 **인증 정보 전달**용으로 API 서버의 인자에 `--proxy-client-cert-file/--proxy-client-key-file`(front-proxy 클라이언트 인증서)와 `--requestheader-*` 설정이 있었다(`--requestheader-username-headers=X-Remote-User`, `--requestheader-group-headers=X-Remote-Group` 등). 즉 백엔드는 사용자가 누구인지를 헤더로 전달받는다(헤더 전달 구조는 설정만 확인했고 요청을 가로채 보지는 않았다).

##### 3-2. 백엔드 장애 시 (실측)

metrics-server를 0으로 줄였다.

```
$ kubectl get apiservice v1beta1.metrics.k8s.io
Available=False reason=MissingEndpoints msg=endpointslices for service/ms-metrics-server in "kube-system" have no addresses with port name "https"
$ kubectl get --raw /apis/metrics.k8s.io/v1beta1/nodes
Error from server (ServiceUnavailable): the server is currently unable to handle the request
$ kubectl api-resources     → metrics.k8s.io 그룹(nodes, pods)은 목록에는 계속 보였다
$ kubectl get widgets       → 정상 (CRD는 영향 없음)
```

- 백엔드가 죽으면 **그 그룹만 `503 ServiceUnavailable`**이고 나머지 API는 영향이 없다.
- 이 실험에서는 **네임스페이스 삭제가 막히지 않았다**(빈 네임스페이스를 삭제해 곧바로 사라졌다). 집계 API 장애가 네임스페이스 삭제를 막는 알려진 문제는 이 조건에서 재현하지 못했다.

---

#### 4. CRD vs Aggregated API

| 항목 | CRD | Aggregated API |
|---|---|---|
| 구현 난이도 | 낮음 (YAML만) | 높음 (API 서버 코드, 인증서, 배포) |
| 저장소 | 메인 etcd (공유) | 자체 선택 가능 |
| 검증 | OpenAPI 스키마 + CEL(선언), 복잡한 건 어드미션 웹훅 | 코드로 자유롭게 |
| 서브리소스 | `status`, `scale`만 | 임의의 서브리소스 가능 |
| API 표현/동작 | 표준 CRUD + watch 고정 | 자유 (예: metrics-server는 **읽기 전용**, 저장 없이 즉석 계산) |
| 가용성/성능 영향 | 메인 API 서버와 etcd 부하를 같이 씀 | 별도 서버라서 격리되지만 운영할 서버가 늘어남 |
| 장애 범위 | CRD 자체는 API 서버의 일부 | 백엔드 장애 시 그 그룹만 503 |
| 적합한 경우 | 설정/상태를 오브젝트로 표현하고 컨트롤러가 처리 (대부분) | 저장소가 다르거나 계산 결과를 API로 제공할 때 |

한 줄 요약: **대부분은 CRD로 충분하다.** 저장소가 etcd가 아니거나 CRUD와 다른 API 의미가 필요할 때만 Aggregated API를 쓴다.

---

#### 5. 선언적 API의 형태 정의

##### 5-1. 선언적(declarative) API의 형태

쿠버네티스 오브젝트(CRD 포함)는 다음 공통 구조를 가진다.

```yaml
apiVersion: example.com/v1     # API 그룹/버전
kind: Widget                   # 종류
metadata:                      # 이름, 네임스페이스, 레이블, 애너테이션, uid, generation, resourceVersion, ownerReferences, finalizers ...
  name: demo
spec:                          # 원하는 상태 (사용자가 선언)
  size: 5
  color: green
status:                        # 관측된 현재 상태 (컨트롤러가 기록)
  phase: Ready
  ready: 5
  observedGeneration: 2        # 컨트롤러가 어느 generation 까지 처리했나
```

| 필드 | 쓰는 쪽 | 의미 |
|---|---|---|
| `spec` | 사용자 | **원하는 상태(desired state)** |
| `status` | 컨트롤러 | **현재 관측 상태(observed state)** |
| `metadata.generation` | API 서버 | spec 변경 횟수 |
| `status.observedGeneration` | 컨트롤러 | 처리 완료한 generation |
| `metadata.resourceVersion` | API 서버 | 낙관적 동시성 제어, watch 기준 |

"어떻게(절차)"가 아니라 **"무엇을(결과)"을 선언**하고, 컨트롤러가 현재를 원하는 상태로 수렴시킨다.

##### 5-2. 컨트롤러의 조정(reconcile) 루프 (셸 루프로 재현)

3초 주기로 모든 Widget을 읽어서 ConfigMap `widget-<이름>`의 값을 spec에 맞추고 `status`를 갱신하는 스크립트를 이 PC에서 실행했다.

| 상황 | 결과 |
|---|---|
| 컨트롤러 시작 전 Widget `demo`(size 2, green) | `generation=1`, `observedGeneration` 없음, ConfigMap 없음 |
| 컨트롤러 시작 | 약 8초 뒤 ConfigMap 생성(`size=2, color=green`), `PHASE Ready` |
| `spec.size`를 2 → 5로 수정 | 6초 안에 ConfigMap 5로 갱신, `generation=2 observed=2 ready=5` |
| **드리프트**: ConfigMap을 직접 `size=99`로 수정 | 6초 안에 **5로 되돌림** |
| ConfigMap 삭제 | 6초 안에 **다시 생성** |
| 컨트롤러 중지 후 spec을 8로 수정 | `generation=3 observed=2` (**stale**), ConfigMap은 5 그대로 |
| 컨트롤러 중지 중에 spec을 3 → 6 → 4로 연속 수정 후 컨트롤러 재시작 | `generation=6 observed=6`, ConfigMap `size=4` (**3과 6은 한 번도 처리되지 않고 최종 상태 4만 반영**) |

- `generation`과 `observedGeneration`의 차이로 "아직 반영 안 됨"을 알 수 있다(롤아웃 상태 `kubectl rollout status`가 같은 원리).
- 컨트롤러는 **변화 이벤트가 아니라 현재 상태(level)** 를 기준으로 동작한다. 중간 변경을 놓쳐도(컨트롤러 다운) 재시작 후 최종 desired 상태로 수렴하고, 직접 수정한 드리프트도 복구한다. 이를 "level-triggered" 방식이라 부른다(이 문서의 셸 루프는 단순 폴링이라 이벤트(edge) 기반 구현과의 차이를 직접 대조하지는 않았다).
- 같은 spec을 여러 번 적용해도 결과가 같아야 한다(**멱등성**). 이 스크립트는 현재 값과 비교해 다를 때만 수정한다.
- 실제 컨트롤러는 폴링 대신 **watch**로 변경을 받고 작업 큐로 처리한다(문서 지식, 이번에 구현하지 않음).

##### 5-3. kubectl apply

`kubectl apply -f`는 선언된 파일을 서버 상태에 합치는 선언적 명령이다. 이 실험에서도 `metadata.annotations["kubectl.kubernetes.io/last-applied-configuration"]`과 `managedFields`(`kubectl-client-side-apply`, `kubectl-patch` 등 관리자별 필드 소유 정보)가 기록된 것을 봤다(9장의 `last-applied`, 그리고 서버 측 필드 소유 개념). `create`/`replace` 같은 명령형과 달리 apply는 **원하는 상태 전체**를 전달한다.

---

#### 6. 선택 가이드와 함정

| 질문 | 답 |
|---|---|
| 새 리소스 종류가 필요하다 | 먼저 CRD (+ 컨트롤러/오퍼레이터) |
| 스키마를 꼭 써야 하나 | v1 CRD는 **구조적(structural) 스키마가 필수** |
| 정의에 없는 필드 | 서버가 거부(strict) 또는 pruning(검증 끈 경우) |
| 불변 필드, 값 간 관계 검증 | CEL(`x-kubernetes-validations`, `oldSelf`) |
| spec/status 권한 분리 | `status` 서브리소스 + `widgets/status` RBAC |
| CR을 지울 때 정리 작업 | finalizer + 컨트롤러 |
| 하위 리소스 자동 정리 | `ownerReferences` (가비지 컬렉션) |
| 버전 올리기 | 새 버전 추가 → 저장 버전 전환 → 마이그레이션 후 옛 버전 제거 |
| 읽기 전용 계산 API / 별도 저장소 | Aggregated API |
| 집계 API 백엔드 장애 | 그 그룹만 503, 다른 API 정상 |
| 컨트롤러가 죽었다 | CR은 남지만 `observedGeneration`이 뒤처짐, 재시작 시 최종 상태로 수렴 |

함정:

- CRD 삭제는 **모든 CR 삭제**다(문서 지식).
- 스키마 오류가 있으면 CEL 규칙은 검사되지 않는다(필수/타입 오류부터 고쳐야 규칙 오류가 보인다).
- `kubectl apply`로 `status`를 넣어도 무시된다(`status` 서브리소스를 켠 경우).
- 저장 버전은 하나이고, 이미 저장된 버전은 마이그레이션 전에 지울 수 없다.
- Aggregated API는 `insecureSkipTLSVerify`(이 실험의 metrics-server처럼)를 쓰면 백엔드 인증서를 검증하지 않는다. 운영에서는 `caBundle`을 구성한다.

---

#### 7. 확인하지 못한 부분

- **진짜 컨트롤러/오퍼레이터**(client-go, kubebuilder/controller-runtime의 watch, 작업 큐, leader election, 재시도), **어드미션 웹훅/변환 웹훅**은 만들지 않았다. 5-2의 셸 루프는 개념 시연용 폴링이다.
- **CRD 삭제 시 CR 일괄 삭제**, `x-kubernetes-preserve-unknown-fields`, `x-kubernetes-list-type`, `x-kubernetes-map-type`, 필드 셀렉터(`selectableFields`)는 시험하지 않았다.
- **서버 측 apply(SSA)와 필드 소유권 충돌**은 이번에 실험하지 않았다(`managedFields`의 존재만 확인).
- **Aggregated API를 직접 구현**(예: sample-apiserver)하지 못했다. metrics-server를 사용 예로 관찰했을 뿐이다. 요청 헤더 전달(`X-Remote-User`)이 실제 백엔드까지 가는지는 가로채 확인하지 못했다.
- 집계 API 장애가 **네임스페이스 삭제나 가비지 컬렉션, `kubectl api-resources`를 막는 문제**는 재현하지 못했다(조건에 따라 다를 수 있음).
- CR을 대상으로 한 **HPA 연동**, `scale` 서브리소스의 `labelSelectorPath`는 확인하지 않았다.
- **etcd에 저장되는 CR의 실제 크기/성능 영향**, CRD가 많을 때의 API 서버 부담은 측정하지 않았다.
- **CRD 버전 마이그레이션**(`storedVersions` 정리, storage version migrator)과 변환 웹훅은 시험하지 않았다.
- **EKS 등 관리형 클러스터의 집계 API/CRD 제약**은 확인하지 못했다.
