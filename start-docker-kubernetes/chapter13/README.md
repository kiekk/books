### 13장. 파드를 사용하는 다른 오브젝트들

> 정리 범위: Job, CronJob, DaemonSet, StatefulSet
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (**단일 노드** `docker-desktop`), 기본 StorageClass `hostpath`(9장), kubectl v1.35.2
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`을 명시했다 (아래에서는 `k`로 줄여 적음). 실험은 `t13` 네임스페이스에서만 했고 끝나고 네임스페이스를 삭제했다 (DaemonSet 실험에서 노드에 붙인 라벨 `t13-role`도 제거했고, 동적 PV의 노드 디렉터리 중 남은 빈 상위 디렉터리도 정리했다).
>
> **단일 노드 환경의 한계**: DaemonSet이 "노드마다 하나"라는 핵심 동작은 노드 하나에서 "하나"만 볼 수 있었다. 여러 노드에 퍼지는 모습은 확인하지 못했다.

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. 전체 비교: 어떤 오브젝트를 언제 쓰나
2. Job
3. CronJob
4. DaemonSet
5. StatefulSet
6. 확인하지 못한 부분

---

#### 1. 전체 비교: 어떤 오브젝트를 언제 쓰나

6장의 Deployment는 "계속 떠 있어야 하는 무상태 앱"용이다. 이 장의 네 가지는 Pod를 쓰는 다른 방식이다.

| 오브젝트 | 한 줄 정의 | Pod 수명 | 대표 용도 |
|---|---|---|---|
| **Deployment** (6장) | 무상태 Pod N개를 계속 유지, 롤링 업데이트 | 계속 실행 | 웹/API 서버 |
| **Job** | Pod를 **끝날 때까지** 실행하고 성공 횟수를 채움 | **종료됨** (완료) | 일회성 배치, 마이그레이션 |
| **CronJob** | **일정에 따라** Job을 만듦 | 주기적으로 종료됨 | 백업, 정기 리포트 |
| **DaemonSet** | **노드마다 Pod 하나** | 계속 실행 | 로그 수집기, 모니터링 에이전트, 네트워크 플러그인 |
| **StatefulSet** | **고유한 이름/저장소를 가진** Pod N개 (순서 있는 생성/삭제) | 계속 실행 | DB, 메시지 브로커 (상태 저장) |

| 비교 | Deployment | StatefulSet | DaemonSet | Job |
|---|---|---|---|---|
| Pod 이름 | 랜덤 접미사 (`web-5bd64f6ccb-xxxxx`) | **고정 순번** (`db-0`, `db-1`) | 랜덤 (`agent-9xvc6`) | 랜덤 (`once-tz44z`) |
| 개수 결정 | `replicas` | `replicas` | **노드 수** (+ 노드 선택) | `completions` |
| Pod마다 저장소 | 공유/없음 | **Pod마다 전용 PVC** | 보통 hostPath | 없음 |
| 재생성 시 | 새 이름, 새 IP | **같은 이름**(새 IP), 같은 PVC | 같은 노드에 새 Pod | 설정에 따라 |
| 시작 순서 | 동시 | **순서대로** (기본) | 동시 | 동시 (`parallelism`) |

#### 2. Job

##### 2-1. 개념

**Pod를 하나 이상 실행해서 지정한 횟수만큼 "성공 종료"할 때까지 보장하는 컨트롤러.** Deployment는 Pod가 종료되면 다시 띄우지만, Job은 **성공으로 끝나면 더 이상 띄우지 않는다.** 실패하면 재시도한다.

```yaml
apiVersion: batch/v1
kind: Job
metadata: {name: once, namespace: t13}
spec:
  template:
    spec:
      restartPolicy: Never                # Job은 Always를 쓸 수 없다 (Never 또는 OnFailure)
      containers: [{name: c, image: "alpine:3.20", command: ["sh","-c","echo hello-from-job; sleep 2"]}]
