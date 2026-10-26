### 5장. 파드

> 정리 범위: Pod 라이프사이클, kubelet의 livenessProbe/readinessProbe/startupProbe, init containers와 containers, pause 컨테이너와 static pod, Pod 환경 변수(env) 설정, 멀티 컨테이너 패턴(사이드카/앰배서더/어댑터)
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), **Docker Desktop 내장 쿠버네티스 v1.32.2**(단일 노드 `docker-desktop`), static pod 생성 실험은 별도의 **임시 kind 클러스터 `t5static`**(v1.35.0, 노드 1개, 실험 후 삭제, 별도 kubeconfig 사용)
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 Docker Desktop 실험은 모두 `--context docker-desktop`을 명시했다 (`k`로 줄여 적음). 실험 네임스페이스 `t5p`와 kind 클러스터는 끝나고 삭제했다.
>
> 다른 책 `시작하세요! 도커/쿠버네티스`와 겹치는 부분은 짧게 적고 링크했다: Pod와 도커 컨테이너의 차이/pause 컨테이너 상세([6장](../../start-docker-kubernetes/chapter06/README.md)), 프로브 상세 실험([11장](../../start-docker-kubernetes/chapter11/README.md)), ConfigMap/Secret([7장](../../start-docker-kubernetes/chapter07/README.md)).

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. Pod 라이프사이클
2. kubelet 프로브: livenessProbe, readinessProbe, startupProbe
3. init containers와 containers
4. pause 컨테이너와 static pod
5. Pod 환경 변수(env) 설정
6. 멀티 컨테이너 패턴: 사이드카, 앰배서더, 어댑터
7. 확인하지 못한 부분

---

#### 1. Pod 라이프사이클

##### 1-1. 세 가지 상태 층위

Pod의 상태는 서로 다른 세 층에서 본다.

| 층 | 필드 | 값 |
|---|---|---|
| **Pod phase** (요약) | `status.phase` | `Pending`, `Running`, `Succeeded`, `Failed` (`Unknown`은 이번에 보지 못함) |
| **Pod conditions** (단계별 진행) | `status.conditions[]` | `PodScheduled`, `PodReadyToStartContainers`, `Initialized`, `ContainersReady`, `Ready` |
| **컨테이너 state** (컨테이너별) | `status.containerStatuses[].state` | `Waiting`(+reason), `Running`, `Terminated`(+reason, exitCode) |

- `kubectl get pods`의 `STATUS` 열은 위 세 가지를 **사람이 읽기 좋게 합성한 값**이다 (`Init:Error`, `CrashLoopBackOff`, `ErrImagePull`, `Completed` 등은 phase가 아니라 이 합성값이다).

##### 1-2. 정상 시작의 타임라인 (실측)

init container(6초) + 메인 컨테이너(`postStart` 훅 3초 + readiness probe)로 된 Pod의 상태를 2초 간격으로 기록했다.

```
 0s phase=Pending  [PodReadyToStartContainers=False Initialized=False Ready=False ContainersReady=False PodScheduled=True]  init=waiting(PodInitializing)
 2s phase=Pending  [PodReadyToStartContainers=True  Initialized=False Ready=False ContainersReady=False PodScheduled=True]  init=running
 8s phase=Pending  [PodReadyToStartContainers=True  Initialized=True  Ready=False ContainersReady=False PodScheduled=True]  init=terminated(Completed)
13s phase=Running  [PodReadyToStartContainers=True  Initialized=True  Ready=True  ContainersReady=True  PodScheduled=True]  init=terminated(Completed)
```

| 순서 | condition | 의미 |
|---|---|---|
| 1 | `PodScheduled=True` | 스케줄러가 노드를 정함 (도커 책 11장) |
| 2 | `PodReadyToStartContainers=True` | 노드에서 **샌드박스(pause 컨테이너 + 네트워크)** 가 준비됨 (4절) |
| 3 | `Initialized=True` | **모든 init container가 성공**으로 끝남 (3절) |
| 4 | `ContainersReady=True` | 모든 컨테이너가 Ready (readinessProbe 통과 포함) |
| 5 | `Ready=True` | Pod가 트래픽을 받을 준비 (Service 엔드포인트에 포함됨) |

- **init container가 도는 동안 phase는 `Pending`** 이다. init이 끝나고 메인 컨테이너가 `Running` 상태가 된 뒤(13초)에야 `Running`이 된다.
- `postStart` 훅(3초 대기)이 끝나기 전까지 메인 컨테이너는 `Running`으로 취급되지 않았다 (훅이 컨테이너 시작을 막는다). 그래서 `Initialized=True`(8초)와 `phase=Running`(13초) 사이가 5초 벌어졌다 (훅 3초 + readiness 주기).

##### 1-3. phase와 restartPolicy (실측)

`sleep 2` 뒤 종료 코드 0/1로 끝나는 컨테이너를 `restartPolicy`별로 비교했다 (약 22초 뒤 상태).

| `restartPolicy` | exit 0 | exit 1 |
|---|---|---|
| **Always** (기본) | `Running`, RESTARTS 1 (계속 다시 시작) | `Running`, RESTARTS 2, STATUS `Error`/`CrashLoopBackOff` |
| **OnFailure** | **`Succeeded`**, `Completed` | `Running`, RESTARTS 2 (재시작) |
| **Never** | **`Succeeded`**, `Completed` | **`Failed`**, `Error`, RESTARTS 0 |

- `restartPolicy`는 **Pod 단위**이고 컨테이너 재시작 여부를 정한다. 웹 서버는 `Always`(기본), 한 번 실행하고 끝나는 작업은 `OnFailure`/`Never`(Job은 이 둘만 가능, 도커 책 13장).
- 컨테이너가 계속 실패하면 kubelet이 **지수적으로 대기 시간을 늘려**(`CrashLoopBackOff`) 재시작한다. `Always`/`OnFailure` Pod의 `RESTARTS`가 계속 늘어나고 STATUS가 `Error`와 `CrashLoopBackOff`를 오간다.
- `Succeeded`/`Failed` Pod는 **끝난 Pod**다. 다시 시작하지 않고 남아 있다 (Job의 Pod가 `Completed`로 남는 이유).

##### 1-4. 컨테이너 state와 reason (실측)

