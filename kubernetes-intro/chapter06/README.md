### 6장. 컨트롤러

> 정리 범위: ReplicaSet, Deployment, DaemonSet, StatefulSet, Job, CronJob 개념 정리
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드 `docker-desktop`), kubectl v1.35.2
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> **다른 책과의 관계**: 이 장의 여섯 컨트롤러는 `시작하세요! 도커/쿠버네티스`에서 이미 실측으로 정리했다. 그래서 이 노트는 **개념 비교와 선택 기준**을 중심으로 정리하고, 그 책의 실측 결과를 요약해 링크했다 (ReplicaSet/Deployment/Service [6장](../../start-docker-kubernetes/chapter06/README.md), DaemonSet/StatefulSet/Job/CronJob [13장](../../start-docker-kubernetes/chapter13/README.md)). 이번에는 **Deployment의 배포 옵션**(maxSurge/maxUnavailable, revisionHistoryLimit, minReadySeconds, progressDeadlineSeconds, pause/resume)을 새로 실험해서 보강했다. 표에서 **"(도커 책 N장)"** 이라고 쓴 결과는 그 책에서 실측한 것이고, 이번에 새로 돌린 것은 "(이번 실측)"이라고 표시했다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`을 명시했다 (아래에서는 `k`로 줄여 적음). 실험은 `t6k` 네임스페이스에서 했고 끝나고 삭제했다.

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. 컨트롤러란: 한눈에 비교
2. ReplicaSet
3. Deployment
4. DaemonSet
5. StatefulSet
6. Job
7. CronJob
8. 무엇을 쓸까: 선택 기준
9. 확인하지 못한 부분

---

#### 1. 컨트롤러란: 한눈에 비교

**컨트롤러**는 "원하는 상태(`spec`)와 실제 상태를 비교해 맞추는 제어 루프"다 (도커 책 6장). 이 장의 컨트롤러는 모두 **Pod를 만들고 관리**하는 컨트롤러이고, 각자 "Pod를 어떻게 유지할지"가 다르다.

| 컨트롤러 | 한 줄 정의 | Pod 개수 결정 | Pod 수명 | 대표 용도 |
|---|---|---|---|---|
| **ReplicaSet** | 같은 Pod를 **N개 유지** | `replicas` | 계속 실행 | (직접 쓰지 않고 Deployment가 사용) |
| **Deployment** | ReplicaSet을 관리하며 **무중단 배포/롤백** | `replicas` | 계속 실행 | 웹/API 같은 **무상태** 앱 |
| **DaemonSet** | **노드마다 Pod 하나** | 노드 수 | 계속 실행 | 로그/모니터링 에이전트, CNI, kube-proxy |
| **StatefulSet** | **고유한 이름·저장소**를 가진 Pod N개 (순서 있음) | `replicas` | 계속 실행 | DB, 브로커 등 **상태 저장** 앱 |
| **Job** | Pod가 **성공 종료**할 때까지 실행 | `completions` | **종료됨** | 일회성 배치, 마이그레이션 |
| **CronJob** | **일정에 맞춰** Job 생성 | (Job이 결정) | 주기적으로 종료됨 | 정기 백업, 리포트 |

**소유 관계**

```
Deployment ──owns──▶ ReplicaSet ──owns──▶ Pod
CronJob    ──owns──▶ Job        ──owns──▶ Pod
DaemonSet  ──owns──▶ Pod
StatefulSet──owns──▶ Pod (+ PVC는 volumeClaimTemplates로 생성)
```

```
$ k -n t6k get rs -l app=hist -o jsonpath='{.items[0].metadata.ownerReferences[0].kind}/{...name} controller={...controller}'
Deployment/hist controller=true                                  ← ReplicaSet의 소유자는 Deployment (이번 실측)
```

- 모든 컨트롤러가 **라벨 셀렉터**로 자기 Pod를 찾고 `ownerReferences`로 소유한다. 소유자를 지우면 가비지 컬렉터가 **연쇄 삭제**한다 (`--cascade=orphan`이면 남김).
- 컨트롤러가 없는 Pod(`kubectl run`)는 죽으면 되살아나지 않는다 (5장).

#### 2. ReplicaSet

**지정한 개수의 같은 Pod가 항상 실행되도록 유지하는 컨트롤러.**

| 필드 | 의미 |
|---|---|
| `replicas` | 유지할 Pod 수 |
| `selector` | **이 라벨의 Pod를 센다** (생성 후 변경 불가) |
| `template` | 부족할 때 만들 Pod의 정의 (`template.metadata.labels`가 `selector`와 맞아야 함) |

| 동작 | 결과 (도커 책 6장 실측) |
|---|---|
| Pod 삭제 | **새 Pod가 자동 생성**되어 개수 유지 (자가 치유) |
| Pod의 라벨을 셀렉터에서 벗어나게 변경 | 그 Pod는 **관리에서 빠지고**(`ownerReferences` 제거) 부족해진 만큼 새로 생성 |
| 라벨이 맞는 **기존 Pod가 이미 있으면** | 소유자가 없는 Pod는 **입양**(새로 만들지 않고 셈에 포함). 이미 다른 컨트롤러가 소유한 Pod는 입양하지 않음 |
| `selector`와 `template` 라벨 불일치 | API가 거부 |
| `selector` 수정 | `field is immutable`로 거부 (이번 실측: Deployment의 selector 수정도 같은 오류) |
| Deployment가 소유한 RS의 `replicas`를 직접 수정 | 소유자(Deployment)가 **원래 값으로 되돌림** |

```
$ k -n t6k patch deploy hist --type=merge -p '{"spec":{"selector":{"matchLabels":{"app":"other"}}}}'
The Deployment "hist" is invalid:
* spec.template.metadata.labels: Invalid value: map[string]string{"app":"hist"}: `selector` does not match template `labels`
* spec.selector: Invalid value: ...: field is immutable                                       ← (이번 실측)
```

- ReplicaSet은 **롤링 업데이트, 롤백 기능이 없다.** 템플릿을 바꿔도 기존 Pod는 그대로이고 새로 만들어질 때만 반영된다. 이 기능이 필요하면 Deployment를 쓴다. 그래서 **ReplicaSet을 직접 만드는 일은 거의 없다.**

#### 3. Deployment

**ReplicaSet을 만들고 관리해서 "버전 교체(롤링 업데이트), 롤백, 일시 정지"를 제공하는 컨트롤러.** Pod 템플릿이 바뀌면 **새 ReplicaSet을 만들고 새 RS는 늘리고 옛 RS는 줄이는 식**으로 교체한다 (도커 책 6장: 이벤트와 RS 변화 로그).

- ReplicaSet 이름의 해시(`hist-5948d57d47`)는 **Pod 템플릿의 해시**이고, Pod에 `pod-template-hash` 라벨로 붙어서 **버전별 RS의 셀렉터가 겹치지 않게** 한다.
- **Pod 템플릿(`spec.template`)이 바뀔 때만** 새 롤아웃이 일어난다. `replicas`만 바꾸는 것은 롤아웃이 아니다.

##### 3-1. 배포 전략

| 전략 | 동작 | 용도 |
|---|---|---|
| **RollingUpdate** (기본) | 새 Pod를 점진적으로 늘리고 옛 Pod를 줄임 (무중단) | 대부분의 웹/API |
| **Recreate** | **옛 Pod를 전부 종료한 뒤** 새 Pod 생성 (다운타임 발생) | 두 버전이 동시에 떠 있으면 안 되는 경우(스키마/락) |

##### 3-2. `maxSurge`와 `maxUnavailable` (이번 실측)

`replicas: 4`, readiness probe(시작 후 4초)를 가진 Deployment의 템플릿을 바꾸고 ReplicaSet별 `desired / current / ready`를 시간순으로 기록했다.

```
# ① maxSurge: 2, maxUnavailable: 0
 0s  6699[des=4 cur=4 rdy=4]  88fb[des=2 cur=2 rdy=0]          ← 옛 4개를 그대로 두고 새 Pod 2개를 먼저 추가 (총 6개)
 6s  6699[des=2 cur=2 rdy=2]  88fb[des=4 cur=4 rdy=2]          ← 새 Pod가 Ready가 되자 옛 Pod를 줄이고 새 Pod를 늘림