```

```
$ k -n t13 get job once
NAME   STATUS     COMPLETIONS   DURATION   AGE
once   Complete   1/1           5s         5s
$ k -n t13 get pod -l job-name=once          →  once-tz44z   Completed        ← 완료된 Pod는 삭제되지 않고 남는다 (로그 확인용)
$ k -n t13 logs job/once                     →  hello-from-job
$ k -n t13 get job once -o jsonpath='{.spec.completionMode} {.spec.backoffLimit} {.spec.template.spec.restartPolicy}'
NonIndexed 6 Never                            ← 기본값: backoffLimit 6
```

- Job이 만드는 Pod에는 `job-name`, `controller-uid` 라벨이 붙어서 Job이 자기 Pod를 찾는다 (6장의 라벨 셀렉터 방식).
- `restartPolicy`는 **`Never` 또는 `OnFailure`만** 가능하다 (Pod의 기본값 `Always`는 Job에서 쓸 수 없다. 이 환경에서 `Always`로 시도하지는 않았고 문서 지식이다).
- 완료된 Pod는 `Completed`로 남는다. 지우려면 Job을 지우거나 `ttlSecondsAfterFinished`를 쓴다 (2-5절).

##### 2-2. completions와 parallelism

`completions: 6, parallelism: 2`로 Pod가 6번 성공해야 끝나는 Job을 실행했다 (각 Pod는 6초 대기).

```
시각   STATUS   COMPLETIONS   동시 Running 수   완료된 Pod
31s    Running  0/6           2                 0
35s    Running  2/6           0                 2        ← 2개 완료, 다음 2개 생성 중
39s    Running  2/6           2                 2
43s    Running  2/6           0                 4
47s    Running  4/6           2                 4
51s    Running  4/6           2                 4
→ par   Complete   6/6   28s
```

| 필드 | 의미 |
|---|---|
| `completions` | 성공해야 하는 Pod 수 (기본 1) |
| `parallelism` | 동시에 실행할 Pod 수 (기본 1) |
| `completionMode` | `NonIndexed`(기본, Pod가 서로 구별 안 됨) / `Indexed` |

- 동시에 최대 2개만 실행하고, 완료 개수를 채울 때까지 새 Pod를 만든다. 총 6번 성공 (28초).

##### 2-3. 실패 처리: backoffLimit, restartPolicy

항상 실패하는 컨테이너(`exit 1`)를 `backoffLimit: 2`로 실행하고 `restartPolicy`를 비교했다.

| 구분 | `restartPolicy: Never` | `restartPolicy: OnFailure` |
|---|---|---|
| 재시도 방법 | **새 Pod를 만든다** | **같은 Pod 안에서 컨테이너를 재시작** (`RESTARTS` 증가) |
| 실패한 Pod | 3개(시도 횟수만큼) `Error`로 **남는다** (로그 확인 가능) | 한도 도달 후 **Pod가 지워진다** (실험 끝에 남은 Pod 없음) |
| 최종 상태 | `Failed`, 이유 `BackoffLimitExceeded` | `Failed`, 이유 `BackoffLimitExceeded` |

```
$ k -n t13 get pod -l job-name=fail-never
fail-never-26zx8   Error   0
fail-never-jssbj   Error   0
fail-never-t2sgj   Error   0                  ← backoffLimit 2 → 최초 1회 + 재시도 2회 = 3개
$ k -n t13 get job fail-never -o jsonpath='{.status.conditions[*].type}: {.status.conditions[*].reason} / {.status.conditions[*].message}'
FailureTarget Failed: BackoffLimitExceeded BackoffLimitExceeded / Job has reached the specified backoff limit ...
```

- `Never`에서 Pod가 만들어진 시점은 대략 0초, 12초, 31초였다. 재시도 사이의 **대기 시간이 점점 늘어나는 지수 백오프**(문서상 10초, 20초, 40초 ... 최대 6분)와 일치한다 (시각은 6초 간격 샘플링이라 정확하지 않다).
- 실패 원인을 `kubectl logs`로 보려면 `Never`가 편하다 (Pod가 남아 있다). `OnFailure`는 한도 도달 후 Pod가 삭제되어 로그를 못 본다.
- `backoffLimit`(기본 6)을 넘으면 Job이 실패로 끝나고 더 이상 Pod를 만들지 않는다.

##### 2-4. activeDeadlineSeconds와 ttlSecondsAfterFinished

```yaml
spec:
  activeDeadlineSeconds: 8             # Job 전체의 최대 실행 시간
  ...
```

```
$ k -n t13 get job deadline
deadline   Failed   0/1   92s
$ k -n t13 get job deadline -o jsonpath='{.status.conditions[*].reason}: {.status.conditions[*].message}'
DeadlineExceeded: Job was active longer than specified deadline
$ k -n t13 get pod -l job-name=deadline          →  No resources found        ← 실행 중이던 Pod도 종료되어 삭제됨
```

- `activeDeadlineSeconds`를 넘기면 Job은 `DeadlineExceeded`로 실패하고 **실행 중이던 Pod도 종료된다.** 무한히 도는 작업을 막는 안전장치다.

```
$ (ttlSecondsAfterFinished: 10 인 Job이 완료)
$ k -n t13 get job ttl            →  Error from server (NotFound)        ← 완료 후 지정한 시간이 지나 Job과 Pod가 자동 삭제됨
```

- 완료된 Job/Pod는 기본으로 남기 때문에 쌓이면 부담이 된다. `ttlSecondsAfterFinished`로 자동 정리한다 (CronJob은 별도의 히스토리 제한이 있다, 3-3절).

##### 2-5. Indexed Job, suspend, 불변성

**Indexed (completionMode)**: 각 Pod에 고유한 번호를 준다. 작업을 번호로 나눠 처리할 때 쓴다.

```
$ (completions: 4, parallelism: 2, completionMode: Indexed) 각 Pod가 echo index=$JOB_COMPLETION_INDEX host=$(hostname)
index=0 host=indexed-0
index=1 host=indexed-1
index=2 host=indexed-2
index=3 host=indexed-3
$ k -n t13 get pod -l job-name=indexed -o jsonpath='{range .items[*]}{.metadata.name} idx={.metadata.annotations.batch\.kubernetes\.io/job-completion-index} hostname={.spec.hostname}{"\n"}{end}'
indexed-0-khz6r idx=0 hostname=indexed-0
...
```

- Pod마다 환경 변수 `JOB_COMPLETION_INDEX`(0~3)와 어노테이션이 주어지고 호스트명도 `indexed-N`이다. 번호별로 서로 다른 데이터 조각을 처리하는 작업(분할 처리)에 쓴다.

**suspend**

```
$ (suspend: true 로 생성)       →  susp   Suspended        Pod 없음
$ k -n t13 patch job susp -p '{"spec":{"suspend":false}}'   →  6초 뒤 Complete 1/1
```

- `suspend: true`로 만들면 Pod를 만들지 않고 대기한다. `false`로 바꾸는 순간 실행된다.

**불변성**

```
$ k -n t13 patch job once --type=merge -p '{"spec":{"template":{"spec":{"containers":[{"name":"c","image":"alpine:3.19"}]}}}}'
The Job "once" is invalid: spec.template: Invalid value: core.PodTemplateSpec{...}: field is immutable
$ k apply -f jobs.yaml (같은 Job을 다시)  →  job.batch/once unchanged
```

- **Job의 Pod 템플릿은 만든 뒤 바꿀 수 없다.** 다시 실행하려면 Job을 삭제하고 새로 만들거나 새 이름으로 만든다. 같은 YAML을 `apply` 하면 이미 완료된 Job은 `unchanged`이고 다시 실행되지 않는다 (한 번 실행하는 일회성 오브젝트).

#### 3. CronJob

##### 3-1. 개념

**cron 표현식의 일정에 따라 Job을 만드는 컨트롤러.** CronJob → Job → Pod의 소유 관계다 (실측: Job의 `ownerReferences`가 `CronJob/fast`, `controller=true`).

```yaml
apiVersion: batch/v1
kind: CronJob
metadata: {name: fast, namespace: t13}
spec:
  schedule: "* * * * *"               # 분 시 일 월 요일
  concurrencyPolicy: Allow            # Allow | Forbid | Replace
  successfulJobsHistoryLimit: 2
  failedJobsHistoryLimit: 1
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: Never
          containers: [{name: c, image: "alpine:3.20", command: ["sh","-c","echo $(date +%T) start $(hostname); sleep 5; echo end"]}]