| 상황 | `kubectl get` STATUS / 컨테이너 state |
|---|---|
| 없는 이미지 태그 | `waiting: ErrImagePull` → 이후 `ImagePullBackOff` |
| 없는 ConfigMap 참조 (7장) | `waiting: CreateContainerConfigError` (Pod phase는 `Pending`) |
| 없는 실행 파일 (`command: ["/no/such/binary"]`) | `waiting: RunContainerError` |
| 종료 코드 3으로 종료 | `terminated: reason=Error, exitCode=3`, Pod phase `Failed` |

- **종료 메시지**: 컨테이너가 `/dev/termination-log`에 쓴 내용이 종료 사유로 `kubectl`에 남는다.

```
$ (echo 'custom failure reason' > /dev/termination-log; exit 3)
$ k -n t5p get pod st-msg -o jsonpath='terminated={...terminated.reason}/{...terminated.exitCode} msg={...terminated.message}'
terminated=Error/3 msg=custom failure reason
```

  로그를 못 보는 상황에서 실패 원인을 짧게 남기는 방법이다 (`terminationMessagePath`로 경로 변경 가능, 이번에는 기본 경로만 시험).
- 실패 원인을 찾는 순서: ① `kubectl describe pod`의 Events와 State/Last State ② `kubectl logs --previous`(이전 컨테이너의 로그) ③ 종료 코드(`137`=SIGKILL/OOM, `143`=SIGTERM, 도커 책 11장).

##### 1-5. 종료(Termination) 순서와 유예 시간 (실측)

Pod 삭제 요청부터 컨테이너가 사라질 때까지의 순서와 시간을 3가지 컨테이너로 재 봤다 (컨테이너 로그를 `logs -f`로 받고 `kubectl delete`의 소요 시간을 쟀다).

```
삭제 요청 → Pod가 Terminating, 엔드포인트에서 제거 시작
         → preStop 훅 실행 (끝날 때까지 대기)
         → 컨테이너 PID 1에 SIGTERM
         → terminationGracePeriodSeconds(기본 30초) 안에 종료하지 않으면 SIGKILL
```

| 컨테이너 | 설정 | 로그 | `delete` 소요 |
|---|---|---|---|
| SIGTERM을 처리하고 2초 뒤 종료 | grace 30, preStop 3초 | `started` → `SIGTERM-received` → `exiting` | **6.1초** (preStop 3 + 처리 2 + 오버헤드) |
| SIGTERM을 무시 (`sh`가 PID 1, trap 없음) | grace 8 | `started` 뒤 아무것도 없음 | **9.2초** (grace 8초 + 오버헤드, 이후 SIGKILL) |
| preStop(6초)이 grace(4초)보다 김 | grace 4 | `started` → `SIGTERM-received` | **5.4초** |

- **`preStop` 훅이 끝나야 SIGTERM이 간다.** 훅 소요 시간도 grace period에 포함된다. 훅(6초)이 grace(4초)보다 길면 훅이 끝나기 전에 시간이 만료되어 kubelet이 SIGTERM을 보내고 곧 정리한다 (5.4초에 끝남. 정확한 추가 유예 시간은 확인하지 않았다).
- **SIGTERM을 무시하면 grace가 끝날 때까지 기다린 뒤 SIGKILL**로 죽는다. 11장의 liveness 재시작이 느렸던 이유다 (셸 스크립트나 `sleep`이 PID 1이면 신호를 못 받는 경우가 많다. 앱이 SIGTERM을 처리하게 만들거나 `exec`로 실행).
- **preStop/postStart 훅의 출력은 컨테이너 로그에 나오지 않았다** (preStop에서 `echo preStop-begin`을 했지만 로그에는 `started`와 SIGTERM 처리 줄만 있었다). 훅의 실패는 이벤트(`FailedPreStopHook` 등)로 남는다고 알려져 있으나 이번에 시험하지 않았다.
- 종료 시 트래픽: 삭제 즉시 엔드포인트에서 제거가 시작되지만 전파에 시간이 걸려서, 종료 직전에 요청이 들어올 수 있다. 그래서 `preStop`에 `sleep 5`를 넣어 전파를 기다리는 관행이 있다 (문서 지식, 이번에 시험하지 않았다).

#### 2. kubelet 프로브: livenessProbe, readinessProbe, startupProbe

실험 상세(재시작 타이밍, 엔드포인트 변화, startupProbe, timeout, 롤아웃 게이팅)는 도커/쿠버네티스 책 11장 6절에 정리했다. 여기서는 개념을 요약하고 이번에 추가로 확인한 것만 적는다.

##### 2-1. 세 프로브 비교

| 프로브 | 질문 | 실패하면 | 시작 시점 |
|---|---|---|---|
| **livenessProbe** | 살아 있나? (먹통/데드락) | 컨테이너 **재시작** | 컨테이너 시작 후 (startupProbe가 있으면 그것이 성공한 뒤) |
| **readinessProbe** | 트래픽을 받을 준비가 됐나? | Service **엔드포인트에서 제외**(재시작 안 함), `Ready=False` | 컨테이너 시작 후 (startupProbe가 성공한 뒤) |
| **startupProbe** | 시작이 끝났나? | 성공할 때까지 위 둘을 **보류**. `failureThreshold` 초과 시 재시작 | 컨테이너 시작 직후 |

| 방식 | 설정 | 성공 조건 |
|---|---|---|
| `httpGet` | 경로/포트/헤더 | 응답 코드 200~399 |
| `tcpSocket` | 포트 | 연결 성공 |
| `exec` | 컨테이너 안 명령 | 종료 코드 0 |
| `grpc` | gRPC health | 이번에 시험하지 않음 |

| 필드 | 의미 |
|---|---|
| `initialDelaySeconds` | 첫 검사 전 대기 |
| `periodSeconds` | 검사 간격 |
| `timeoutSeconds` | 한 번의 검사 제한 시간 |
| `failureThreshold` | 연속 실패 횟수 임계값 |
| `successThreshold` | 연속 성공 횟수 임계값 (liveness/startup은 1이어야 함) |

##### 2-2. 프로브는 누가 보내나: kubelet (실측)

nginx에 httpGet 프로브(readiness `/index.html`에 커스텀 헤더 `X-Probe`, liveness `/`)를 걸고 nginx 접근 로그를 봤다.

```
$ k -n t5p logs probe-who --tail=60 | grep -E '"GET /(index.html)? HTTP' | awk '{print $1,$7,$9,$(NF-1)}' | sort | uniq -c
   5 10.1.0.1 /index.html 200 "kube-probe/1.32"
   3 10.1.0.1 /        200 "kube-probe/1.32"
$ k -n t5p exec probe-who -- ip route | head -1
default via 10.1.0.1 dev eth0                              ← 요청의 소스 IP가 이 Pod의 기본 게이트웨이(노드 쪽)
$ k -n t5p get pod probe-who -o jsonpath='podIP={.status.podIP} hostIP={.status.hostIP}'
podIP=10.1.1.247 hostIP=192.168.65.3
```