# ② maxSurge: 0, maxUnavailable: 2
 1s  84c9[des=2 cur=2 rdy=2]  d648[des=2 cur=2 rdy=0]          ← 옛 Pod를 먼저 2개로 줄이고(총 4개 유지) 새 Pod 2개 생성: 준비된 Pod는 2개뿐
 7s  d648[des=4 cur=4 rdy=2]
11s  d648[des=4 cur=4 rdy=4]

# ③ Recreate
 4s  5454[des=4 cur=4 rdy=0]                                    ← 옛 RS가 0이 된 뒤 새 RS의 4개가 Ready 되기를 기다리는 상태
```

| 옵션 | 의미 | 효과 |
|---|---|---|
| `maxSurge` | 원하는 개수보다 **초과해서** 만들 수 있는 Pod 수 (개수 또는 %) | 크면 빠르지만 자원을 더 쓴다. 0이면 초과 없이 옛 Pod를 먼저 줄인다 |
| `maxUnavailable` | 롤아웃 중 **사용 불가**일 수 있는 Pod 수 (개수 또는 %) | 0이면 항상 원하는 수만큼 Ready를 유지 (느림, 자원 필요). 크면 빠르지만 순간 용량이 줄어든다 |

- 둘 다 0일 수는 없다 (진행이 불가능). 기본값은 둘 다 25%다 (도커 책 6장).
- ① 조합(`surge 2, unavailable 0`)은 **용량 저하 없이** 교체하지만 순간적으로 6개가 필요하다. ② 조합은 **추가 자원 없이** 교체하지만 순간 용량이 절반(2/4)이 된다. 노드 자원이 빠듯하면 ②, 무중단이 중요하면 ①을 쓴다.
- ③ Recreate는 첫 표본이 4초 뒤여서 "옛 Pod가 전부 사라진 뒤 새 Pod가 만들어진다"는 순서를 위 한 줄로 단정할 수는 없다. 옛 RS가 먼저 `0`으로 내려가고 새 RS가 올라간 것은 보였다 (정확한 중간 상태는 샘플링 간격 때문에 놓쳤다).

##### 3-3. 이력과 롤백: `revisionHistoryLimit` (이번 실측)

```
$ (revisionHistoryLimit: 2) 템플릿을 5번 변경 (v2~v6)
$ k -n t6k get rs -l app=hist
hist-5948d57d47  desired=0
hist-5db5787c7b  desired=0
hist-7b6dd7bd7d  desired=2                       ← 현재 RS 1개 + 옛 RS 2개만 남음 (더 오래된 RS는 삭제됨)
$ k -n t6k rollout history deploy/hist
4  <none>
5  <none>
6  <none>                                         ← 리비전 1~3은 정리됨
```

- `revisionHistoryLimit`(기본 10)은 **롤백용으로 남겨 둘 옛 ReplicaSet의 개수**다. 5번 바꿨더니 2개만 남아 **리비전 4~6만 롤백 대상**이 되었다. 너무 작게 잡으면 롤백할 버전이 없어진다 (0이면 롤백 불가). 도커 책 6장에서 `rollout undo`가 옛 RS를 재사용하는 것을 확인했다.

##### 3-4. 가용성 판정: `minReadySeconds` (이번 실측)

```
$ (minReadySeconds: 10, replicas 2)
 2s  ready=2  available=                         ← Pod는 Ready지만 아직 Available이 아님