```

```
$ k -n t13 get cronjob
NAME   SCHEDULE    TIMEZONE   SUSPEND   ACTIVE   LAST SCHEDULE   AGE
fast   * * * * *   <none>     False     0        <none>          0s
$ k -n t13 get job
fast-29880859   Complete   1/1   8s
fast-29880860   Complete   1/1   8s                ← Job 이름 = <cronjob>-<예정 시각(분 단위 에포크)>
```

- 만들어지는 Job 이름의 숫자는 **예정 시각을 분 단위로 환산한 값**이다 (`29880860`은 1분마다 1씩 증가). 이 이름 덕분에 같은 예정 시각의 Job이 중복 생성되는 것을 막는다.
- CronJob의 스케줄은 **컨트롤러 매니저의 시간대(UTC)** 로 해석된다 (`lastScheduleTime`이 `...14:27:00Z`). 시간대를 지정하려면 `timeZone`을 쓴다 (3-4절).

##### 3-2. concurrencyPolicy: 이전 Job이 아직 실행 중일 때

1분마다 실행하는데 작업이 80초 걸리는 CronJob 세 개를 만들어 비교했다 (실행 시간이 주기보다 길다). `fast`는 5초짜리 비교군이다.

| 정책 | 동작 | 실측 |
|---|---|---|
| **Allow** (기본) | 이전 Job이 끝나지 않아도 새 Job을 만든다 (중첩 실행) | `slow-allow`의 `ACTIVE`에 Job이 **2개 동시에** (`slow-allow-29880866`, `...867`) |
| **Forbid** | 이전 Job이 실행 중이면 **새 Job을 만들지 않는다** (건너뜀) | `slow-forbid`는 **동시에 둘이 실행된 적이 없다**. 이전 Job이 끝난 뒤에야 다음 Job이 시작되었고 일부 예정 시각은 건너뛰어졌다 |
| **Replace** | 이전 Job을 **삭제하고** 새 Job으로 교체 | `slow-replace`는 매분 `SuccessfulDelete: Deleted job slow-replace-...` + `Killing: Stopping container c`. **한 번도 완료되지 못했고** `lastSuccessfulTime`이 비어 있음 |

```
$ k -n t13 get cronjob slow-allow    -o jsonpath='active={.status.active[*].name}'    →  slow-allow-29880866 slow-allow-29880867
$ k -n t13 get cronjob slow-forbid   -o jsonpath='active=...'                          →  slow-forbid-29880865
$ k -n t13 get cronjob slow-replace  -o jsonpath='active=... lastSuccessful=...'      →  active=slow-replace-29880867  lastSuccessful=
$ k -n t13 get events
slow-replace   SuccessfulDelete   Deleted job slow-replace-29880865
slow-replace-29880865-7xl49   Killing   Stopping container c
```

- **Allow**는 중첩이 생겨서 자원/데이터 충돌이 날 수 있다. **Forbid**는 처리가 밀리면 건너뛴다. **Replace**는 작업이 주기보다 오래 걸리면 영원히 끝나지 못한다.
- 작업 시간이 주기보다 길어질 수 있는 일(백업 등)에는 보통 `Forbid`를 쓴다 (문서 지식). 이 실험에서는 `Forbid`에서 건너뛰어진 예정 시각을 별도 이벤트로 확인하지는 못했고 Job 이름의 번호가 일부 비는 것으로만 확인했다. 이전 Job이 끝난 직후 밀린 예정 시각의 Job이 늦게 시작되는 모습(예: `slow-forbid-29880865`가 예정 시각보다 늦게 시작)을 봤고, 이를 컨트롤러가 정해진 시간 안에서 따라잡아 실행하는 것으로 추정한다 (`startingDeadlineSeconds`와 관련해 보이나 이 필드는 시험하지 않았다).

##### 3-3. 히스토리 제한

```
$ (successfulJobsHistoryLimit: 2) fast
fast-29880865   Complete
fast-29880866   Complete
fast-29880867   Running        ← 이전 Job fast-29880864는 이벤트 SuccessfulDelete: Deleted job fast-29880864 로 삭제됨
```

- 완료된 Job을 **성공은 2개, 실패는 1개**(`failedJobsHistoryLimit`)만 남기고 오래된 것부터 지운다. 기본값은 성공 3, 실패 1이다 (기본값은 문서 지식). Pod도 Job과 함께 지워진다.
- 디버깅을 위해 실패한 Job은 오래 남겨 두는 편이 좋다.

##### 3-4. 수동 실행, suspend, 검증, 삭제

```
$ k -n t13 create job manual-run --from=cronjob/fast        # CronJob 정의로 즉시 한 번 실행
$ k -n t13 get job manual-run -o jsonpath='owner={.metadata.ownerReferences[0].kind}/{...name} annotation={.metadata.annotations}'
owner=CronJob/fast annotation={"cronjob.kubernetes.io/instantiate":"manual"}