- 프로브 요청의 **User-Agent는 `kube-probe/1.32`** (kubelet 버전과 같음)이고 **소스 IP는 Pod의 게이트웨이 `10.1.0.1`**, 즉 **kubelet이 있는 노드 쪽**이다. 프로브는 **kubelet이 노드에서 Pod로 보내는 요청**이다 (클러스터의 다른 Pod나 Service를 거치지 않는다).
- 그래서 앱이 접근 로그를 남기면 **프로브 요청이 로그를 채운다** (위의 `kube-probe`). 로그 수집/알림에서 프로브 요청을 제외하거나, 전용 헬스 경로를 쓰는 것이 일반적이다.
- 앱이 클라이언트 IP로 접근 제한을 걸고 있다면(허용 목록 등) **kubelet(노드)의 IP를 허용**해야 프로브가 통과한다.
- `describe` 출력: `Readiness: http-get http://:http/index.html delay=0s timeout=1s period=3s #success=3 #failure=3`. **포트는 이름(`http`)으로도 쓸 수 있다.**
- `successThreshold: 3`을 준 readiness는 프로브 3번이 연속 성공해야 Ready가 되어, 컨테이너 시작(11:07:11) 약 4초 뒤(11:07:15)에 `Ready`로 전이했다 (프로브 로그의 `/index.html` 요청이 11, 12, 15초에 있었다. 3번째 성공이 `Ready` 전이 시각과 맞는지는 초 단위로만 확인).
- 404를 반환하는 경로에 프로브를 걸면 `Liveness probe failed: HTTP probe failed with statuscode: 404` 같은 이벤트가 쌓였다 (nginx에 없는 경로 `/live-check`로 먼저 시도해서 우연히 확인).

##### 2-3. 프로브 설정 팁

| 팁 | 이유 |
|---|---|
| readiness와 liveness에 같은 경로/조건을 쓰지 않는다 | liveness가 외부 의존성(DB)까지 보면 의존성 장애가 전체 재시작으로 번진다 (도커 책 11장) |
| 느리게 뜨는 앱은 startupProbe | liveness가 시작 전에 죽이는 재시작 루프를 막는다 (도커 책 11장 실측) |
| 프로브 전용 경로의 로그 제외 | 위 실측: 프로브 요청이 접근 로그를 채운다 |
| 종료 시 readiness를 먼저 실패시키거나 preStop으로 대기 | 엔드포인트 제거 전파 지연 |

#### 3. init containers와 containers

##### 3-1. 개념

| 구분 | containers (앱 컨테이너) | init containers |
|---|---|---|
| 시작 | **동시에** 시작 | **순서대로 하나씩**, 앞이 **성공(exit 0)** 해야 다음이 시작 |
| 수명 | Pod가 사는 동안 계속 (또는 `restartPolicy`대로) | **끝나면 종료** (성공으로 끝나야 함) |
| 프로브 | liveness/readiness/startup 사용 | readiness/liveness 없음 (끝나는 것이 목적). **native sidecar는 예외** (3-4) |
| 용도 | 앱 본체, 사이드카 | 사전 준비: 설정 파일 생성, DB 대기, 권한 설정, 데이터 복사, 마이그레이션 |
| 이미지/도구 | 앱 이미지 | 앱 이미지에 없는 도구(`curl`, `git` 등)를 쓰는 별도 이미지 가능 |

- 모든 init container가 성공해야 앱 컨테이너가 시작된다 (`Initialized` condition).
- init container와 앱 컨테이너는 **같은 Pod의 볼륨을 공유**할 수 있어서, init이 만든 파일(설정, 초기 데이터)을 앱이 읽는 패턴이 가장 흔하다.

##### 3-2. 순서와 볼륨 공유 (실측)

init 2개(`i1` 4초, `i2` 2초)가 공유 볼륨(`emptyDir`)에 시각을 기록하고 앱이 마지막에 이어서 읽는 Pod:

```
10:52:03 i1 start
10:52:07 i1 end                                  ← i1이 끝난 뒤에야
10:52:07 i2 start (sees: 2 lines)                ← i2 시작 (i1의 기록을 본다)
10:52:09 i2 end
10:52:10 app start, config=prepared              ← i2가 만든 config.txt를 앱이 읽음
$ k -n t5p get pod init-order -o jsonpath='{range .status.initContainerStatuses[*]}{.name}: {.state.terminated.reason} exit={.state.terminated.exitCode} restarts={.restartCount}{"\n"}{end}'
i1: Completed exit=0 restarts=0
i2: Completed exit=0 restarts=0
```

##### 3-3. init container가 실패하면 (실측)

```
$ (init: echo failing; exit 1)         restartPolicy 기본(Always)
0/1  Init:Error  1       ← 6초 간격으로 재시도
0/1  Init:Error  1
0/1  Init:Error  2
0/1  Init:Error  2
$ k -n t5p get pod init-fail -o jsonpath='app state: {.status.containerStatuses[0].state}  Initialized={.status.conditions[?(@.type=="Initialized")].status}'
app state: {"waiting":{"reason":"PodInitializing"}}  Initialized=False        ← 앱 컨테이너는 시작조차 못 함

$ (같은 init, restartPolicy: Never)
0/1  Init:Error      →  phase=Failed
```

- init이 실패하면 **앱 컨테이너는 시작되지 않고**(`PodInitializing`) `STATUS`가 `Init:Error`(재시도 사이) / `Init:CrashLoopBackOff`가 된다. `restartPolicy`가 `Always`/`OnFailure`면 **init을 다시 실행**하고, `Never`면 **Pod가 `Failed`** 가 된다.
- init이 끝나지 않는 Pod는 `kubectl logs <pod> -c <init 이름>`으로 init 로그를 본다. 앱 컨테이너 로그(`-c <앱>`)는 아직 없다.
- init에는 **"DB가 준비될 때까지 대기"** 같은 루프를 넣기도 하지만, 영원히 끝나지 않으면 Pod가 `Init:0/1`에 갇힌다 (타임아웃을 두거나 앱이 스스로 재시도하게 하는 것이 낫다).

##### 3-4. init container가 다시 실행되는 때 (실측)

