### 9장. 레이블과 애너테이션

> 정리 범위: 레이블(Label)과 애너테이션(Annotation)의 개념과 특징
>
> 실측 환경: macOS (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드 `docker-desktop`), kubectl v1.35.2. 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`을 명시했다(`k`로 줄여 적음). 실험 네임스페이스 `t9`는 끝나고 삭제했고, 노드에 붙였던 임시 레이블도 제거했다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: Deployment/ReplicaSet의 selector 불변성과 `pod-template-hash`는 6장, Service selector는 7장에서 다뤘다. 여기서는 레이블 자체의 규칙과 동작을 중심으로 적었다.

```bash
k() { kubectl --context docker-desktop -n t9 "$@"; }
```

#### 목차

1. 한눈에 보는 차이
2. 레이블
3. 셀렉터(레이블 쿼리)
4. 레이블이 만드는 소유 관계 (컨트롤러와 Service)
5. 애너테이션
6. 레이블과 애너테이션을 Pod 안에서 읽기
7. 자주 쓰는 표준 레이블/애너테이션
8. 선택 가이드와 함정
9. 확인하지 못한 부분

---

#### 1. 한눈에 보는 차이

| 구분 | 레이블 | 애너테이션 |
|---|---|---|
| 목적 | 오브젝트를 **식별하고 선택**(그룹화, 필터링) | 오브젝트에 **메타데이터를 덧붙임** (도구/사람이 읽는 정보) |
| 셀렉터로 선택 | **가능** (`-l`, Service/Deployment selector 등) | **불가능** |
| 값 제약 | 값 63자 이하, 문자 제한이 엄격 | 거의 자유(문자열, JSON 가능), 전체 크기 제한 |
| 대표 사용처 | `app=web`, `env=prod`, `tier=front` | 빌드 정보, 담당자, 도구 설정(`rewrite-target` 등), 변경 사유 |
| 컨트롤러 동작에 영향 | **영향** (ReplicaSet 소유, Service 엔드포인트) | 직접 영향 없음 (도구가 읽어서 동작을 바꿀 수는 있음) |

---

#### 2. 레이블

**오브젝트에 붙이는 key=value 쌍.** 같은 종류의 오브젝트를 임의의 기준으로 묶기 위한 것이고, 하나의 오브젝트에 여러 개를 붙일 수 있다.

##### 2-1. 붙이기/수정/삭제

```
$ k run a1 --image=nginx:alpine --labels=app=web,env=prod,tier=front
$ k label pod a4 env=prod            → labeled
$ k label pod a4 env=dev             → error: 'env' already has a value (prod), and --overwrite is false
$ k label pod a4 env=dev --overwrite → labeled
$ k label pod a4 env-                → unlabeled   (키 뒤에 '-' 로 삭제)
```

- 이미 있는 키를 바꾸려면 `--overwrite`가 필요하다. 안 주면 거부한다.
- 키 뒤에 `-`를 붙이면 삭제한다.
- `k run`은 레이블을 따로 안 주면 `run=<이름>`을 자동으로 붙였다(`a4`는 `run=a4`).
- 레이블 변경은 Pod 재생성 없이 즉시 반영된다.

##### 2-2. 키/값 규칙 (실측)

| 시도 | 결과 |
|---|---|
| `a/b/c=x` | 거부 — 키는 `[접두사/]이름` 형태이고 `/`는 하나만 |
| `k=-bad` | 거부 — 값은 영숫자로 시작하고 끝나야 함 |
| `k=has space` | 거부 — 공백 불가 |
| 값이 64바이트 | 거부 — `must be no more than 63 bytes` |
| 키 이름 부분이 64자 | 거부 — 이름 부분은 63자 이하 |
| `Example.com/k=v` | 거부 — 접두사는 소문자 RFC 1123 서브도메인 |
| `app.kubernetes.io/name=ok` | 통과 |
| `k=` (빈 값) | **통과** (값은 빈 문자열 가능) |
| `kubernetes.io/x=1` | **통과** (Pod에서는 예약 접두사도 막지 않았다) |

정리: 키는 `선택적 접두사(DNS 서브도메인, 253자 이하) + / + 이름(63자 이하)`, 값은 63자 이하이고 비어도 된다. 사용자 정의 키에는 `kubernetes.io/`, `k8s.io/` 접두사를 쓰지 않는 것이 관례다(Pod에서는 막히지 않았지만, 예약 접두사라 시스템 레이블과 충돌할 수 있다).

##### 2-3. 컨트롤러가 붙이는 레이블 (6장 복습)

Deployment가 만든 Pod와 ReplicaSet에는 `pod-template-hash=65bccb98cc`가 자동으로 붙었다. 네임스페이스도 `kubernetes.io/metadata.name=t9` 레이블이 자동으로 붙는다. 노드는 `kubernetes.io/hostname=docker-desktop`, `kubernetes.io/arch=arm64`, `kubernetes.io/os=linux`, `node-role.kubernetes.io/control-plane=` 같은 시스템 레이블을 가진다.

##### 2-4. 노드 레이블로 스케줄링 (`nodeSelector`)

```
$ kubectl label node docker-desktop disk=ssd
# Pod A: nodeSelector disk=ssd → Running
# Pod B: nodeSelector disk=hdd → Pending
  Warning  FailedScheduling  default-scheduler  0/1 nodes are available:
  1 node(s) didn't match Pod's node affinity/selector. ...