$ k -n t13 patch cronjob fast -p '{"spec":{"suspend":true}}'
(75초 대기)  새 Job 없음, lastScheduleTime 그대로(14:27:00Z)       ← 일시 중지
$ k -n t13 patch cronjob fast --type=merge -p '{"spec":{"schedule":"61 * * * *"}}'
The CronJob "fast" is invalid: spec.schedule: Invalid value: "61 * * * *": end of range (61) above maximum (59): 61
$ ... -p '{"spec":{"timeZone":"Asia/Seoul"}}'           →  patched
$ ... -p '{"spec":{"timeZone":"Mars/Base"}}'
The CronJob "fast" is invalid: spec.timeZone: Invalid value: "Mars/Base": unknown time zone Mars/Base
$ k -n t13 delete cronjob fast                           →  Job과 Pod도 모두 삭제됨 (소유 관계)
```

- `kubectl create job --from=cronjob/<이름>`으로 정해진 일정과 상관없이 **테스트로 즉시 실행**할 수 있다. 어노테이션 `cronjob.kubernetes.io/instantiate: manual`이 붙는다.
- `suspend: true`는 **새 Job 생성만 멈추고** 이미 실행 중인 Job은 그대로 둔다 (이 실험에서 실행 중 Job이 있는 상태로 suspend를 시험하지는 않았다. 새 Job이 안 생기는 것만 확인).
- 스케줄 형식과 `timeZone` 이름은 API가 검증한다.
- CronJob을 지우면 소유한 Job과 Pod도 같이 지워진다 (6장의 연쇄 삭제).

#### 4. DaemonSet

##### 4-1. 개념

**조건에 맞는 모든 노드에 Pod를 하나씩 실행하는 컨트롤러.** 노드가 추가되면 거기에도 Pod를 만들고, 노드가 사라지면 Pod도 정리한다. 로그 수집기(fluentd), 모니터링 에이전트(node-exporter), 네트워크 플러그인, kube-proxy(6장에서 DaemonSet으로 확인)가 대표적이다.

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata: {name: agent, namespace: t13}
spec:
  selector: {matchLabels: {app: agent}}
  template:
    metadata: {labels: {app: agent}}
    spec:
      nodeSelector: {t13-role: logging}              # 이 라벨이 있는 노드에만
      containers: [{name: c, image: "alpine:3.20", command: ["sleep","3600"], resources: {requests: {cpu: 10m}}}]
```

- `replicas` 필드가 없다. 개수는 **노드 수(와 선택 조건)** 로 정해진다.

##### 4-2. 노드 선택과 동작 (실측)

```
$ k apply -f ds.yaml                              # 노드에 t13-role 라벨 없음
$ k -n t13 get ds agent
DESIRED=0 CURRENT=0 READY=0                        ← 조건에 맞는 노드가 없으면 Pod도 0개

$ k label node docker-desktop t13-role=logging     # 5초 뒤
DESIRED=1 CURRENT=1 READY=1
agent-9xvc6   Running   docker-desktop              ← 라벨이 붙자 자동으로 Pod 생성

$ k -n t13 delete pod agent-9xvc6                   →  agent-m42fl Running (새 Pod가 바로 생성)
$ k label node docker-desktop t13-role-             # 라벨 제거
agent-m42fl   1/1   Terminating                      ← 조건에서 벗어나자 Pod 삭제, DESIRED=0
```

- 노드 라벨이 바뀌면 DaemonSet 컨트롤러가 즉시 Pod를 만들거나 지운다. 새 노드가 클러스터에 합류하면 라벨 조건만 맞으면 자동으로 에이전트가 배치된다 (노드 추가는 이 환경에서 확인하지 못했다).
- Pod를 직접 지워도 같은 노드에 새 Pod가 생성된다.

**DaemonSet 컨트롤러가 Pod에 넣어 주는 것**