init이 `/w/runs`에 한 줄씩 기록하는 Pod:

| 사건 | init 재실행? | 결과 |
|---|---|---|
| **앱 컨테이너만 죽음** (`docker kill`) | **아니오** | `restartCount=1`, `/w/runs`는 한 줄 그대로 |
| **Pod 삭제 후 새로 생성** | **예** (새 Pod이므로 init부터 다시. 이번에는 직접 시험하지 않고 4절의 샌드박스 재생성에서 확인) | |
| **pause(샌드박스) 컨테이너가 죽음** | **예** | 4절의 실측: `initRestarts=1`, `/w/runs`에 두 번째 줄 추가, 앱 컨테이너 `restarts(app)=2` |

- **init은 "Pod의 (샌드박스) 시작 때마다 한 번" 실행**된다. 앱 컨테이너만 재시작될 때는 다시 돌지 않는다. 그러므로 init 작업은 **멱등(여러 번 실행해도 안전)** 해야 한다 (샌드박스가 재생성되면 다시 실행된다).

##### 3-5. 자원 계산: 유효 requests

```
initContainers:  heavy-init  requests cpu 500m, memory 256Mi
containers:      a  requests 100m / 64Mi     b  requests 200m / 64Mi      (합계 300m / 128Mi)
$ k describe node docker-desktop | grep "t5p .* eff"
  t5p   eff   500m (4%)   0 (0%)   256Mi (3%)   0 (0%)
```

- Pod의 유효 requests는 **`max(init container 각각의 최대, 앱 컨테이너 합계)`** 이다 (CPU: max(500m, 300m) = 500m, 메모리: max(256Mi, 128Mi) = 256Mi). init은 앱과 동시에 돌지 않으므로 합산하지 않고 **가장 큰 init**과 앱 합계 중 큰 쪽이 스케줄링 기준이 된다. 무거운 init 하나가 Pod 전체의 배치 요구량을 올릴 수 있다.

##### 3-6. native sidecar (init container + `restartPolicy: Always`)

일반 init은 끝나야 하지만, **`restartPolicy: Always`를 준 init container는 끝나지 않고 앱 컨테이너와 함께 계속 실행되는 사이드카**가 된다 (이 서버 v1.32에서 동작).

```yaml
initContainers:
- name: setup                      # 일반 init (끝나야 다음으로)
  ...
- name: log-sidecar                # native sidecar
  restartPolicy: Always            # ← 이것이 핵심
  startupProbe: {exec: {command: ["sh","-c","test -f /w/log"]}, periodSeconds: 1}     # 프로브 사용 가능
containers:
- name: app
```

```
$ k -n t5p exec native-sidecar -c app -- cat /w/log
10:55:22 setup done
10:55:23 sidecar started                          ← setup이 끝난 뒤 사이드카 시작
10:55:25 app started                               ← 사이드카의 startupProbe 통과 후 앱 시작
$ k -n t5p get pod native-sidecar -o jsonpath='{range .status.initContainerStatuses[*]}init {.name}: started={.started} ready={.ready} state={.state}{"\n"}{end}'
init setup: started=false ready=true state=terminated(Completed)
init log-sidecar: started=true ready=true state=running        ← 계속 실행 중
$ k -n t5p get pod native-sidecar        →  READY=2/2  STATUS=Running
```

- **시작 순서가 보장된다**: 일반 init → 사이드카(startupProbe 통과) → 앱. 일반 사이드카(containers에 나란히)는 시작 순서를 보장할 수 없지만 native sidecar는 앱보다 **먼저 준비되는 것을 보장**한다 (프록시나 로그 에이전트가 앱보다 먼저 떠 있어야 할 때).
- 읽기: `STATUS` `READY=2/2`는 사이드카 + 앱이다.

**Job에서의 차이 (실측)**

작업은 3초 만에 끝나는데 사이드카는 무한 루프인 Job을 두 방식으로 비교했다.

| 방식 | Job 결과 |
|---|---|
| **일반 사이드카** (`containers`에 나란히) | **영원히 끝나지 않음**: Pod `1/2 NotReady`, Job `Running 0/1` (40초 뒤에도 그대로) |
| **native sidecar** (init + `restartPolicy: Always`) | 작업이 끝나자 사이드카가 종료되고 Job이 **`Complete 1/1`** (약 40초, SIGTERM을 무시하는 사이드카라 30초 유예를 기다린 시간 포함) |

- 일반 사이드카는 앱 컨테이너가 끝나도 사이드카가 계속 돌아서 Pod가 끝나지 않아 **Job/CronJob에서 문제**가 된다 (프록시 사이드카가 있는 Job이 안 끝나는 흔한 사고). native sidecar는 메인 컨테이너가 끝나면 사이드카를 종료해 준다.
- 종료 순서(앱이 먼저 끝나고 사이드카가 나중에 종료)는 로그를 잡으려다 실패해서 확인하지 못했다 (문서 지식).

#### 4. pause 컨테이너와 static pod

##### 4-1. pause 컨테이너

**Pod마다 가장 먼저 만들어져서 Pod의 네트워크/IPC 네임스페이스를 붙들고 있는 아주 작은 컨테이너(`registry.k8s.io/pause`)** 다. 앱 컨테이너들은 이 컨테이너의 네임스페이스에 합류한다. (상세 실험은 도커/쿠버네티스 책 6장: `NetworkMode=container:<pause ID>`, net/ipc 공유, pid/uts/mnt 분리, `shareProcessNamespace`일 때 PID 1이 pause.)

- 이 환경의 이미지는 `registry.k8s.io/pause:3.10`이고 Docker Desktop의 `docker ps`에는 보이지 않았다 (앱 컨테이너의 `HostConfig.NetworkMode`에서 ID를 찾아 `docker inspect`로 확인).
- 역할: ① **Pod IP의 주인** (앱 컨테이너가 재시작되어도 IP와 네트워크 네임스페이스가 유지됨) ② 네임스페이스 공유의 기준점 ③ (PID 네임스페이스를 공유할 때) PID 1로서 좀비 프로세스 정리.

**pause가 죽으면 무슨 일이 생기나 (실측)**