```

노드에 붙인 레이블과 Pod의 `nodeSelector`가 일치하는 노드에만 배치된다. 일치하는 노드가 없으면 Pending이다. (스케줄링 심화는 12장.)

---

#### 3. 셀렉터(레이블 쿼리)

##### 3-1. 등호 기반과 집합 기반 (실측)

대상: `a1`(app=web, env=prod, tier=front), `a2`(app=web, env=dev, tier=front), `a3`(app=db, env=prod), `a4`(레이블 `run=a4`만)

| 셀렉터 | 결과 |
|---|---|
| `app=web` | a1 a2 |
| `app!=web` | a3 a4 (**키가 없는 a4도 포함**) |
| `env in (prod,dev)` | a1 a2 a3 |
| `env notin (prod)` | a2 a4 (**env 키가 없는 a4도 포함**) |
| `tier` | a1 a2 (키가 존재하는 것) |
| `!tier` | a3 a4 (키가 없는 것) |
| `app=web,env=prod` | a1 (쉼표는 AND) |
| `app=web,!tier` | (없음) |

- 쉼표는 **AND**다. OR은 `in (a,b)`로만 표현한다.
- `!=`와 `notin`은 키가 없는 오브젝트도 포함한다. "`env`가 `prod`가 아닌 것"에 env 레이블이 없는 것도 들어간다.
- `-L <키>`로 열을 추가해 값을 비교할 수 있다(`k get pod -L track`).

##### 3-2. 오브젝트 selector 필드

- 등호 기반만 지원하는 리소스(Service의 `spec.selector`)는 `key: value`의 맵이다.
- `matchLabels` / `matchExpressions`를 지원하는 리소스(Deployment, ReplicaSet, Job 등)는 연산자 `In`, `NotIn`, `Exists`, `DoesNotExist`를 쓴다. 이 실험의 Deployment는 `app In [web]` AND `track NotIn [debug]`로 만들었다.

##### 3-3. 필드 셀렉터는 레이블이 아니다

`--field-selector status.phase=Running`처럼 필드 값으로 거르는 방식은 별개 기능이다. 이 실험에서는 Pod가 아직 생성 중이어서 0건이 나왔고, 의미 있는 비교는 하지 못했다.

---

#### 4. 레이블이 만드는 소유 관계 (컨트롤러와 Service)

레이블은 "표시"에 그치지 않고 **컨트롤러와 Service의 동작을 결정**한다. Deployment(replicas 3, selector `app In [web]` AND `track NotIn [debug]`)와 Service(`selector app=web`)로 확인했다.

##### 4-1. 레이블을 떼면 Pod가 컨트롤러에서 분리된다

```
$ k label pod web-65bccb98cc-7pfvs app-     # 레이블 app 삭제
web-65bccb98cc-7pfvs   Running  0  7s  pod-template-hash=65bccb98cc      ← 남은 레이블
web-65bccb98cc-b7pkt   Running  0  3s  app=web,...                        ← ReplicaSet이 새로 만든 Pod
ownerReferences of detached pod: (비어 있음)
Service 엔드포인트: web-65bccb98cc-nrrh5  web-65bccb98cc-xl9hq  web-65bccb98cc-b7pkt   (분리된 Pod 제외)
```

- 레이블을 지운 Pod는 ReplicaSet의 selector에 안 맞아서 **`ownerReferences`가 사라지고** 고아 Pod가 됐다.
- ReplicaSet은 3개를 맞추려고 **새 Pod를 하나 더 만들었다**.
- 분리된 Pod는 **Service 엔드포인트에서도 빠졌다**. 장애 Pod를 서비스에서 빼고 디버깅하는 데 쓸 수 있는 방법이다(Pod는 죽지 않고 남는다).

##### 4-2. 레이블을 되돌리면 다시 입양한다

```
$ k label pod web-65bccb98cc-7pfvs app=web
→ 3초 뒤 Pod 3개 (b7pkt 삭제됨), 7pfvs의 ownerReferences 가 다시 ReplicaSet web-65bccb98cc
```

selector에 다시 맞는 고아 Pod는 ReplicaSet이 **다시 소유**했고, 늘어난 개수는 줄이기 위해 새로 만들었던 Pod를 지웠다.

##### 4-3. `matchExpressions`도 동일하게 동작

`track=debug` 레이블을 붙이자 `NotIn [debug]`에 걸려 그 Pod가 분리됐고 새 Pod가 만들어졌다(`k get pod -L track`에 debug 1개 + 나머지 3개).

##### 4-4. 정리

| 상황 | 결과 |
|---|---|
| selector에 안 맞게 레이블 변경 | 컨트롤러에서 분리, Service에서 제외, 컨트롤러는 새 Pod 생성 |
| 다시 맞게 변경 | 고아 Pod를 다시 소유 (여분 Pod 삭제) |
| 컨트롤러 selector 변경 | Deployment selector는 불변(6장) |

---

#### 5. 애너테이션

**레이블과 달리 선택 용도가 아닌, 도구/사람을 위한 부가 정보.** 키 규칙은 레이블과 같고(접두사 + 이름), 값은 길이/문자 제한이 사실상 없다.

##### 5-1. 값은 자유 형식 (JSON 가능)

```
$ k annotate pod a4 note='hello world' 'example.com/json={"a":1}'
{"example.com/json":"{\"a\":1}","note":"hello world"}
```

- 공백과 JSON 문자열을 값으로 쓸 수 있다(레이블은 불가).

##### 5-2. 크기 제한

```
$ k annotate pod a4 big=<262145바이트>
The Pod "a4" is invalid: metadata.annotations: Too long: may not be more than 262144 bytes
$ k annotate pod a4 mid=<200000바이트>   → annotated (통과)
```

- 한 오브젝트의 **애너테이션 전체 합이 256KiB(262144바이트)를 넘으면** 거부된다. 한 값이 아니라 합계 기준이다.
- 큰 데이터를 애너테이션에 넣지 않는다(오브젝트 크기를 키워 etcd, watch 트래픽에 부담이 된다).

##### 5-3. 시스템/도구가 쓰는 애너테이션 (실측으로 확인한 것)

| 애너테이션 | 누가 | 의미 |
|---|---|---|
| `kubectl.kubernetes.io/last-applied-configuration` | `kubectl apply` | 마지막으로 적용한 매니페스트 전체(JSON). 3-way merge의 기준 |
| `deployment.kubernetes.io/revision` | Deployment 컨트롤러 | 리비전 번호 (Deployment, ReplicaSet 모두) |
| `kubernetes.io/change-cause` | 사람/도구 | `rollout history`의 CHANGE-CAUSE 열 |
| `kubectl.kubernetes.io/restartedAt` | `kubectl rollout restart` | Pod 템플릿에 시각을 넣어 롤아웃을 유발 |
| `nginx.ingress.kubernetes.io/rewrite-target` | 사용자(→ ingress-nginx) | 8장에서 확인한 컨트롤러별 설정 |

```
$ k annotate deploy/web kubernetes.io/change-cause="upgrade 1.27" --overwrite
$ k rollout history deploy/web
REVISION  CHANGE-CAUSE
1         initial
2         upgrade 1.27