```
$ k -n t13 get pod agent-9xvc6 -o jsonpath='{.spec.affinity}'
{"nodeAffinity":{"requiredDuringSchedulingIgnoredDuringExecution":{"nodeSelectorTerms":[{"matchFields":[{"key":"metadata.name","operator":"In","values":["docker-desktop"]}]}]}}}
$ ... -o jsonpath='{range .spec.tolerations[*]}{.key}:{.effect} {end}'
node.kubernetes.io/not-ready:NoExecute node.kubernetes.io/unreachable:NoExecute node.kubernetes.io/disk-pressure:NoSchedule
node.kubernetes.io/memory-pressure:NoSchedule node.kubernetes.io/pid-pressure:NoSchedule node.kubernetes.io/unschedulable:NoSchedule
$ ... -o jsonpath='owner={.metadata.ownerReferences[0].kind}/{...name} schedulerName={.spec.schedulerName}'
owner=DaemonSet/agent schedulerName=default-scheduler
```

- DaemonSet Pod는 **기본 스케줄러(`default-scheduler`)** 가 스케줄한다. 컨트롤러는 Pod마다 **"이 노드(`metadata.name`)에만"** 이라는 필수 노드 어피니티를 Pod에 넣어서 노드를 지정한다 (11장 5절의 nodeAffinity가 그대로 쓰인다).
- **노드가 NotReady, unreachable, 디스크/메모리/PID 압박, unschedulable 상태여도** Pod가 계속 있거나 배치되도록 톨러레이션이 자동으로 붙는다. 시스템 에이전트는 노드 상태가 나빠도 계속 돌아야 하기 때문이다 (11장 4절의 taint/toleration).

##### 4-3. 업데이트 전략

```
$ k -n t13 get ds agent -o jsonpath='{.spec.updateStrategy}'
{"rollingUpdate":{"maxSurge":0,"maxUnavailable":1},"type":"RollingUpdate"}       ← 기본값

$ k -n t13 set image ds/agent c=alpine:3.19 ; k -n t13 rollout status ds/agent
daemon set "agent" successfully rolled out                 # Pod가 새 이미지로 교체됨
$ k -n t13 rollout history ds/agent                         →  REVISION 1, 2
$ k -n t13 get controllerrevision                           →  agent-79c976495b 1 / agent-7d564f8d96 2

$ k -n t13 patch ds agent -p '{"spec":{"updateStrategy":{"type":"OnDelete"}}}' ; k -n t13 set image ds/agent c=alpine:3.20
$ k -n t13 get pod -l app=agent -o jsonpath='image still={...image}'     →  alpine:3.19         ← 템플릿은 바뀌었는데 Pod는 그대로
$ k -n t13 delete pod -l app=agent                                          # 직접 삭제하면
after manual delete image=alpine:3.20                                       ← 새 템플릿으로 생성
```

| 전략 | 동작 |
|---|---|
| `RollingUpdate` (기본) | 노드별로 `maxUnavailable`(기본 1)개씩 Pod를 새 버전으로 교체 |
| `OnDelete` | 템플릿이 바뀌어도 **Pod를 직접 삭제할 때만** 새 버전으로 교체 |

- 6장의 Deployment와 달리 **ReplicaSet이 없고** DaemonSet이 Pod를 직접 소유한다. 리비전은 `ControllerRevision` 오브젝트에 저장된다 (`rollout history`, `rollout undo`가 이를 이용).
- 단일 노드라서 노드별로 순차 교체되는 모습은 보지 못했다. `maxSurge`가 0이라 기본 전략은 **옛 Pod를 먼저 지운 뒤 새 Pod를 만든다** (이 환경에서는 노드가 하나라 순서를 구분해 보지 못했다).

##### 4-4. 쓰임새

| 용도 | 예 |
|---|---|
| 노드의 로그/메트릭 수집 | fluentd, node-exporter (`hostPath`로 `/var/log`, `/proc` 마운트, 9장) |
| 네트워크/스토리지 플러그인 | CNI, CSI 노드 플러그인, kube-proxy |
| 노드별 데몬 | 보안 에이전트 |

#### 5. StatefulSet

##### 5-1. 개념

**상태를 가진 앱(DB 등)을 위해 Pod에 고유한 정체성을 주는 컨트롤러.** Deployment의 Pod는 서로 같고 교체 가능하지만, DB 복제본은 "나는 0번(프라이머리)"처럼 **구별되어야 하고 자기 데이터를 유지**해야 한다. StatefulSet이 보장하는 것:

1. **안정적인 이름과 네트워크 ID**: `<이름>-0`, `<이름>-1` ... 고정 (재생성되어도 같은 이름)
2. **Pod마다 고유한 저장소**: `volumeClaimTemplates`로 Pod마다 PVC 생성, 재생성되어도 같은 PVC를 다시 붙임
3. **순서 있는 생성/삭제/업데이트**: 순번 순서대로 (기본)