```
$ APPC=$(docker ps -q --filter "name=k8s_app_init-rerun_t5p"); PAUSE=$(docker inspect -f '{{.HostConfig.NetworkMode}}' $APPC | sed 's/container://')
$ docker kill $PAUSE
(이벤트)  10:53:48  Killing          Stopping container app
          10:53:48  SandboxChanged   Pod sandbox changed, it will be killed and re-created.
          10:54:18  Created/Started  container counter                      ← init container가 다시 실행됨
          10:54:20  BackOff          Back-off restarting failed container app
          10:54:32  Created/Started  container app
(약 1분 뒤)
phase=Running ip=10.1.1.233 uid=5b4d078e-...(동일) restarts(app)=2 initRestarts=1 ready=True      ← 이전 IP는 10.1.1.231
pause now: 6604d4c90ff8                                                  ← 새 pause 컨테이너
/w/runs: "10:53:01 init ran", "10:54:18 init ran"                        ← emptyDir은 유지(같은 Pod UID), init은 재실행
```

- **pause(샌드박스)가 죽으면 Pod의 모든 컨테이너가 내려가고 샌드박스가 다시 만들어진다**(`SandboxChanged` 이벤트). **Pod IP가 바뀌고**(231 → 233), **init container가 다시 실행**되고 앱 컨테이너가 재시작되었다. **Pod UID는 같고** `emptyDir` 볼륨의 내용은 유지되었다 (Pod 오브젝트가 아니라 노드의 샌드박스만 새로 만들어진 것).
- `kill` 직후 약 30초 동안 `kubectl get`은 `Running`, 기존 IP로 보였다. 상태 반영이 늦으므로 짧은 폴링으로는 못 볼 수 있다 (처음에 24초 폴링으로는 변화를 못 봤다).
- 컨테이너 하나가 죽는 것(앱 `docker kill`, `restarts=1`, init 재실행 없음, IP 유지)과 샌드박스가 죽는 것의 차이가 이것이다.

##### 4-2. static pod

**API 서버를 거치지 않고 노드의 kubelet이 직접 관리하는 Pod.** kubelet은 설정의 `staticPodPath`(디렉터리)에 있는 매니페스트 파일을 감시해서 Pod를 만든다. 4장에서 마스터 컴포넌트(etcd, apiserver 등)가 이 방식이라고 봤다.

```
$ docker exec t5static-control-plane grep -E "staticPodPath|fileCheckFrequency|syncFrequency" /var/lib/kubelet/config.yaml
fileCheckFrequency: 0s    staticPodPath: /etc/kubernetes/manifests    syncFrequency: 0s
```

**직접 만들어 보기 (kind 노드에 파일을 두는 것만으로)**

```
$ docker exec -i <노드> sh -c 'cat > /etc/kubernetes/manifests/static-web.yaml' <<EOF   # apiVersion: v1 / kind: Pod / name: static-web / nginx:alpine
$ kc get pod                                                     # 약 3초 뒤
static-web-t5static-control-plane   0/1   Pending   ...  →  Running          ← 이름은 <name>-<노드 이름>
$ kc get pod static-web-<노드> -o jsonpath='owner={.metadata.ownerReferences[0].kind}/{...name}'
owner=Node/t5static-control-plane                                ← 소유자는 Node
annotations: kubernetes.io/config.source=file, kubernetes.io/config.mirror=<해시>, kubernetes.io/config.hash=<해시>, kubernetes.io/config.seen=<시각>
```

| 항목 | 내용 (실측) |
|---|---|
| 만드는 법 | `staticPodPath`에 YAML 파일 추가 (kubectl 불필요). 약 3초 안에 Pod 생성 |
| 이름 | `<매니페스트의 name>-<노드 이름>` |
| API 서버에서의 모습 | **미러 Pod**: API 서버에 읽기용 사본이 보인다 (`config.mirror`, `config.source=file`), 소유자는 `Node` |
| `kubectl delete pod` | 사라졌다가 **바로 다시 생성**되고 Running (`uid`는 새로 바뀜 = 미러 오브젝트가 재생성). **파일을 지워야** 진짜로 삭제 |
| 파일 수정(이미지 변경) | kubelet이 감지해 Pod를 교체 (**약 20초** 뒤 `image=nginx:1.27-alpine`) |
| 파일 삭제 | 15초 안에 Pod 사라짐 (`No resources found`) |
| **ConfigMap 참조** | **거부**: kubelet 로그 `static pods may not reference configmaps` (Pod가 만들어지지 않음) |
| ServiceAccount/토큰 | **없음**: `serviceAccount=[] volumes=[]` (API 서버에 의존하지 않기 때문) |
| API 서버가 죽었을 때 | 컨테이너를 `crictl stop`했더니 **API 서버가 없어도 kubelet이 새 컨테이너로 교체** (`18e3fc615eec` → `204f71ee7f4f`) |
| 컨트롤러(Deployment 등) | **없음**: 스케일, 롤링 업데이트 같은 기능이 없다. kubelet이 파일 기준으로만 관리 |

- **API 서버 없이도 동작**하므로 **API 서버 자체를 Pod로 띄우는 부트스트랩**(마스터 컴포넌트)에 쓰는 것이 본래 용도다 (닭과 달걀 문제를 푸는 방법). 앱 배포에는 쓰지 않는다. 그 노드에 고정되고, ConfigMap/Secret/ServiceAccount를 못 쓰며, 컨트롤러의 도움이 없기 때문이다.
- 스태틱 Pod는 **노드에 파일을 둬야 하는** 관리 방식이라 클러스터 관리자 영역이고, 파일 접근 권한이 곧 Pod를 만드는 권한이다.
- 4장의 마스터 구성(`/etc/kubernetes/manifests`에 etcd/apiserver/scheduler/controller-manager 4개 파일)이 이 메커니즘이다.
- DaemonSet(도커 책 13장)과 비교: DaemonSet은 API 서버가 관리하는 컨트롤러라 노드마다 하나를 자동 배치하고 ConfigMap/ServiceAccount를 쓸 수 있다.

#### 5. Pod 환경 변수(env) 설정

##### 5-1. 값을 넣는 방법

| 방법 | 형식 | 비고 |
|---|---|---|
| 직접 값 | `env: [{name: A, value: "x"}]` | |
| 다른 변수 참조 | `value: "$(A)-world"` | **앞에 정의된** 변수만 참조 가능 |
| **Downward API** (필드) | `valueFrom.fieldRef.fieldPath` | Pod 자신의 메타데이터/상태 |
| **Downward API** (리소스) | `valueFrom.resourceFieldRef` | 컨테이너의 requests/limits |
| ConfigMap의 키 | `valueFrom.configMapKeyRef` | 7장 |
| Secret의 키 | `valueFrom.secretKeyRef` | 7장 |
| ConfigMap/Secret 전체 | `envFrom: [{configMapRef/secretRef, prefix}]` | 키가 모두 환경 변수로 |