12s  ready=2  available=2                        ← Ready가 된 뒤 10초를 버텨야 Available
```

- `minReadySeconds`는 **Ready가 된 Pod가 그 시간 동안 문제없이 유지되어야 "사용 가능(available)"으로 센다.** 롤링 업데이트는 `available` 기준으로 진행하므로 **새 Pod가 Ready 직후 곧바로 죽는 경우(초기 크래시)** 를 걸러서 롤아웃이 다음 단계로 넘어가지 않게 해 준다. 단점은 그만큼 롤아웃이 느려지는 것이다.

##### 3-5. 롤아웃 정체: `progressDeadlineSeconds` (이번 실측)

```
$ (progressDeadlineSeconds: 20) 이미지를 없는 태그로 변경 → 28초 뒤
Available=True(MinimumReplicasAvailable)  Progressing=False(ProgressDeadlineExceeded)
$ k -n t6k rollout status deploy/dead
error: deployment "dead" exceeded its progress deadline
$ k -n t6k get deploy dead      →  READY=2/2  UP-TO-DATE=1  AVAILABLE=2
dead-5f9cc54c77-hbwxm Running      dead-5f9cc54c77-nnqg8 Running      dead-95c5b4f48-hn596 ImagePullBackOff
$ k -n t6k get deploy dead -o jsonpath='image={.spec.template.spec.containers[0].image}'
image=alpine:no-such-tag                           ← 롤백되지 않음
```

- 지정한 시간 안에 롤아웃이 진행되지 않으면 `Progressing=False (ProgressDeadlineExceeded)` condition이 붙고 `rollout status`가 오류로 끝난다 (**CI/CD에서 배포 실패를 감지하는 신호**). 기본값은 600초다.
- **자동 롤백은 하지 않는다.** 옛 Pod 2개가 계속 서비스하고(`Available=True`), `spec`은 잘못된 이미지로 남는다. 롤백은 `kubectl rollout undo`로 직접 해야 한다 (자동 롤백이 필요하면 Argo Rollouts 같은 도구를 쓴다는 것은 문서 지식).

##### 3-6. 일시 정지/재개로 변경을 묶기 (이번 실측)

```
$ k -n t6k rollout pause deploy/hist                        →  deployment.apps/hist paused  (spec.paused=true)
$ k -n t6k set env deploy/hist V=paused1 ; k -n t6k set image deploy/hist c=alpine:3.19 ; k -n t6k set resources deploy/hist -c c --requests=cpu=10m
(5초 뒤)  Pod 이미지: alpine:3.20, alpine:3.20                ← 3개를 바꿨지만 Pod는 그대로 (롤아웃이 시작되지 않음)
$ k -n t6k rollout status deploy/hist --timeout=3s          →  error: timed out waiting for the condition
$ k -n t6k rollout resume deploy/hist ; k -n t6k rollout status deploy/hist
deployment "hist" successfully rolled out                    →  Pod 이미지가 alpine:3.19 로 교체됨
```

- `pause` 동안 템플릿을 여러 번 바꿔도 **롤아웃이 일어나지 않고**, `resume`하면 **한 번에 하나의 롤아웃**으로 반영된다. 변경 3개를 묶었을 때 리비전이 하나만 늘어나는지는 위 3-3절의 `revisionHistoryLimit` 때문에 리비전 번호로 비교하지 못했다. 카나리처럼 일부만 먼저 내보내는 용도로 쓰기도 한다 (이 용도는 시험하지 않았다).
- `pause` 동안 RS 개수가 표시상 3개에서 2개로 줄어든 것을 봤는데 정확한 이유(이력 정리 시점으로 추정)는 확인하지 않았다.

##### 3-7. Deployment 요약 명령

| 목적 | 명령 |
|---|---|
| 배포 | `kubectl apply -f`, `kubectl set image deploy/<이름> <컨테이너>=<이미지>` |
| 진행 확인 | `kubectl rollout status deploy/<이름>` |
| 이력/롤백 | `kubectl rollout history`, `kubectl rollout undo [--to-revision=N]` |
| 재시작 (설정 변경 없이) | `kubectl rollout restart deploy/<이름>` (3장) |
| 일시 정지/재개 | `kubectl rollout pause|resume deploy/<이름>` |
| 확장 | `kubectl scale deploy/<이름> --replicas=N` |

#### 4. DaemonSet

**조건에 맞는 모든 노드에 Pod를 하나씩 실행하는 컨트롤러.** 노드가 추가되면 거기에도 만들고, 노드가 사라지면 정리한다 (`replicas` 필드가 없다).

| 특징 | 내용 (도커 책 13장 실측) |
|---|---|
| 개수 | **노드 수**(+ `nodeSelector`/affinity 조건). 조건에 맞는 노드가 없으면 desired 0 |
| 노드 라벨이 바뀌면 | 라벨이 붙으면 Pod 생성, 제거되면 삭제 (5초 안에) |
| 스케줄 | 기본 스케줄러가 스케줄하되, 컨트롤러가 Pod마다 `metadata.name` **노드 어피니티**를 주입해 노드를 지정 |
| 톨러레이션 자동 부여 | `not-ready`, `unreachable`, `disk/memory/pid-pressure`, `unschedulable` (노드가 나빠도 에이전트는 계속 돌아야 하므로) |
| 업데이트 | `RollingUpdate`(기본, `maxUnavailable: 1`, `maxSurge: 0`) / `OnDelete`(Pod를 직접 삭제할 때만 교체) |
| 리비전 | `ControllerRevision` 오브젝트 (`rollout history/undo` 가능). ReplicaSet은 없다 |

- 대표 용도: 노드의 로그/메트릭 수집(Fluent Bit, node-exporter), 네트워크(CNI, kube-proxy), 보안 에이전트 (4장: kube-proxy가 DaemonSet).
- 한계: 이 환경은 노드가 하나라 **노드별 분산 배치 모습**은 확인하지 못했다.

#### 5. StatefulSet

**상태 저장 앱을 위해 Pod에 안정적인 정체성(이름, 네트워크 ID, 저장소)을 주는 컨트롤러.**

| 보장 | 내용 (도커 책 13장 실측) |
|---|---|
| **고정된 이름** | `db-0`, `db-1`, `db-2` (재생성되어도 같은 이름. IP는 바뀜) |
| **고정된 DNS** | 헤드리스 Service(`clusterIP: None`)와 함께: `db.<ns>.svc` → 전체 Pod IP, `db-1.db.<ns>.svc` → 개별 Pod |
| **Pod마다 전용 PVC** | `volumeClaimTemplates` → `data-db-0`, `data-db-1`... 재생성되어도 **같은 PVC에 다시 붙음** |
| **순서 있는 생성** | `db-0`이 Ready여야 `db-1` 생성 (`podManagementPolicy: OrderedReady`, `Parallel`이면 동시) |
| **역순 스케일 인/업데이트** | 큰 순번부터 하나씩 삭제/교체 |
| **업데이트 제어** | `RollingUpdate` + `partition: N`(순번 N 이상만 업데이트: 카나리), `OnDelete` |
| **PVC 보존 정책** | 기본 `Retain`(스케일 인/삭제해도 PVC 유지), `persistentVolumeClaimRetentionPolicy`로 `Delete` 가능 |

- Deployment와 가장 큰 차이: **Pod가 교체 가능한 동일 복제본이 아니라 "개별 정체성을 가진 구성원"** 이다. DB 클러스터에서 "0번이 프라이머리, 1·2번이 복제본"처럼 구성원을 구별해야 하는 앱에 쓴다.
- **StatefulSet은 앱의 복제/클러스터링을 해 주지 않는다.** 이름, 저장소, 순서만 제공하고 DB 복제 구성은 앱(또는 오퍼레이터, 도커 책 12장)의 몫이다.
- 변경 불가 필드: `serviceName`, `selector`, `volumeClaimTemplates` 등 (`replicas`, `template`, `updateStrategy`, `persistentVolumeClaimRetentionPolicy`, `minReadySeconds`, `ordinals`만 수정 가능).

#### 6. Job

**Pod를 하나 이상 실행해서 지정한 횟수만큼 성공 종료할 때까지 보장하는 컨트롤러.** Deployment는 종료된 Pod를 다시 띄우지만 Job은 **성공으로 끝나면 더 이상 띄우지 않는다.**

| 필드/개념 | 의미 (도커 책 13장 실측) |
|---|---|
| `completions` | 성공해야 하는 Pod 수 (기본 1) |
| `parallelism` | 동시에 실행할 Pod 수 (`completions: 6, parallelism: 2`: 2개씩 6번, 28초) |
| `restartPolicy` | **`Never` 또는 `OnFailure`만** 가능. `Never`는 실패 시 **새 Pod**를 만들고(실패 Pod가 남아 로그 확인 가능), `OnFailure`는 **같은 Pod에서 컨테이너를 재시작**(한도 도달 후 Pod 삭제) |
| `backoffLimit` | 재시도 한도 (기본 6). 초과하면 `BackoffLimitExceeded`로 실패. 재시도 간격은 지수적으로 증가 |
| `activeDeadlineSeconds` | Job 전체 최대 실행 시간. 초과하면 `DeadlineExceeded`로 실패하고 **실행 중이던 Pod도 종료** |
| `ttlSecondsAfterFinished` | 완료 후 지정한 시간이 지나면 Job과 Pod 자동 삭제 |
| `completionMode: Indexed` | Pod마다 `JOB_COMPLETION_INDEX`(0부터), 호스트명 `<job>-N` (작업 분할 처리) |
| `suspend` | `true`로 만들면 Pod를 만들지 않고 대기, `false`로 바꾸면 실행 |

- Job의 **Pod 템플릿은 만든 뒤 바꿀 수 없다** (`field is immutable`). 같은 이름의 Job을 `apply`해도 이미 끝난 Job은 다시 실행되지 않는다 (`unchanged`). 다시 실행하려면 삭제하고 새로 만든다.
- 완료된 Pod는 `Completed`로 **남는다** (로그 확인용). 정리는 `ttlSecondsAfterFinished` 또는 직접 삭제.
- **사이드카가 있는 Job**: 일반 사이드카(`containers`에 나란히)는 앱이 끝나도 안 끝나서 **Job이 영원히 끝나지 않는다.** native sidecar(init container + `restartPolicy: Always`)를 쓰면 메인이 끝날 때 사이드카가 종료되어 Job이 `Complete`가 된다 (5장 3-6절 실측).

#### 7. CronJob

**cron 표현식의 일정에 따라 Job을 만드는 컨트롤러.**

| 필드 | 의미 (도커 책 13장 실측) |
|---|---|
| `schedule` | `분 시 일 월 요일` (잘못된 값은 API가 거부, 예: `61 * * * *`) |
| `timeZone` | 시간대(`Asia/Seoul` 등). 지정하지 않으면 **컨트롤러 매니저의 시간대(UTC)** |
| `concurrencyPolicy` | `Allow`(기본, 이전 Job이 안 끝나도 새로 만듦 → 중첩 실행), `Forbid`(이전 Job이 실행 중이면 건너뜀/지연), `Replace`(**이전 Job을 삭제하고 교체**: 작업이 주기보다 길면 영영 끝나지 못함) |
| `successfulJobsHistoryLimit` / `failedJobsHistoryLimit` | 남겨 둘 완료/실패 Job 개수 (기본 3 / 1). 초과분은 오래된 것부터 삭제 |
| `suspend` | `true`면 새 Job 생성을 멈춤 (이미 실행 중인 Job은 유지) |
| 수동 실행 | `kubectl create job manual --from=cronjob/<이름>` (어노테이션 `cronjob.kubernetes.io/instantiate: manual`) |

- 만들어지는 Job 이름은 **`<cronjob 이름>-<예정 시각의 분 단위 에포크>`**(`fast-29880860`)다. 같은 예정 시각의 중복 생성을 막는다.
- CronJob을 지우면 소유한 Job과 Pod도 삭제된다. 작업 시간이 주기보다 길어질 수 있는 일(백업)에는 보통 `Forbid`를 쓴다 (문서 지식. `Forbid`는 이전 Job이 끝난 뒤 밀린 일정이 늦게 시작되는 것을 도커 책에서 관찰).

#### 8. 무엇을 쓸까: 선택 기준

```
Pod가 계속 떠 있어야 하나?
├─ 예 ─ 노드마다 하나씩 필요한가?
│       ├─ 예 ─────────────────────────▶ DaemonSet
│       └─ 아니오 ─ Pod마다 고유한 이름/저장소/순서가 필요한가?
│                   ├─ 예 ─────────────▶ StatefulSet
│                   └─ 아니오 ─────────▶ Deployment  (내부적으로 ReplicaSet)
└─ 아니오 (끝나는 작업) ─ 정해진 일정에 반복?
                    ├─ 예 ─────────────▶ CronJob  (내부적으로 Job)
                    └─ 아니오 ─────────▶ Job