```yaml
apiVersion: v1
kind: Service
metadata: {name: db, namespace: t13}
spec:
  clusterIP: None                           # 헤드리스 Service
  selector: {app: db}
  ports: [{port: 80, name: web}]
---
apiVersion: apps/v1
kind: StatefulSet
metadata: {name: db, namespace: t13}
spec:
  serviceName: db                           # 헤드리스 Service 이름
  replicas: 3
  selector: {matchLabels: {app: db}}
  template:
    metadata: {labels: {app: db}}
    spec:
      containers:
      - name: c
        image: alpine:3.20
        command: ["sh","-c","echo $(date +%T) started on $(hostname) >> /data/log.txt; touch /tmp/ready; sleep 3600"]
        readinessProbe: {exec: {command: ["sh","-c","test -f /tmp/ready"]}, initialDelaySeconds: 6, periodSeconds: 2}
        volumeMounts: [{name: data, mountPath: /data}]
  volumeClaimTemplates:
  - metadata: {name: data}
    spec: {accessModes: [ReadWriteOnce], resources: {requests: {storage: 100Mi}}}
```

##### 5-2. 순서 있는 생성 (OrderedReady)

readiness probe가 시작 6초 뒤 통과하는 Pod를 3개로 만들고 3초 간격으로 기록했다.

```
 0s  db-0=Pending(0/1)
 6s  db-0=Running(0/1)
 9s  db-0=Running(1/1) db-1=Pending(0/1)                ← db-0이 Ready가 된 뒤에야 db-1 생성
12s  db-0=Running(1/1) db-1=Running(0/1)
18s  db-0=Running(1/1) db-1=Running(1/1) db-2=Pending(0/1)   ← db-1이 Ready가 된 뒤에야 db-2 생성
28s  db-0=Running(1/1) db-1=Running(1/1) db-2=Running(1/1)
$ k -n t13 get pvc
data-db-0   Bound   pvc-a09e0565-...         ← Pod마다 PVC가 만들어짐 (이름: <템플릿 이름>-<Pod 이름>)
data-db-1   Bound   pvc-23a00421-...
data-db-2   Bound   pvc-4b60a839-...
$ k -n t13 get pod db-0 -o jsonpath='hostname={.spec.hostname} subdomain={.spec.subdomain}'
hostname=db-0 subdomain=db
$ k -n t13 get pod db-0 -o jsonpath='{.metadata.labels}'
{"app":"db","apps.kubernetes.io/pod-index":"0","controller-revision-hash":"db-6564894d8f","statefulset.kubernetes.io/pod-name":"db-0"}
```

- **앞 Pod가 Ready여야 다음 Pod를 만든다** (기본 `podManagementPolicy: OrderedReady`). DB 클러스터처럼 0번이 먼저 떠야 1번이 합류할 수 있는 앱을 위한 것이다. Deployment는 모든 Pod를 동시에 만든다 (6장).
- Pod 이름이 순번 `db-0`, `db-1`, `db-2`이고 Pod에는 `statefulset.kubernetes.io/pod-name`, `apps.kubernetes.io/pod-index` 라벨이 붙는다 (이 라벨로 특정 Pod만 선택하는 Service를 만들 수 있다).
- PVC가 **Pod마다 하나씩 동적 프로비저닝**되었다 (9장).

##### 5-3. 헤드리스 Service와 안정적인 DNS

```
$ k -n t13 exec cli -- nslookup db.t13.svc.cluster.local               # 헤드리스 Service 이름
Address: 10.1.1.138
Address: 10.1.1.140
Address: 10.1.1.139                                                     ← Pod IP 3개가 모두 반환됨 (ClusterIP가 없다)
$ k -n t13 exec cli -- nslookup db-1.db.t13.svc.cluster.local           # Pod 개별 이름
Address: 10.1.1.139                                                     ← db-1 하나의 IP
$ k -n t13 get endpointslice -l kubernetes.io/service-name=db -o jsonpath='{range .items[0].endpoints[*]}{.hostname}={.addresses[0]} {end}'
db-0=10.1.1.138 db-1=10.1.1.139 db-2=10.1.1.140
```

| DNS 이름 | 의미 |
|---|---|
| `<서비스>.<ns>.svc.cluster.local` | 헤드리스 Service: **모든 Pod의 IP** (로드밸런싱 없이 클라이언트가 직접 선택) |
| `<Pod 이름>.<서비스>.<ns>.svc.cluster.local` | **특정 Pod 하나의 IP** (예: `db-0.db.t13.svc...`) |

- 6장의 일반 Service는 ClusterIP 하나로 Pod들을 대표했다. **헤드리스 Service(`clusterIP: None`)** 는 가상 IP 없이 **Pod의 IP를 DNS로 직접 알려 준다.** StatefulSet의 Pod는 `<Pod 이름>.<서비스>`라는 **고정된 DNS 이름**을 가지므로, DB 복제본이 "프라이머리는 `db-0.db`"처럼 서로를 이름으로 찾을 수 있다. StatefulSet의 `serviceName`이 이 Service를 가리킨다.
- Pod IP는 재생성되면 바뀌지만 **DNS 이름은 그대로**다 (5-4절).

##### 5-4. Pod 삭제와 재생성: 같은 이름, 같은 데이터

```
$ k -n t13 exec db-1 -- sh -c 'echo written-by-db-1 > /data/mine.txt; cat /data/log.txt'
14:20:48 started on db-1
$ k -n t13 delete pod db-1 ; k -n t13 wait --for=condition=Ready pod/db-1
name=db-1 같은 이름, 새 uid           ip: 10.1.1.139 -> 10.1.1.146          ← 새 Pod, IP 변경
$ k -n t13 exec db-1 -- sh -c 'cat /data/mine.txt; cat /data/log.txt'
written-by-db-1                                                             ← 데이터 유지
14:20:48 started on db-1
14:21:54 started on db-1                                                    ← 컨테이너가 다시 시작하며 기록 추가 (같은 PVC에 이어서)
```