##### 5-2. 실측 결과

```yaml
envFrom:
- configMapRef: {name: app-cfg}
  prefix: CFG_
- configMapRef: {name: app-cfg}          # ConfigMap: LOG_LEVEL=debug MODE=prod bad-key=1 _OK=yes
env:
- {name: A_PLAIN, value: "hello"}
- {name: B_REF, value: "$(A_PLAIN)-world"}
- {name: C_FORWARD, value: "$(D_LATER)-x"}
- {name: D_LATER, value: "defined-after"}
- {name: POD_NAME, valueFrom: {fieldRef: {fieldPath: metadata.name}}}
- {name: POD_IP, valueFrom: {fieldRef: {fieldPath: status.podIP}}}
- {name: NODE_NAME, valueFrom: {fieldRef: {fieldPath: spec.nodeName}}}
- {name: POD_LABEL_APP, valueFrom: {fieldRef: {fieldPath: "metadata.labels['app']"}}}
- {name: LIM_CPU, valueFrom: {resourceFieldRef: {resource: limits.cpu}}}               # limits.cpu: 500m
- {name: LIM_MEM_MI, valueFrom: {resourceFieldRef: {resource: limits.memory, divisor: 1Mi}}}
- {name: REQ_CPU_M, valueFrom: {resourceFieldRef: {resource: requests.cpu, divisor: 1m}}}
- {name: SECRET_DB_PASS, valueFrom: {secretKeyRef: {name: app-sec, key: DB_PASS}}}
- {name: OPT_MISSING, valueFrom: {configMapKeyRef: {name: no-such-cm, key: x, optional: true}}}
- {name: DUP, value: "first"}
- {name: DUP, value: "second"}
- {name: MODE, value: "from-env-overrides-envFrom"}
- {name: FULL, value: "$$(A_PLAIN) is not expanded; $(UNDEFINED_VAR) stays"}
```

```
A_PLAIN=hello
B_REF=hello-world                                ← $(A_PLAIN) 참조가 확장됨
C_FORWARD=$(D_LATER)-x                           ← 나중에 정의된 변수는 확장되지 않음
CFG_LOG_LEVEL=debug  CFG_MODE=prod  CFG__OK=yes  CFG_bad-key=1       ← prefix가 붙음
LOG_LEVEL=debug                                  ← prefix 없는 envFrom도 그대로
POD_NAME=envs  POD_NS=t5p  POD_IP=10.1.1.237  NODE_NAME=docker-desktop  POD_SA=default
POD_LABEL_APP=envdemo  POD_ANNOT_OWNER=team-a    ← 라벨/어노테이션은 ['키'] 형식
LIM_CPU=1                                         ← limits.cpu 500m이 divisor 1(코어)로 올림되어 1
LIM_MEM_MI=128                                    ← limits.memory 128Mi를 divisor 1Mi로
REQ_CPU_M=250                                     ← requests.cpu 250m을 divisor 1m로
SECRET_DB_PASS=s3cret
(OPT_MISSING 없음)                                 ← optional: true 이면 없는 ConfigMap을 무시
DUP=second                                        ← 중복 이름은 뒤의 것이 이김
MODE=from-env-overrides-envFrom                   ← env가 envFrom을 이김 (envFrom에는 MODE=prod)
FULL=$(A_PLAIN) is not expanded; $(UNDEFINED_VAR) stays     ← $$(...)는 리터럴 $(...)로 (이스케이프)
```

| 규칙 | 실측 |
|---|---|
| `$(VAR)` 참조는 **같은 `env` 목록에서 앞에 정의된** 변수만 확장 | `B_REF` 확장됨, `C_FORWARD`(뒤에 정의된 `D_LATER` 참조)는 확장 안 됨 |
| 정의되지 않은 `$(X)`는 그대로 남음 | `$(UNDEFINED_VAR)` |
| `$$(...)`로 확장을 막는다 | `FULL` |
| `fieldRef`로 쓸 수 있는 것 | `metadata.name`, `metadata.namespace`, `status.podIP`, `spec.nodeName`, `spec.serviceAccountName`, `metadata.labels['<키>']`, `metadata.annotations['<키>']` (이번에 확인한 것들) |
| `resourceFieldRef`는 **`divisor`로 단위를 조정**. 기본 divisor는 CPU 1 | `limits.cpu: 500m`이 `LIM_CPU=1`(올림). 밀리코어로 받으려면 `divisor: 1m` |
| `env`가 같은 키의 `envFrom`을 덮어쓴다 | `MODE` |
| 같은 이름을 두 번 쓰면 마지막이 이김 | `DUP=second`. `kubectl apply`가 `hides previous definition of "DUP"` 경고를 냄 |
| `optional: true`는 없는 ConfigMap/키를 무시 | `OPT_MISSING` 미설정 (optional이 없으면 7장의 `CreateContainerConfigError`) |
| `envFrom`의 `prefix` | `CFG_LOG_LEVEL` 등 |

- **환경 변수로 쓸 수 없는 이름의 키**(`bad-key`)도 `envFrom`으로 주입되었다 (`env` 명령에는 `CFG_bad-key=1`로 보였다). 셸에서 `$CFG_bad-key`로는 참조할 수 없는 이름이라 앱에서 쓰기 어렵다. 이 환경에서 이 키에 대한 경고 이벤트는 찾지 못했다 (문서에는 유효하지 않은 이름의 키는 건너뛴다는 설명이 있을 수 있으나 이 서버에서 확인한 것은 주입된다는 사실이다).
- 위 `kubectl apply`는 중복 `DUP`에 대해 `Warning: spec.containers[0].env[18]: hides previous definition of "DUP"`를 냈다.

##### 5-3. 환경 변수의 한계

**① 컨테이너 시작 시점에 고정된다**

```
$ k -n t5p patch configmap app-cfg -p '{"data":{"MODE":"changed"}}'
$ k -n t5p exec envs -- sh -c 'echo GREETING=$GREETING CFG_MODE=$CFG_MODE'
GREETING=prod CFG_MODE=prod                      ← ConfigMap을 바꿔도 환경 변수는 그대로 (7장)
```

**② Secret도 평문으로 보인다**