```

| 상황 | 선택 | 이유 |
|---|---|---|
| 웹/API 서버, 상태 없음 | **Deployment** | 롤링 업데이트, 롤백, 자유로운 확장 |
| DB, Kafka, ZooKeeper 같은 클러스터 | **StatefulSet** (보통 오퍼레이터로 감싸서) | 고정 이름/DNS/전용 PVC, 순서 |
| 로그 수집기, 모니터링 에이전트 | **DaemonSet** | 노드마다 하나, 노드 추가 시 자동 배치 |
| 한 번 실행하는 DB 마이그레이션 | **Job** | 성공 종료 보장, 재시도 |
| 매일 새벽 백업/리포트 | **CronJob** | 일정, 이력 관리, 중첩 정책 |
| 단일 Pod를 잠깐 띄워 확인 | `kubectl run` Pod (netshoot 등, 3장) | 컨트롤러가 필요 없는 임시 용도 |

**자주 하는 실수**

| 실수 | 올바른 방법 |
|---|---|
| ReplicaSet을 직접 만들어 롤링 업데이트를 기대 | Deployment 사용 |
| DB를 Deployment로 띄워 Pod마다 같은 PVC를 공유 | StatefulSet의 `volumeClaimTemplates` (Pod마다 PVC) |
| HPA를 쓰면서 YAML에 `replicas`를 계속 적고 `apply` | `replicas`를 YAML에서 빼야 함 (도커 책 12장: 필드를 빼면 기본값 1로 떨어짐) |
| CronJob에 `Allow`를 둔 채 작업 시간이 주기보다 김 | `Forbid`로 중첩 방지 |
| Job에 일반 사이드카를 둠 | native sidecar 또는 앱 종료 시 사이드카도 종료 |
| 롤아웃이 실패했는데 자동으로 되돌아갈 거라 기대 | `progressDeadlineSeconds` 초과는 알려 주기만 함, 직접 `rollout undo` |
| StatefulSet을 지운 뒤 PVC가 남은 것을 모름 | `persistentVolumeClaimRetentionPolicy`와 정리 절차 |

#### 9. 확인하지 못한 부분

- **이번 장의 새 실험은 Deployment의 배포 옵션뿐**이다. ReplicaSet, DaemonSet, StatefulSet, Job, CronJob의 동작은 도커 책 6장/13장의 실측을 요약해 옮겼고 이 노트를 쓰면서 다시 실행하지는 않았다.
- **Recreate 전략의 순서**: 첫 표본이 4초 뒤여서 옛 Pod 종료와 새 Pod 생성 사이의 정확한 순서/시간은 이번에 기록하지 못했다.
- **롤링 업데이트의 세부**: `maxSurge`/`maxUnavailable`을 `%`로 줄 때의 올림/내림 규칙, 롤아웃 중에 또 한 번 변경(중첩 롤아웃)하는 경우, `kubectl rollout undo --to-revision`은 이번에 시험하지 않았다.
- **`pause` 상태에서 변경 3개가 리비전 하나로 묶이는지**: `revisionHistoryLimit: 2` 때문에 리비전 번호로 비교하지 못했다. RS 개수가 `pause` 중에 3 → 2로 보인 이유도 확인하지 못했다. 자동 롤백(Argo Rollouts 등)과 카나리/블루-그린 배포는 시험하지 않았다.
- **DaemonSet/StatefulSet**: 노드가 하나라 여러 노드 배치, 노드 장애 시 StatefulSet Pod 처리, `ordinals`, `minReadySeconds`는 확인하지 못했다.
- **Job/CronJob**: `podFailurePolicy`, `backoffLimitPerIndex`, `startingDeadlineSeconds`, 밀린 일정의 보충 실행 규칙, 시간대/서머타임 경계는 확인하지 못했다.
- **HPA와의 상호작용**(오토스케일링은 18장 범위): `replicas`를 HPA가 조절하는 상태에서 롤아웃이 어떻게 동작하는지는 이번에 시험하지 않았다.