- 삭제해도 **같은 이름 `db-1`로 재생성**되고 **같은 PVC(`data-db-1`)** 를 다시 붙여서 데이터가 이어졌다. 새 Pod라서 `uid`와 IP는 바뀌었다. Deployment라면 랜덤 이름의 새 Pod가 빈 저장소로 시작한다.

##### 5-5. 스케일 인/아웃

```
$ k -n t13 scale sts db --replicas=1
 2s  db-0=Running db-1=Running db-2=Terminating
 6s  db-0=Running db-1=Terminating                          ← 큰 순번부터 하나씩 (db-2가 끝난 뒤 db-1)
 8s  db-0=Running
$ k -n t13 get pvc
data-db-0   Bound
data-db-1   Bound                                           ← PVC는 남는다
data-db-2   Bound
$ k -n t13 scale sts db --replicas=3                          # 다시 늘리면
$ k -n t13 exec db-1 -- sh -c 'cat /data/mine.txt; wc -l < /data/log.txt'
written-by-db-1 ; log lines: 4                              ← 이전 데이터가 붙은 채로 db-1이 돌아옴
```

- **스케일 인은 역순(큰 순번부터)** 이고 한 번에 하나씩이다. 스케일 인해도 **PVC는 삭제되지 않는다** (기본). 다시 늘리면 같은 순번의 Pod가 기존 PVC에 붙어 데이터가 이어진다.
- 종료가 오래 걸리면(컨테이너 PID 1이 SIGTERM을 무시해 `terminationGracePeriodSeconds` 30초 대기, 11장 6절) 스케일 인도 그만큼 느려진다. 실험에서 처음 시도한 스케일 인은 이 때문에 길어졌고, 템플릿에 `terminationGracePeriodSeconds: 2`를 넣은 뒤 다시 확인했다.

**PVC 보존 정책 `persistentVolumeClaimRetentionPolicy`**

| 설정 | 동작 (실측) |
|---|---|
| 기본 (`whenDeleted: Retain`, `whenScaled: Retain`) | StatefulSet을 삭제해도 PVC가 남는다 (`data-db-0~2` 남음). **같은 이름으로 다시 만들면 기존 PVC를 재사용**하고 이전 `log.txt`가 그대로 보였다 |
| `whenScaled: Delete` | 스케일 인(3→1) 시 사라진 순번의 PVC도 삭제 (`data-par-1`, `data-par-2` 삭제, `data-par-0`만 남음) |
| `whenDeleted: Delete` | StatefulSet 삭제 시 PVC도 삭제 (`par`의 PVC가 모두 사라짐) |

- 기본값은 **데이터를 지키는 쪽(Retain)** 이다. 대신 StatefulSet을 지운 뒤 PVC가 남아 있는 것을 알아서 정리해야 한다. 동적 PV는 PVC 삭제 시 `Delete` 정책으로 데이터가 함께 지워진다 (9장).

##### 5-6. podManagementPolicy: Parallel

```
$ (podManagementPolicy: Parallel)
par-0=Pending par-1=Pending par-2=Pending
par-0=Running par-1=Running par-2=Running            ← 순서 없이 동시에 생성
```

- 순서가 필요 없는 상태 저장 앱(고유 ID와 저장소만 필요)에는 `Parallel`로 빠르게 띄울 수 있다. 업데이트 순서는 이 설정과 별개로 `updateStrategy`가 정한다.

##### 5-7. 업데이트 전략

**RollingUpdate (기본): 역순으로 하나씩**

```
$ k -n t13 patch sts db --type=merge -p '{"spec":{"template":{"spec":{"terminationGracePeriodSeconds":2}}}}'
 2s  db-0=Running db-1=Running  db-2=Terminating
 6s  db-0=Running db-1=Running  db-2=Running(0/1)         ← db-2가 먼저 새 버전으로
10s  db-0=Running db-1=Terminating db-2=Running(1/1)      ← db-2가 Ready가 된 뒤 db-1
(이후 db-0)
$ k -n t13 rollout status sts/db   →  partitioned roll out complete: 3 new pods have been updated...
```

- **가장 큰 순번(db-2)부터** 새 버전으로 교체하고 **Ready가 된 뒤에** 다음 순번을 교체한다. 프라이머리(0번)를 가장 마지막에 건드린다는 의미다.

**partition: 카나리 배포**

```
$ k -n t13 patch sts db --type=merge -p '{"spec":{"updateStrategy":{"type":"RollingUpdate","rollingUpdate":{"partition":2}}}}'
$ (템플릿 변경: env VER=2 추가)
db-0 VER=  rev=db-66db8697f6
db-1 VER=  rev=db-66db8697f6
db-2 VER=2 rev=db-759884d8cb                               ← 순번 2 이상만 업데이트됨
$ k -n t13 get sts db -o jsonpath='current={.status.currentRevision} update={.status.updateRevision} updated={.status.updatedReplicas}'
current=db-66db8697f6 update=db-759884d8cb updated=1
$ (partition: 1)   →  db-1, db-2 업데이트    $ (partition: 0)   →  전체 업데이트, current = update = db-759884d8cb
```