```
$ k -n t5p exec envs -- sh -c 'cat /proc/1/environ | tr "\0" "\n" | grep -E "^(POD_NAME|SECRET_DB_PASS)="'
POD_NAME=envs   SECRET_DB_PASS=s3cret
$ docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' <컨테이너> | grep SECRET_DB_PASS
SECRET_DB_PASS=s3cret                             ← 런타임 설정에도 평문 (7장)
$ k -n t5p get pod envs -o jsonpath='{.spec.containers[0].env[?(@.name=="SECRET_DB_PASS")]}'
{"name":"SECRET_DB_PASS","valueFrom":{"secretKeyRef":{"key":"DB_PASS","name":"app-sec"}}}    ← Pod 정의에는 참조만
```

**③ 바뀌는 값은 Downward API 볼륨으로**

```
$ (downwardAPI 볼륨 + env 둘 다 라벨 stage를 노출)   k -n t5p label pod dapi stage=two --overwrite
volume label after ~8s: stage="two"              ← 볼륨 파일은 갱신됨
env STAGE_ENV=one                                 ← 환경 변수는 그대로
cpu_limit file=1                                  ← 볼륨의 resourceFieldRef도 올림(기본 divisor)
```

- 라벨/어노테이션처럼 **실행 중에 바뀔 수 있는 값**은 환경 변수가 아니라 **downwardAPI 볼륨**으로 받아야 한다.

#### 6. 멀티 컨테이너 패턴: 사이드카, 앰배서더, 어댑터

한 Pod 안에 컨테이너를 여럿 두는 이유는 **네트워크(localhost)와 볼륨을 공유**하면서 **관심사를 분리**하기 위해서다 (Pod 안 컨테이너는 같은 IP, 같은 localhost, 공유 가능한 볼륨. 도커 책 6장). 함께 배치되고 함께 확장/종료되어야 하는 보조 프로세스만 같은 Pod에 둔다.

| 패턴 | 한 줄 정의 | 보조 컨테이너의 방향 | 대표 예 |
|---|---|---|---|
| **사이드카** (sidecar) | 메인 앱 옆에서 **기능을 보강** | 앱의 부가 기능 | 로그 수집/전송, 설정 동기화, 프록시(서비스 메시), 인증서 갱신 |
| **앰배서더** (ambassador) | 메인 앱을 대신해 **외부 서비스와 통신을 중개** | **앱 → 외부** | 로컬 프록시, DB 샤딩/연결 풀링, 서비스 디스커버리, TLS 종단 |
| **어댑터** (adapter) | 메인 앱의 **출력을 외부가 기대하는 표준 형식으로 변환** | **외부 ← 앱** | 메트릭/로그 포맷 변환 (예: 앱의 독자 포맷 → Prometheus) |

##### 6-1. 사이드카: 로그 전송 (실측)

앱은 로그를 **파일에만** 쓰고(레거시 방식), 사이드카가 그 파일을 읽어 **표준 출력**으로 내보낸다.

```yaml
volumes: [{name: logs, emptyDir: {}}]
containers:
- name: app            # 파일에만 쓴다
  command: ["sh","-c","i=0; while :; do i=$((i+1)); echo \"$(date +%T) request-$i status=200\" >> /var/log/app/app.log; sleep 2; done"]
  volumeMounts: [{name: logs, mountPath: /var/log/app}]
- name: log-shipper    # 사이드카: 파일 → stdout
  command: ["sh","-c","touch /var/log/app/app.log; tail -n+1 -F /var/log/app/app.log"]
  volumeMounts: [{name: logs, mountPath: /var/log/app}]
```

```
$ k -n t5p logs p-sidecar -c app | wc -l
0                                              ← 앱의 stdout에는 아무것도 없다
$ k -n t5p logs p-sidecar -c log-shipper --tail=3
10:59:17 request-61 status=200
10:59:19 request-62 status=200
10:59:21 request-63 status=200                 ← 사이드카가 파일을 stdout으로
```

- 앱을 수정하지 않고도 `kubectl logs`와 노드의 로그 수집기가 읽을 수 있게 만든다. 사이드카 자리에 Fluent Bit 같은 **로그 전송 에이전트**를 두고 외부 로그 저장소로 보내는 방식이 흔하다 (이 환경에서 실제 에이전트는 시험하지 않았다).
- **같은 emptyDir를 두 컨테이너가 마운트**한 것이 핵심이다 (9장, 도커 책의 emptyDir).
- 한계: 사이드카도 Pod의 일부라서 자원을 쓰고(requests 합산), **앱이 끝나도 사이드카가 안 끝나면 Job이 안 끝난다** (3-6절의 native sidecar가 해결).

##### 6-2. 앰배서더: 로컬 프록시 (실측)

앱은 **`localhost:8080`** 만 알고, 앰배서더(nginx)가 실제 백엔드 Service(`backend-svc`)로 전달한다. 프록시 설정은 init container가 만들어 emptyDir로 전달했다.

```
$ k -n t5p exec p-ambassador -c app -- wget -q -S -O /dev/null http://127.0.0.1:8080
  HTTP/1.1 200 OK / Server: nginx/1.31.7 / X-Via: ambassador              ← 앱 → localhost:8080 → 프록시 → backend-svc, 프록시가 헤더 추가
$ k -n t5p exec p-ambassador -c ambassador -- cat /etc/nginx/conf.d/default.conf
server { listen 8080; location / { proxy_pass http://backend-svc.t5p.svc.cluster.local:80; add_header X-Via ambassador; } }
$ (백엔드 Pod 삭제 후, 앱 코드 변경 없이)    →  HTTP/1.1 502 Bad Gateway          ← 앱은 프록시의 오류만 본다
```

- 앱은 **백엔드의 실제 주소(DNS 이름, TLS, 재시도)를 몰라도** 된다. 이 지식이 앰배서더 설정에 들어 있으므로 **앱 이미지를 바꾸지 않고** 연결 방식(서비스 디스커버리, TLS 종단, 재시도, 연결 풀)을 바꿀 수 있다. 서비스 메시의 사이드카 프록시(Envoy 등)가 같은 구조다.
- **`localhost` 함정**: `http://localhost:8080`으로 호출하자 `Connection refused`였고 `127.0.0.1`로는 성공했다. busybox `wget`이 `localhost`를 IPv6(`::1`)로 먼저 시도했고, nginx의 `listen 8080;`은 IPv4만 바인딩해서다. 앰배서더가 IPv6에도 바인딩하거나(`listen [::]:8080`) 앱이 `127.0.0.1`을 쓰게 해야 한다 (이 원인은 증상과 설정으로 추정한 것이고 `::1` 접속 시도를 직접 확인하지는 않았다).