$ k rollout restart deploy/web
{"kubectl.kubernetes.io/restartedAt":"2026-10-25T22:43:37+09:00"}     ← spec.template.metadata.annotations
```

- `restartedAt`은 **Pod 템플릿의 애너테이션**이라서 템플릿이 바뀌었다고 판단해 새 ReplicaSet으로 롤아웃한다. 애너테이션이 컨트롤러 동작을 간접적으로 바꾸는 사례다.
- `change-cause`는 오브젝트에 쓴 애너테이션이 `rollout history`에 노출되는 구조다. 이미 만들어진 ReplicaSet에 복사된 값이다(리비전 1 RS의 `change-cause`가 `initial`).

##### 5-4. 애너테이션은 보안 경계가 아니다

`kubectl apply`로 만든 오브젝트에는 `last-applied-configuration`에 **매니페스트 전체가 평문으로** 들어간다. 11장 Secret에서 `stringData`로 apply하면 평문이 이 애너테이션에 남는 것을 확인했다. 민감 정보를 애너테이션에 넣지 않는다.

---

#### 6. 레이블과 애너테이션을 Pod 안에서 읽기

환경변수와 downwardAPI 볼륨으로 Pod 자신의 메타데이터를 읽을 수 있다 (5장 복습).

```
$ k label pod dw ver=v2 --overwrite ; k annotate pod dw owner=team-b --overwrite
(약 70초 뒤)
env OWNER = team-a                    ← 환경변수: 안 바뀜
/etc/m/labels:  app="dw"  ver="v2"    ← downwardAPI 볼륨: 바뀜
/etc/m/ann:     ... owner="team-b"    ← 바뀜 (kubernetes.io/config.* 등 시스템 애너테이션도 함께 노출)
```

- 환경변수는 컨테이너 시작 시점의 값이고 이후 변경은 반영되지 않는다.
- 볼륨 파일은 kubelet이 주기적으로 갱신한다. 이 실험에서는 70초 안에 반영됐다(정확한 반영 시간은 측정하지 않음).
- 이 방식으로 Pod 안의 앱이 자기 레이블/애너테이션을 읽을 수 있다.

---

#### 7. 자주 쓰는 표준 레이블/애너테이션

- 권장 레이블(공식 문서의 `app.kubernetes.io/*`): `name`, `instance`, `version`, `component`, `part-of`, `managed-by`. 위 2-2에서 `app.kubernetes.io/name=ok`는 통과했다. 도구(대시보드, Helm 등)가 이 키로 앱을 묶는다.
- 쿠버네티스가 관리하는 레이블은 `kubernetes.io/`, `k8s.io/` 접두사를 쓴다(노드/네임스페이스 레이블 위 2-3).
- 팀 내부 규칙: 접두사를 붙여(`example.com/team`) 충돌을 피하고, 키 목록(최소 `app`, `env`, `owner` 등)을 문서로 정해 둔다.

(권장 레이블 목록의 개별 키 설명은 공식 문서 지식이며 이번 실험에서 각 키를 검증한 것은 아니다.)

---

#### 8. 선택 가이드와 함정

| 질문 | 답 |
|---|---|
| 선택/그룹화에 쓸 값 | 레이블. 선택에 안 쓰고 정보만 남기면 애너테이션 |
| 레이블 값에 URL이나 JSON | 불가. 애너테이션으로 |
| `!=`, `notin` 결과에 레이블 없는 것이 섞임 | 키가 없는 오브젝트도 포함. 필요하면 `env,env!=prod`처럼 존재 조건을 함께 건다 |
| Pod를 서비스에서 빼고 보존해서 디버깅 | 셀렉터에 쓰이는 레이블을 제거(분리 + 새 Pod 생성). Pod는 남는다 |
| 레이블을 잘못 바꿨다 | ReplicaSet이 고아 Pod를 다시 입양할 수도, 새 Pod를 늘릴 수도 있다. 소유 관계는 `ownerReferences`로 확인 |
| 애너테이션에 큰 데이터 | 합계 256KiB 제한. 쓰지 않는다 |
| 민감 정보 | 애너테이션/레이블에 넣지 않는다(`last-applied` 포함) |
| 컨트롤러별 어노테이션 | 다른 컨트롤러에서는 무시된다(8장) |

---

#### 9. 확인하지 못한 부분

- 레이블과 애너테이션이 **etcd/API 서버 성능에 주는 영향**(레이블 인덱싱, 대규모 클러스터의 `-l` 조회 비용)은 측정하지 않았다.
- `app.kubernetes.io/*` 권장 레이블의 **개별 키 의미**는 공식 문서 지식이고 실험으로 검증하지 않았다.
- 필드 셀렉터는 Pod가 생성 중일 때 한 번 보았을 뿐이다(0건).
- 예약 접두사(`kubernetes.io/`)를 Pod 레이블에 붙이는 것이 통과했지만, 다른 리소스 종류나 어드미션 정책(예: 정책 엔진) 환경에서도 같은지는 확인하지 못했다.
- 애너테이션의 **Pod 안 반영 지연 시간**(downwardAPI)은 70초 이내라는 것만 확인했다.
- `kubectl label --all`, `-l` 로 대량 변경/삭제의 동작은 `delete -l`로 Pod 3개를 지운 것 외에는 다루지 않았다.
- 네임스페이스 레이블을 기준으로 하는 NetworkPolicy, 리소스 쿼터 스코프, PodDisruptionBudget 등 **다른 리소스가 레이블을 쓰는 방식**은 실험하지 않았다.