- **`partition: N`이면 순번 N 이상만** 새 버전으로 업데이트되고 나머지는 옛 버전에 머문다. 일부만 먼저 올려 보고(카나리) 문제없으면 `partition`을 낮춰서 점진적으로 확장한다. 전체가 올라가면 `currentRevision`이 새 리비전으로 바뀐다.

**OnDelete**

```
$ k -n t13 patch sts db --type=merge -p '{"spec":{"updateStrategy":{"type":"OnDelete","rollingUpdate":null}}}'
$ (템플릿 변경: VER=3)       →  db-0, db-1, db-2 모두 VER=2 그대로
$ k -n t13 delete pod db-0     →  db-0 after manual delete VER=3         ← 삭제한 Pod만 새 템플릿으로 재생성
```

- DaemonSet의 `OnDelete`와 같은 방식이다. 운영자가 Pod 교체 시점을 직접 정할 때 쓴다.

**수정 가능한 필드**

```
$ k -n t13 patch sts db --type=merge -p '{"spec":{"serviceName":"other"}}'
The StatefulSet "db" is invalid: spec: Forbidden: updates to statefulset spec for fields other than 'replicas', 'ordinals', 'template', 'updateStrategy', 'persistentVolumeClaimRetentionPolicy' and 'minReadySeconds' are forbidden
```

- `serviceName`, `selector`, **`volumeClaimTemplates`** 등은 만든 뒤 바꿀 수 없다. 저장소 크기를 늘리려면 PVC를 직접 수정하는 방식(9장의 볼륨 확장)이 필요하다 (이 환경에서는 확장이 지원되지 않았다).

##### 5-8. StatefulSet 사용 시 주의

| 주의 | 이유 |
|---|---|
| 헤드리스 Service를 따로 만들어야 한다 | StatefulSet은 Service를 만들어 주지 않는다 (`serviceName`으로 연결만) |
| PVC가 자동으로 지워지지 않는다 (기본) | 삭제/스케일 인 후 정리 필요, `persistentVolumeClaimRetentionPolicy` |
| 노드/Pod 장애 시 동작 | 같은 ID의 Pod를 중복으로 띄우지 않도록 하는 보장(at-most-one)이 있다는 설명이 있으나 이 환경(단일 노드)에서 확인하지 못했다 |
| 앱이 클러스터링을 알아서 해야 한다 | StatefulSet은 이름/저장소/순서만 제공하고 DB 복제 구성은 앱(또는 오퍼레이터, 12장)의 몫 |
| 데이터 가용성은 저장소에 달림 | `hostpath` 같은 노드 로컬 저장소는 노드를 벗어나면 접근 불가 (9장) |

- 상태 저장 앱을 운영할 때는 StatefulSet을 직접 쓰기보다 **오퍼레이터(12장)** 로 감싸서 쓰는 경우가 많다.

#### 6. 확인하지 못한 부분

- **DaemonSet의 노드별 배치**: 노드가 하나라 여러 노드에 퍼지는 모습, 노드 추가/제거에 따른 자동 배치/정리, 컨트롤 플레인 노드의 taint와 톨러레이션, `RollingUpdate`의 `maxUnavailable`에 따른 노드별 순차 교체, `maxSurge` 설정은 확인하지 못했다. `hostNetwork`/`hostPort`와 `nodeAffinity`/`nodeSelector` 조합도 확인하지 않았다.
- **Job의 나머지 기능**: `podFailurePolicy`, `backoffLimitPerIndex`, `successPolicy`, `completions`+`parallelism`으로 작업 큐 패턴 구현, 병렬 Job의 실패 처리, Job이 사용하는 Pod 템플릿의 `restartPolicy: Always` 거부 메시지는 확인하지 않았다. 지수 백오프의 정확한 대기 시간은 6초 간격 샘플링이라 근사값만 확인했다.
- **CronJob의 나머지 기능**: `startingDeadlineSeconds`와 밀린 일정의 보충 실행 규칙(100회 초과 시 중단 등), 시간대/서머타임 경계, 실행 중인 Job이 있는 상태에서의 `suspend`, `Forbid`에서 건너뛴 일정이 이벤트로 남는지는 확인하지 못했다. `Forbid`와 `Replace`의 동작은 Job 이름 번호와 이벤트에서 추정한 부분이 있다.
- **StatefulSet의 장애 동작**: 노드 장애 시 Pod 재생성 보장, `minReadySeconds`, `ordinals.start`, 헤드리스 Service의 `publishNotReadyAddresses`, Pod 정체성이 유지되는 실제 DB 복제 구성은 실험하지 않았다. `volumeClaimTemplates` 크기 변경과 PVC 확장은 이 환경의 프로비저너가 확장을 지원하지 않아 확인하지 못했다.
- **StatefulSet 삭제 시 `--cascade=orphan`**, 강제 삭제(`--force`)의 위험은 다루지 않았다.
- **OnFailure에서 재시작 횟수 표기**: `OnFailure`의 같은 Pod 재시작이 `RESTARTS` 증가로 보이는 것은 샘플링 중 한 번 관찰했고, 한도 도달 후 Pod가 삭제되는 정확한 시점과 이유는 확인하지 않았다.