##### 6-3. 어댑터: 출력 형식 변환 (실측)

앱은 **독자 포맷**(`requests=N errors=N latency_ms=N`)으로 통계 파일을 쓰고, 어댑터가 **Prometheus 텍스트 형식**으로 바꿔 HTTP로 노출한다 (14장의 Prometheus가 긁을 수 있는 형식).

```
$ k -n t5p exec p-adapter -c app -- cat /shared/stats.txt
requests=21 errors=1 latency_ms=141                                       ← 앱의 포맷
$ k -n t5p exec p-adapter -c app -- wget -qO- http://127.0.0.1:80/metrics
# TYPE app_requests_total counter
app_requests_total 21
# TYPE app_errors_total counter
app_errors_total 1
# TYPE app_latency_ms gauge
app_latency_ms 141                                                          ← 어댑터가 변환/노출
```

- **앱을 수정하지 않고** 표준 형식으로 맞춘다. 레거시 앱/서드파티 앱의 메트릭·로그를 모니터링 시스템의 형식에 맞출 때 쓴다 (실제로는 exporter 이미지를 어댑터로 붙이는 경우가 많다).
- 실험 중 막힌 점: `alpine`의 busybox에는 **`httpd` applet이 없어서**(`httpd: applet not found`, `exit 127`) 어댑터가 `CrashLoopBackOff`였다. 웹 서버가 필요하면 `nginx:alpine` 같은 이미지를 쓴다.

##### 6-4. 세 패턴의 비교와 선택

| | 사이드카 | 앰배서더 | 어댑터 |
|---|---|---|---|
| 데이터 흐름 | 앱 ↔ 보조 (부가 기능) | 앱 → 보조 → **외부** | 앱 → 보조 → **외부 시스템** (표준 형식) |
| 앱이 아는 것 | 보조의 존재(볼륨 등) | 보조의 `localhost` 주소만 | 보조의 존재 (출력 위치만) |
| 공유 수단 | 볼륨, localhost | **localhost 네트워크** | 볼륨, localhost |
| 이 실험 | 파일 → stdout | localhost:8080 → backend-svc | 독자 포맷 → Prometheus 포맷 |
| 흔한 사례 | 로그 에이전트, 서비스 메시 프록시 | 서비스 메시 프록시, DB 프록시 | exporter |

- 세 패턴은 **구조(같은 Pod, 공유 볼륨/localhost)가 같고 목적이 다를 뿐**이다. 사이드카가 상위 개념이고 앰배서더/어댑터를 사이드카의 특수한 형태로 보기도 한다 (분류는 문서마다 다르다).
- **주의**: 컨테이너를 같은 Pod에 넣으면 **함께 스케줄링/확장/종료**된다. 독립적으로 확장해야 하는 구성요소(웹과 DB)는 별도 Pod로 분리한다. 같은 Pod에 두면 자원 requests가 합산되고(유효 requests, 3-5절) 하나가 죽으면 Pod 전체의 `Ready`에 영향을 준다.
- 시작 순서가 중요한(앱보다 먼저 프록시가 준비되어야 하는) 사이드카는 **native sidecar**(3-6절)를 쓴다.

#### 7. 확인하지 못한 부분

- **Pod phase `Unknown`**: 노드와 통신이 끊긴 Pod가 이 상태가 될 수 있다고 알려져 있으나 이번에는 보지 못했다 (4장 노드 장애 실험에서는 API 기준 `Running`으로 보였다).
- **종료 순서의 세부**: `preStop` 훅이 grace보다 길 때 추가 유예 시간의 정확한 값, 훅 실패 이벤트(`FailedPreStopHook`), `terminationGracePeriodSeconds`와 프로브별 유예, 종료 시 엔드포인트 제거 지연(`preStop sleep`의 효과)은 시험하지 않았다. native sidecar의 **종료 순서**(앱 → 사이드카)는 확인하지 못했다.
- **프로브**: `grpc` 프로브, 프로브의 `exec`가 컨테이너 안에서 프로세스로 보이는지, `terminationGracePeriodSeconds`(프로브 단위), liveness 실패 후 재시작 횟수에 따른 `CrashLoopBackOff` 간격은 확인하지 않았다. 도커/쿠버네티스 책 11장의 실험과 같은 환경 한계를 따른다.
- **init container**: 영원히 끝나지 않는 init(`Init:0/1`)의 타임아웃, init의 `securityContext`/자원 제한 상세, **Pod를 삭제한 뒤 새 Pod에서 init이 다시 도는 것**은 직접 시험하지 않았다 (새 Pod이므로 당연하다는 해석).
- **pause 컨테이너**: 다른 런타임(containerd, CRI-O)에서의 pause/샌드박스 동작, pause 컨테이너가 죽었을 때 재생성까지 약 30초가 걸린 이유(kubelet 폴링/백오프 추정)는 확인하지 않았다. pause 컨테이너가 죽은 직후 상태가 `Running`으로 보이는 시간이 약 30초였는데 `kubectl`의 반영 지연 원인은 확인하지 못했다.
- **static pod**: 미러 Pod를 삭제했을 때 **실제 컨테이너가 그대로 유지되는지**는 확인에 실패했다 (컨테이너 ID 비교 명령이 비어 있었다). 미러 오브젝트의 `uid`가 바뀐 것만 확인했다. `fileCheckFrequency`의 값(`0s`는 기본값 적용을 뜻하는 것으로 보이나 실제 주기는 확인하지 않음), HTTP로 매니페스트를 받는 `staticPodURL`, 노드가 여러 개일 때의 이름 규칙은 확인하지 않았다.
- **환경 변수**: 유효하지 않은 이름의 `envFrom` 키(`bad-key`)가 건너뛰어지지 않고 주입된 이유, `envFrom`에서 두 ConfigMap의 같은 키 충돌 우선순위, `fieldRef`의 `status.hostIP`/`spec.serviceAccountName` 외 필드, 환경 변수 개수/크기 제한, `env`와 `envFrom`이 Secret에서 오는 경우의 차이(`secretRef`)는 시험하지 않았다. `resourceFieldRef`의 `requests.memory`, `limits.ephemeral-storage`는 확인하지 않았다.
- **멀티 컨테이너 패턴**: 실제 로그 에이전트(Fluent Bit), 서비스 메시 프록시(Envoy/Istio), 실제 exporter는 설치하지 않고 shell/nginx로 같은 구조를 흉내 냈다. 앰배서더의 `localhost` 실패 원인(IPv6 `::1` 시도)은 추정이다.
